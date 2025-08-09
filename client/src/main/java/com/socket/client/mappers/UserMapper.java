package com.socket.client.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.socket.client.model.User;

import java.io.IOException;

public class UserMapper {
   public static User stringJsonToUser(String json) throws IOException {
       return Mapper.objectMapper.readValue(json, User.class);
   }

    public static String userToJSON(User message) throws JsonProcessingException {
       return Mapper.objectMapper.writeValueAsString(message);
    }
}
