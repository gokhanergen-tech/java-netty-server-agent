package org.server;


import org.server.agents.AnalyserAgent;
import org.server.client.AsyncClient;
import org.server.mappers.Mapper;
import org.server.mappers.UserMapper;
import org.server.model.Message;
import org.server.model.User;
import org.server.model.UserJoinedMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.server.agents.AnalyserAgent.agentReader;
import static org.server.agents.AnalyserAgent.agentWriter;
import static org.server.model.Message.MessageTopic.ON_MESSAGE;

public class ServerManager {
    private static final int MAX_CLIENTS = Runtime.getRuntime().availableProcessors()*2;

    private Socket socket;

    private ServerSocket serverSocket;
    private LinkedBlockingQueue<User> messages;

    private final List<AsyncClient> clients = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            MAX_CLIENTS,
            MAX_CLIENTS,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadPoolExecutor.AbortPolicy()
    );

    public boolean stopServer() {
        try {
            serverSocket.close();
        } catch (IOException io) {
            return false;
        }
        return true;
    }

    /**
    *  Starting server
    */
    public void startServer() throws IOException {
        this.serverSocket = new ServerSocket(8131);
    }

    /**
     *  When we take any message, send all of the clients
     */
    private void sendAllMessage(Message.MessageTopic messageTopic, User message) {
        for (AsyncClient client : clients) {
            client.sendMessage(new Message<>(messageTopic, message));
        }
    }

    /**
     *  Initialization Server
     */
    public void start() {
        messages = new LinkedBlockingQueue<>();

        executorService.submit(()->{
            try {
                startServer();
                Thread.sleep(1000);
                while (true) {
                    try {
                        socket = serverSocket.accept();

                        System.out.println("Connected a client");
                        AsyncClient client = new AsyncClient(socket, (asyncClient)->{
                            clients.remove(asyncClient);
                        }, (Message message)->{
                            switch (message.getTopic()){
                                case ON_MESSAGE:
                                    User user = Mapper.objectMapper.convertValue(message.getMessageObject(),User.class);

                                    agentWriter.println(UserMapper.userToJSON(user));

                                    String response = agentReader.readLine();

                                    user.setMessage(String.format("%s: %s", user.getName(), response));
                                    messages.add(user);
                                    break;
                                case ON_JOIN:
                                    UserJoinedMessage userJoinedMessage = Mapper.objectMapper.convertValue(message.getMessageObject(), UserJoinedMessage.class);

                                    break;
                            }

                        });

                        this.executor.submit(client);

                        clients.add(client);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        /**
         * Listing all message and send all clients
         */
     executorService.submit(()->{
         while (true) {
             try {
                 User message = messages.take();

                 System.out.println(message);

                 sendAllMessage(ON_MESSAGE, message);
             } catch (InterruptedException e) {
                 break;
             }
         }
     });
    }
}
