package com.socket.client.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.socket.client.SessionManager;
import com.socket.client.mappers.Mapper;
import com.socket.client.model.Message;
import com.socket.client.model.User;
import com.socket.client.model.UserJoinedMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.socket.client.model.Message.MessageTopic.ON_JOIN;
import static com.socket.client.model.Message.MessageTopic.ON_MESSAGE;

public class ChatController {
    @FXML
    public ListView<String> userListView;

    @FXML
    public VBox messagesBox;

    @FXML
    public ScrollPane scrollBar;

    @FXML
    public TextArea messageInput;

    private static Channel channel;

    private List<UserJoinedMessage> userJoinedMessages = Collections.synchronizedList(new ArrayList<>());


    private ObservableList<String> users = FXCollections.observableArrayList();

    public void initialize() {

        scrollBar.vvalueProperty().bind(messagesBox.heightProperty());

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
                                        Platform.runLater(() -> {
                                            switch (message.getTopic()){
                                                case ON_MESSAGE:
                                                    User user = Mapper.objectMapper.convertValue(message.getMessageObject(),User.class);

                                                    VBox container = new VBox(10);
                                                    if(user.getMessageType()== User.MessageType.PROMPT){
                                                        String base64Image = user.getMessage();
                                                        if(base64Image.contains(":")) {
                                                            base64Image = base64Image.substring(base64Image.indexOf(":") + 1).trim();
                                                        }

                                                        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                                                        InputStream is = new ByteArrayInputStream(imageBytes);
                                                        Image image = new Image(is);
                                                        ImageView imageView = new ImageView(image);
                                                        imageView.setPreserveRatio(true);
                                                        imageView.getStyleClass().add("image-view");


                                                        Button downloadBtn = new Button("Download");
                                                        downloadBtn.setOnAction(event -> {
                                                            FileChooser fileChooser = new FileChooser();
                                                            fileChooser.setTitle("Save Image");
                                                            fileChooser.getExtensionFilters().add(
                                                                    new FileChooser.ExtensionFilter("PNG Image", "*.png")
                                                            );
                                                            File file = fileChooser.showSaveDialog(downloadBtn.getScene().getWindow());
                                                            if(file != null){
                                                                try (FileOutputStream fos = new FileOutputStream(file)) {
                                                                    BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
                                                                    ImageIO.write(bImage, "png", fos);
                                                                    System.out.println("Resim kaydedildi: " + file.getAbsolutePath());
                                                                } catch (IOException e) {
                                                                    e.printStackTrace();
                                                                }
                                                            }
                                                        });


                                                        imageView.setFitHeight(200);

                                                        Label userName = new Label("Image Generator: ");
                                                        userName.setStyle("-fx-text-fill: #A0C4FF;");
                                                        container.getStyleClass().add("text-message-wrapper");
                                                        container.getChildren().addAll(userName,imageView, downloadBtn);
                                                        container.getStyleClass().add("wrapper");

                                                    }else{
                                                        Label userName = new Label(user.getName()+": ");
                                                        userName.setStyle("-fx-text-fill: #A0C4FF;");

                                                        Label textLabel = new Label(user.getMessage());
                                                        textLabel.setWrapText(true);
                                                        textLabel.setMaxWidth(500);
                                                        textLabel.setAlignment(Pos.CENTER_LEFT);
                                                        container.getStyleClass().add("text-message-wrapper");
                                                        textLabel.getStyleClass().add("label");
                                                        container.getChildren().add(userName);;
                                                        container.getChildren().add(textLabel);
                                                    }
                                                    messagesBox.getChildren().add(container);

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
                                        channel.writeAndFlush(Mapper.objectMapper.writeValueAsString(messageNew)+"\n");
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    cause.printStackTrace();
                                    ctx.close();

                                    // JavaFX UI thread’inde ekran değişimini yap
                                    Platform.runLater(() -> {
                                        closedConnection();
                                    });
                                }
                            });
                        }
                    });

            ChannelFuture future = bootstrap.connect("192.168.1.103", 8131).sync();
            future.channel().closeFuture().addListener(f -> {
               closedConnection();
            });

        } catch (Exception e) {
            System.out.println("Occurred error: "+e.getMessage());
            closedConnection();
        }


        userListView.setItems(users);
    }

    public void closeSocket(){
        System.out.println("Uygulama kapanıyor, soket kapatılıyor...");
        try {
            if(channel != null && channel.isOpen()){
                channel.close().sync(); // channel kapat
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void closedConnection() {
       Platform.runLater(()->{
           try {
               FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/socket/client/main-screen.fxml"));
               Parent root = loader.load();
               Stage stage = (Stage) userListView.getScene().getWindow();
               double currentWidth = stage.getWidth();
               double currentHeight = stage.getHeight();

               Scene newScene = new Scene(root, currentWidth, currentHeight);

               stage.setScene(newScene);
               stage.show();
           } catch (IOException e) {
               throw new RuntimeException(e);
           }
       });
    }
    // /image -prompt draw a basic tree
    @FXML
    public void sendMessage(ActionEvent event) {
        String message = messageInput.getText();
        if (!message.isEmpty()) {
            messageInput.clear();

            boolean isImageRequest = message.startsWith("/image -prompt");
            boolean isLamaRequest = message.startsWith("/lama -prompt");

            SessionManager.user.setMessageType(isImageRequest ? User.MessageType.PROMPT: isLamaRequest? User.MessageType.LAMA:User.MessageType.TEXT);

            SessionManager.user.setMessage(isImageRequest || isLamaRequest?message.substring(message.indexOf("prompt")+7):message);

            Message messageNew = new Message<>(ON_MESSAGE, SessionManager.user);
            System.out.println(channel.isActive());
            System.out.println(message);
            try {
                channel.writeAndFlush(Mapper.objectMapper.writeValueAsString(messageNew)+"\n");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void lamaPrompt(){
        String message = messageInput.getText();
        if (!message.isEmpty()) {
            messageInput.clear();

            SessionManager.user.setMessageType(User.MessageType.LAMA);

            SessionManager.user.setMessage(message);

            Message messageNew = new Message<>(ON_MESSAGE, SessionManager.user);

            try {
                channel.writeAndFlush(Mapper.objectMapper.writeValueAsString(messageNew)+"\n");

                SessionManager.user.setMessageType(User.MessageType.TEXT);
                channel.writeAndFlush(Mapper.objectMapper.writeValueAsString(messageNew)+"\n");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void imagePrompt(){
        String message = messageInput.getText();
        if (!message.isEmpty()) {
            messageInput.clear();

            SessionManager.user.setMessageType(User.MessageType.PROMPT);

            SessionManager.user.setMessage(message);

            Message messageNew = new Message<>(ON_MESSAGE, SessionManager.user);

            try {
                channel.writeAndFlush(Mapper.objectMapper.writeValueAsString(messageNew)+"\n");
                SessionManager.user.setMessageType(User.MessageType.TEXT);
                channel.writeAndFlush(Mapper.objectMapper.writeValueAsString(messageNew)+"\n");
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