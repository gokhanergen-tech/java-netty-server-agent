package org.server.lamaClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.server.mappers.Mapper;
import org.server.model.Message;
import org.server.model.User;
import org.server.model.UserJoinedMessage;

import java.io.IOException;
import java.util.UUID;

import static org.server.model.Message.MessageTopic.ON_JOIN;
import static org.server.model.Message.MessageTopic.ON_MESSAGE;

public class LLamaAgent {

    private static Channel channel;
    private static User user;

    public static void connect() {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {

                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new DelimiterBasedFrameDecoder(5 * 1024 * 1024,
                                    Unpooled.wrappedBuffer(new byte[]{'\n'})));
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                    try {
                                        final Message message = Mapper.objectMapper.readValue(msg, Message.class);
                                        switch (message.getTopic()) {
                                            case ON_MESSAGE:
                                                User user = Mapper.objectMapper.convertValue(message.getMessageObject(), User.class);
                                                break;

                                        }
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    channel = ctx.channel();
                                    UUID uuid = UUID.randomUUID();
                                    user = new User();
                                    user.setId(uuid.toString());
                                    user.setName("Lama");
                                    UserJoinedMessage userJoinedMessage = new UserJoinedMessage(user, "1");
                                    Message messageNew = new Message<>(ON_JOIN, userJoinedMessage);
                                    try {
                                        channel.writeAndFlush(Mapper.objectMapper.writeValueAsString(messageNew) + "\n");
                                        System.out.println("Agent connected");
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
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
            ChannelFuture future = bootstrap.connect("192.168.1.103", 8131).sync();
            future.channel().closeFuture().addListener(f -> {
                //Handle If you have close case
            });

            System.out.println("Agent connecting");

        } catch (Exception e) {
            System.out.println("Occurred error: " + e.getMessage());
        }
    }

    public static void askToLama(String message){
        if(user!=null && channel!=null){
            user.setMessage(message);
            user.setMessageType(User.MessageType.TEXT);
            Message messageNew = new Message<>(ON_MESSAGE, user);
            try {
                channel.writeAndFlush(Mapper.objectMapper.writeValueAsString(messageNew)+"\n");
            } catch (JsonProcessingException e) {
                System.out.println("Occurred error: " + e.getMessage());
            }
        }

    }
}
