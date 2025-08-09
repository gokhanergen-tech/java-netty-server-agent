package org.server.client;

import org.server.model.Message;

import java.io.IOException;

public interface OnClientSendAMessage {
    void OnClientSendAMessage(Message message) throws IOException;
}
