module com.socket.client {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.antdesignicons;

    opens com.socket.client to javafx.fxml;
    exports com.socket.client;
    exports com.socket.client.client to com.fasterxml.jackson.databind;
    exports com.socket.client.chat;
    opens com.socket.client.chat to javafx.fxml;
    exports com.socket.client.model to com.fasterxml.jackson.databind;

    requires io.netty.transport;
    requires io.netty.codec;
    requires io.netty.common;
}