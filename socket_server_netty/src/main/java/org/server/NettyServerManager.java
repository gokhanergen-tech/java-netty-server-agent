package org.server;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.server.agent.AgentWebClient;
import org.server.lamaClient.LLamaAgent;
import org.server.mappers.Mapper;
import org.server.model.Message;
import org.server.model.User;
import org.server.model.UserJoinedMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.*;

@Component
public class NettyServerManager {

    public static class Request{
        private User user;
        private String requestId;

        public Request(User user, String requestId) {
            this.user = user;
            this.requestId = requestId;
        }
    }

    private static final int PORT = 8131;

    private final List<Channel> clients = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<UserJoinedMessage>> listMap = new HashMap<>();
    private final ConcurrentMap<Channel, UserJoinedMessage> userMap = new ConcurrentHashMap<>();

    private final AgentWebClient agentWebClient;

    @Autowired
    public NettyServerManager(AgentWebClient agentWebClient) {
        this.agentWebClient = agentWebClient;
    }


    public void start() throws InterruptedException {

        listMap.put("1", Collections.synchronizedList(new ArrayList<>()));

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            clients.add(ch);

                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(5 * 1024 * 1024,
                                    Unpooled.copiedBuffer("\n", CharsetUtil.UTF_8)));

                            ch.pipeline().addLast(new StringDecoder(CharsetUtil.UTF_8));

                            ch.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));

                            ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    clients.remove(ctx.channel());
                                    disconnectUser(ch);
                                    super.channelInactive(ctx);
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                    System.out.println("Got message");
                                    Message<?> message = objectMapper.readValue(msg, Message.class);
                                    switch (message.getTopic()) {
                                        case ON_MESSAGE:
                                            User user = Mapper.objectMapper.convertValue(message.getMessageObject(),User.class);
                                            if(user.getMessageType()== User.MessageType.PROMPT){

                                                Flux<DataBuffer> stream = agentWebClient.sendToAgentGetStream(user);
                                                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                                                stream.subscribe(buffer -> {
                                                    try {
                                                        byte[] chunk = new byte[buffer.readableByteCount()];
                                                        buffer.read(chunk);
                                                        baos.write(chunk, 0, chunk.length);
                                                    } finally {
                                                        DataBufferUtils.release(buffer);
                                                    }
                                                }, error -> {
                                                    error.printStackTrace();
                                                }, () -> {
                                                    String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
                                                    user.setMessage(base64Image);
                                                    sendAllMessage(new Message<>(Message.MessageTopic.ON_MESSAGE, user));
                                                });
                                            } else if (user.getMessageType() == User.MessageType.LAMA) {
                                                user.setMessage(user.getMessage());
                                                CompletableFuture<Map> future = agentWebClient.sendToAgent(user, Map.class);
                                                future.thenAccept(response -> {
                                                    String messageResult = (String) response.get("response");

                                                    LLamaAgent.askToLama(messageResult);
                                                });
                                            } else{
                                                CompletableFuture<Map> future = agentWebClient.sendToAgent(user, Map.class);
                                                future.thenAccept(response -> {
                                                    String messageResult = (String) response.get("response");

                                                    user.setMessage(messageResult);

                                                    if(user.getMessageType() == User.MessageType.LAMA)
                                                        user.setMessageType(User.MessageType.TEXT);

                                                    sendAllMessage(new Message<>(Message.MessageTopic.ON_MESSAGE, user));

                                                });
                                            }
                                            break;
                                        case ON_JOIN:
                                            UserJoinedMessage joined = Mapper.objectMapper.convertValue(message.getMessageObject(), UserJoinedMessage.class);

                                            listMap.get("1").add(joined);
                                            userMap.put(ch,joined);

                                            sendMessageAClient(new Message(Message.MessageTopic.ON_JOIN_SELF,listMap.get("1")), ch);
                                            sendMessageToAllClientExceptAClient(new Message(Message.MessageTopic.ON_JOIN,joined), ch);
                                            break;
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    cause.printStackTrace();
                                    ctx.close();
                                }
                            });
                        }
                    });

            ChannelFuture f = bootstrap.bind(PORT).sync();
            System.out.println("Netty Server started on port " + PORT);
            LLamaAgent.connect();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void disconnectUser(Channel ch) {

        UserJoinedMessage userJoinedMessage = userMap.get(ch);

        System.out.println(String.format("Disconnect user id %s", userJoinedMessage.getUser().getId()));
        listMap.get("1").remove(userJoinedMessage);
        userMap.remove(ch);
        sendAllMessage(new Message(Message.MessageTopic.ON_LEAVE, userJoinedMessage.getUser().getId()));
    }

    private void sendAllMessage(Message message) {
        try {
            for (Channel client : clients) {
               sendMessageAClient(message,client);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageAClient(Message message, Channel client) {
        try {
            String json = Mapper.objectMapper.writeValueAsString(message);

            if (client.isActive()) {
                System.out.println(message.getTopic());
                client.writeAndFlush(json+"\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToAllClientExceptAClient(Message message, Channel clientExcept) {
        try {
            for (Channel client : clients) {
                if(clientExcept!=client)
                 sendMessageAClient(message,client);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
