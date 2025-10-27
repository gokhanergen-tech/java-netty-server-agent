package org.server;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

    public static void main(String[] args) {
        //ServerManager serverSocketManager = new ServerManager();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        NettyServerManager nettyServerManager = context.getBean(NettyServerManager.class);

        try {
            nettyServerManager.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
