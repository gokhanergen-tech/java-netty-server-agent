package org.server.model;

import java.io.Serializable;
import java.util.UUID;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String surname;
    private String message;
    private String id;

    public User(String name,String surname){
        this.id= UUID.randomUUID().toString();
        this.name=name;
        this.surname=surname;
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

    public String getSurname() {
        return surname;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }
}
