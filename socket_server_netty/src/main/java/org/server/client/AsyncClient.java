package org.server.client;

import org.server.mappers.Mapper;
import org.server.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Connected main.com.socket.client object
 */
public class AsyncClient implements Runnable{
    private final Socket socket;

    private BufferedReader in;
    private PrintWriter out;

    private OnCloseHandler onCloseHandler;
    private OnClientSendAMessage onClientSendAMessage;

    public AsyncClient(Socket socket, OnCloseHandler onCloseHandler, OnClientSendAMessage onClientSendAMessage) {

        this.socket = socket;

        try {
            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.onCloseHandler = onCloseHandler;
        this.onClientSendAMessage = onClientSendAMessage;
    }

    /**
     * Send the message to the this main.com.socket.client
     * @param message
     */
    public <T> void sendMessage(Message<T> message) {
        try {
            out.println(Mapper.objectMapper.writeValueAsString(message));
            out.flush();
        } catch (IOException e) {
            System.out.println("Cannot send the message: "+e.getMessage());
        }

    }


    @Override
    public void run() {
        while (!socket.isClosed()) {
            try {
                String object= in.readLine();
                Message message = Mapper.objectMapper.readValue(object, Message.class);
                onClientSendAMessage.OnClientSendAMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
                // TODO Auto-generated catch block
                try {
                    socket.close();
                } catch (IOException ex) {}
                break;
            }
        }


        onCloseHandler.onConnectionClose(this);
    }
}
