package org.server;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.bouncycastle.crypto.engines.CAST5Engine;
import org.server.mappers.Mapper;
import org.server.mappers.UserMapper;
import org.server.model.Message;
import org.server.model.User;
import org.server.model.UserJoinedMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.server.agents.AnalyserAgent.agentReader;
import static org.server.agents.AnalyserAgent.agentWriter;

public class NettyServerManager {

    private static final int PORT = 8131;

    private final List<Channel> clients = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<UserJoinedMessage>> listMap = new HashMap<>();
    private final ConcurrentMap<Channel, UserJoinedMessage> userMap = new ConcurrentHashMap<>();

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

                            ch.pipeline().addLast(new StringDecoder(CharsetUtil.UTF_8));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    clients.remove(ctx.channel());
                                    disconnectUser(ch);
                                    super.channelInactive(ctx);
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                    System.out.println(msg);
                                    Message<?> message = objectMapper.readValue(msg, Message.class);

                                    switch (message.getTopic()) {
                                        case ON_MESSAGE:
                                            User user = Mapper.objectMapper.convertValue(message.getMessageObject(),User.class);

                                            agentWriter.println(UserMapper.userToJSON(user));

                                            String response = agentReader.readLine();

                                            user.setMessage(String.format("%s: %s", user.getName(), response));
                                            sendAllMessage(new Message<>(Message.MessageTopic.ON_MESSAGE,user));
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
            ByteBuf buf = io.netty.buffer.Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);

            if (client.isActive()) {
                client.writeAndFlush(buf.retainedDuplicate());
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
