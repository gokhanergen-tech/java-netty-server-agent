package com.socket.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class User implements Serializable {
    public enum MessageType{
        TEXT,
        PROMPT,
        LAMA
    }


    private static final long serialVersionUID = 1L;
    private String name;
    private MessageType messageType;
    private String message;
    private String id;

    @JsonIgnore
    private String getFullName(){
        return String.format("%s %s", this.name);
    }

    public String getMessage() {
        return message;
    }

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", message='" + message + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}

