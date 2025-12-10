package com.auction.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Requête d'enchère envoyée par un client
 */
public class BidRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String clientId;
    private String clientName;
    private String productId;
    private double amount;
    private LocalDateTime timestamp;
    
    public BidRequest() {
        this.timestamp = LocalDateTime.now();
    }
    
    public BidRequest(String clientId, String clientName, String productId, double amount) {
        this();
        this.clientId = clientId;
        this.clientName = clientName;
        this.productId = productId;
        this.amount = amount;
    }
    
    // Getters et Setters
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientName() {
        return clientName;
    }
    
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "BidRequest{" +
                "clientId='" + clientId + '\'' +
                ", clientName='" + clientName + '\'' +
                ", productId='" + productId + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}
