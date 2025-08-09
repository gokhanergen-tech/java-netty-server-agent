package org.server.agents;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AnalyserAgent {
    private static Socket agent;
    public static PrintWriter agentWriter;
    public static BufferedReader agentReader;

    public static void connect(){
        try {
            System.out.println("Connecting to analyser agent...");
            agent = new Socket("127.0.0.1", 9100);

            agentWriter = new PrintWriter(agent.getOutputStream(), true);
            agentReader = new BufferedReader(new InputStreamReader(agent.getInputStream()));
            System.out.println("Connected to agent");

        } catch (IOException e) {
            System.err.println("Couldn't connect to analyser agent: " + e.getMessage());
        }
    }

    public static boolean isConnected(){
        return agent!=null && agent.isConnected();
    }
}
