package org.server.model;

import java.io.Serializable;
import java.util.UUID;

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

    public User(String name){
        this.id= UUID.randomUUID().toString();
        this.name=name;
    }

    public User() {}

    public String getName() {
        return name;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
