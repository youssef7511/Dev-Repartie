package com.auction.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Message générique pour la communication TCP
 */
public class Message implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        LOGIN_REQUEST,
        LOGIN_RESPONSE,
        BID_REQUEST,
        BID_RESPONSE,
        AUCTION_UPDATE,
        AUCTION_START,
        AUCTION_END,
        ERROR,
        DISCONNECT
    }
    
    private MessageType type;
    private String content;
    private String sender;
    private LocalDateTime timestamp;
    private Object data;
    
    public Message() {
        this.timestamp = LocalDateTime.now();
    }
    
    public Message(MessageType type, String content) {
        this();
        this.type = type;
        this.content = content;
    }
    
    public Message(MessageType type, String content, String sender) {
        this(type, content);
        this.sender = sender;
    }
    
    public Message(MessageType type, String content, Object data) {
        this(type, content);
        this.data = data;
    }
    
    // Getters et Setters
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", content='" + content + '\'' +
                ", sender='" + sender + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
