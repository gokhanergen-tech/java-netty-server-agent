package com.socket.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;

public class MainScreenController {
    @FXML
    private ImageView userAvatar;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField surnameField;

    @FXML
    private Button joinButton;

    public void initialize() {
        Image avatar = new Image(MainScreen.class.getResourceAsStream("/assets/images/avatar.jpg")); // Proje içindeki resim
        userAvatar.setImage(avatar);
    }

    @FXML
    private void handleJoinButtonClick(ActionEvent event) {
        String username = usernameField.getText();
        String surname = surnameField.getText();

        if (!username.isEmpty() && !surname.isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("chat-layout.fxml"));
                Parent chatRoot = loader.load();

                Stage chatStage = new Stage();
                Scene scene = new Scene(chatRoot);
                scene.getStylesheets().add(getClass().getResource("chat-style.css").toExternalForm());

                SessionManager.user.setName(username);
                SessionManager.user.setSurname(surname);

                chatStage.setTitle("Chat Screen - " + username);
                chatStage.setScene(scene);
                chatStage.show();



                ((Stage) joinButton.getScene().getWindow()).close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Oops");
        }
    }
}
