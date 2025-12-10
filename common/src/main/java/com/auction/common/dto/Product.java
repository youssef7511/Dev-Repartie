package com.auction.common.dto;

import java.io.Serializable;

/**
 * Représente un produit mis aux enchères
 */
public class Product implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private String highestBidderId;
    private String highestBidderName;
    private boolean active;
    
    public Product() {
        this.active = false;
    }
    
    public Product(String id, String name, String description, double startingPrice) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
    }
    
    // Getters et Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public double getStartingPrice() {
        return startingPrice;
    }
    
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }
    
    public double getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public String getHighestBidderId() {
        return highestBidderId;
    }
    
    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }
    
    public String getHighestBidderName() {
        return highestBidderName;
    }
    
    public void setHighestBidderName(String highestBidderName) {
        this.highestBidderName = highestBidderName;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", active=" + active +
                '}';
    }
}
