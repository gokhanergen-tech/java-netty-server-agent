package com.socket.client.client;

import com.socket.client.model.Message;
import com.socket.client.model.User;

public interface IClientSocket {
    void connect(MessageHandler messageHandler);

    <T> void sendAMessage(Message<T> user);
}
