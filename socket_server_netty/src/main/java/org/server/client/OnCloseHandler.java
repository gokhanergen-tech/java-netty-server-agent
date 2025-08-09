package org.server.client;

public interface OnCloseHandler {
    void onConnectionClose(AsyncClient client);
}
