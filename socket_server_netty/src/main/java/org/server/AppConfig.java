package org.server;

import org.server.agent.AgentWebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public AgentWebClient agentWebClient(){
         return new AgentWebClient();
    }

    @Bean
    public NettyServerManager nettyServerManager(AgentWebClient agentWebClient){
        return new NettyServerManager(agentWebClient);
    }
}
