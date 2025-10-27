package org.server.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.server.NettyServerManager;
import org.server.model.User;

import java.io.IOException;

public class UserMapper {
   public static User stringJsonToUser(String json) throws IOException {
       return Mapper.objectMapper.readValue(json, User.class);
   }

    public static String userToJSON(User message) throws JsonProcessingException {
       return Mapper.objectMapper.writeValueAsString(message);
    }

    public static String requestToJSON(NettyServerManager.Request request) throws JsonProcessingException {
        return Mapper.objectMapper.writeValueAsString(request);
    }
}
