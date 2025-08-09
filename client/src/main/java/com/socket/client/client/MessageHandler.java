package com.socket.client.client;

import com.socket.client.model.Message;
import com.socket.client.model.User;

public interface MessageHandler {
    void onTakenMessage(Message user);
    void onConnected();
    void onDisconnect();
    void onError();
}
