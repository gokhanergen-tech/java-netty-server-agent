package org.server;


import org.server.agents.AnalyserAgent;

public class Main {
    public static void main(String[] args) {
        //ServerManager serverSocketManager = new ServerManager();
        NettyServerManager nettyServerManager = new NettyServerManager();

        try {
            nettyServerManager.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
