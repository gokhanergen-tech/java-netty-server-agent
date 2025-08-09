package com.socket.client.model;

import java.io.Serializable;

public class UserJoinedMessage implements Serializable {
    private User user;
    private String roomId;

    public UserJoinedMessage() {
    }


    public UserJoinedMessage(User user, String roomId) {
        this.user = user;
        this.roomId = roomId;
    }

    public User getUser() {
        return user;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
