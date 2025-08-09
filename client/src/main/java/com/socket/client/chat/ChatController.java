package com.socket.client.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.socket.client.SessionManager;
import com.socket.client.client.ClientAsync;
import com.socket.client.client.MessageHandler;
import com.socket.client.mappers.Mapper;
import com.socket.client.model.Message;
import com.socket.client.model.User;
import com.socket.client.model.UserJoinedMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.socket.client.model.Message.MessageTopic.ON_JOIN;
import static com.socket.client.model.Message.MessageTopic.ON_MESSAGE;

public class ChatController {
    @FXML
    public ListView<String> userListView;

    @FXML
    public ListView<String> messageListView;

    @FXML
    public TextArea messageInput;


    private static Channel channel;

    private List<UserJoinedMessage> userJoinedMessages = Collections.synchronizedList(new ArrayList<>());


    private ObservableList<String> users = FXCollections.observableArrayList();
    private ObservableList<String> messages = FXCollections.observableArrayList();

    public void initialize() {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                    System.out.println("Sunucudan cevap: " + msg);
                                    try {
                                        final Message message = Mapper.objectMapper.readValue(msg, Message.class);
                                        Platform.runLater(() -> {
                                            switch (message.getTopic()){
                                                case ON_MESSAGE:
                                                    User user = Mapper.objectMapper.convertValue(message.getMessageObject(),User.class);
                                                    messages.add(user.getMessage());
                                                    break;
                                                case ON_JOIN:
                                                    UserJoinedMessage userJoinedMessage = Mapper.objectMapper.convertValue(message.getMessageObject(),UserJoinedMessage.class);
                                                    userJoinedMessages.add(userJoinedMessage);
                                                    users.setAll(userJoinedMessages.stream()
                                                            .map(userJoinedMessage1 -> userJoinedMessage1.getUser().getName()).collect(Collectors.toList()));
                                                    break;

                                                case ON_JOIN_SELF:
                                                    List<UserJoinedMessage> userJoinedMessageList =
                                                            Mapper.objectMapper.convertValue(
                                                                    message.getMessageObject(),
                                                                    new TypeReference<List<UserJoinedMessage>>() {}
                                                            );                                                    userJoinedMessages.addAll(userJoinedMessageList);
                                                    users.setAll(userJoinedMessages.stream()
                                                            .map(userJoinedMessage1 -> userJoinedMessage1.getUser().getName()).collect(Collectors.toList()));
                                                    break;
                                                case ON_LEAVE:
                                                    String userId = Mapper.objectMapper.convertValue(message.getMessageObject(),String.class);
                                                    System.out.println(String.format("Left user id %s",userId));
                                                    if(userJoinedMessages.removeIf(userJoinedMessage1 -> userJoinedMessage1.getUser().getId().equals(userId))){
                                                        users.setAll(userJoinedMessages.stream()
                                                                .map(userJoinedMessage1 -> userJoinedMessage1.getUser().getName()).collect(Collectors.toList()));
                                                    }
                                                    break;

                                            }

                                        });
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    channel = ctx.channel();
                                    UUID uuid = UUID.randomUUID();
                                    SessionManager.user.setId(uuid.toString());
                                    UserJoinedMessage userJoinedMessage = new UserJoinedMessage(SessionManager.user,"1");
                                    Message messageNew = new Message<>(ON_JOIN,userJoinedMessage );
                                    try {
                                        channel.writeAndFlush(Mapper.objectMapper.writeValueAsString(messageNew));
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
                System.out.println("Bağlantı kapatıldı.");
                group.shutdownGracefully();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }


        userListView.setItems(users);

        messageListView.setItems(messages);

        // Kullanıcı listesi için özel hücreler (avatar, durum vb.) burada ayarlanabilir.
        // messageListView.setCellFactory(new MessageCellFactory());
    }

    @FXML
    public void sendMessage(ActionEvent event) {
        String message = messageInput.getText();
        if (!message.isEmpty()) {
            messageInput.clear();

            SessionManager.user.setMessage(message);

            Message messageNew = new Message<>(ON_MESSAGE, SessionManager.user);
            System.out.println(channel.isActive());
            System.out.println(message);
            try {
                channel.writeAndFlush(Mapper.objectMapper.writeValueAsString(messageNew));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleEnterReleased(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER)
            sendMessage(null);
    }
}