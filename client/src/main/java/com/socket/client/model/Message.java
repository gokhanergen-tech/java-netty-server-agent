package com.socket.client.model;

public class Message<T> {
    T messageObject;
    private MessageTopic topic;

    public enum MessageTopic{
        ON_JOIN,
        ON_MESSAGE,
        ON_LEAVE,
        ON_JOIN_SELF
    }

    public Message() {}

    public Message(MessageTopic topic, T t){
       this.messageObject = t;
       this.topic = topic;
    }

    public MessageTopic getTopic() {
        return topic;
    }

    public T getMessageObject() {
        return messageObject;
    }

    public void setMessageObject(T messageObject) {
        this.messageObject = messageObject;
    }

    public void setTopic(MessageTopic topic) {
        this.topic = topic;
    }
}
