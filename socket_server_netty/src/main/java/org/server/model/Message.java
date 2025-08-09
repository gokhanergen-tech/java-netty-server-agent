package org.server.model;

public class Message<T> {
    T messageObject;
    private MessageTopic topic;

    public enum MessageTopic{
        ON_JOIN,
        ON_MESSAGE,
        ON_JOIN_SELF, ON_LEAVE
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
}
