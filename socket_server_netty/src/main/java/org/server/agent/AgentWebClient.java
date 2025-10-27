package org.server.agent;

import org.server.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

@Component
public class AgentWebClient {

    private final WebClient webClient;

    @Autowired
    public AgentWebClient() {
        this.webClient = WebClient.builder()
                .baseUrl("http://127.0.0.1:9100")
                .build();
    }

    public <T> CompletableFuture<T> sendToAgent(User user, Class<T> tClass) {
        return webClient.post()
                .uri("/api/v1/agent")
                .bodyValue(user)
                .retrieve()
                .bodyToMono(tClass)
                .toFuture();
    }

    public Flux<DataBuffer> sendToAgentGetStream(User user) {
        return webClient.post()
                .uri("/api/v1/agent")
                .bodyValue(user)
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class);
    }
}
