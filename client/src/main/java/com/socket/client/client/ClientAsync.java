package com.socket.client.client;

import com.socket.client.SessionManager;
import com.socket.client.mappers.Mapper;
import com.socket.client.mappers.UserMapper;
import com.socket.client.model.Message;
import com.socket.client.model.User;
import com.socket.client.model.UserJoinedMessage;

import java.io.*;
import java.net.Socket;

public class ClientAsync implements IClientSocket{
    private Socket client;
    private BufferedWriter out;
    private BufferedReader in;

    @Override
    public void connect(MessageHandler messageHandler) {
        Thread clientThread = new Thread(() -> {
            try {
                System.out.println("Connecting to server");
                client = new Socket("127.0.0.1", 8131);
                System.out.println("Connected to server");
                messageHandler.onConnected();

                out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                sendAMessage(new Message<>(Message.MessageTopic.ON_JOIN, new UserJoinedMessage(SessionManager.user,"123")));

                while(!client.isClosed()){
                    String object= in.readLine();
                    Message message = Mapper.objectMapper.readValue(object, Message.class);
                    messageHandler.onTakenMessage(message);
                }

                System.out.println("Disconnected from the server");
                messageHandler.onDisconnect();
            } catch (Exception e) {
                System.out.println("Occured an error from server");
                System.out.println(e);
                messageHandler.onError();
            }

        });
        clientThread.start();
    }

    @Override
    public <T> void sendAMessage(Message<T> message) {
       try {
           String jsonUser = Mapper.objectMapper.writeValueAsString(message);
           out.write(jsonUser);
           out.newLine();

           out.flush();
       }catch (Exception e){
           e.printStackTrace();
       }
    }
}
