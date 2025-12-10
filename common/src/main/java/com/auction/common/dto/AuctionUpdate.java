package com.auction.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Mise à jour d'enchère diffusée en Multicast
 */
public class AuctionUpdate implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public enum UpdateType {
        NEW_AUCTION,      // Nouvelle vente démarrée
        NEW_BID,          // Nouvelle enchère validée
        AUCTION_CLOSED,   // Vente terminée
        AUCTION_CANCELLED // Vente annulée
    }
    
    private UpdateType updateType;
    private String productId;
    private String productName;
    private String productDescription;
    private double currentPrice;
    private double startingPrice;
    private String highestBidder;
    private String highestBidderName;
    private String winnerId;
    private String winnerName;
    private double winningPrice;
    private LocalDateTime timestamp;
    private String message;
    
    public AuctionUpdate() {
        this.timestamp = LocalDateTime.now();
    }
    
    public AuctionUpdate(UpdateType updateType, String productId, String productName, double currentPrice) {
        this();
        this.updateType = updateType;
        this.productId = productId;
        this.productName = productName;
        this.currentPrice = currentPrice;
    }
    
    // Méthodes statiques pour créer des mises à jour
    public static AuctionUpdate newAuction(String productId, String productName, String description, double startingPrice) {
        AuctionUpdate update = new AuctionUpdate(UpdateType.NEW_AUCTION, productId, productName, startingPrice);
        update.setProductDescription(description);
        update.setStartingPrice(startingPrice);
        update.setMessage("Nouvelle vente: " + productName + " - Prix de départ: " + startingPrice + "€");
        return update;
    }
    
    public static AuctionUpdate newBid(String productId, String productName, double newPrice, String bidderId, String bidderName) {
        AuctionUpdate update = new AuctionUpdate(UpdateType.NEW_BID, productId, productName, newPrice);
        update.setHighestBidder(bidderId);
        update.setHighestBidderName(bidderName);
        update.setMessage("Nouvelle enchère: " + newPrice + "€ par " + bidderName);
        return update;
    }
    
    public static AuctionUpdate auctionClosed(String productId, String productName, String winnerId, String winnerName, double finalPrice) {
        AuctionUpdate update = new AuctionUpdate(UpdateType.AUCTION_CLOSED, productId, productName, finalPrice);
        update.setWinnerId(winnerId);
        update.setWinnerName(winnerName);
        update.setWinningPrice(finalPrice);
        update.setMessage("VENDU! " + productName + " à " + winnerName + " pour " + finalPrice + "€");
        return update;
    }
    
    // Getters et Setters
    public UpdateType getUpdateType() {
        return updateType;
    }
    
    public void setUpdateType(UpdateType updateType) {
        this.updateType = updateType;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductDescription() {
        return productDescription;
    }
    
    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }
    
    public double getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public double getStartingPrice() {
        return startingPrice;
    }
    
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }
    
    public String getHighestBidder() {
        return highestBidder;
    }
    
    public void setHighestBidder(String highestBidder) {
        this.highestBidder = highestBidder;
    }
    
    public String getHighestBidderName() {
        return highestBidderName;
    }
    
    public void setHighestBidderName(String highestBidderName) {
        this.highestBidderName = highestBidderName;
    }
    
    public String getWinnerId() {
        return winnerId;
    }
    
    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }
    
    public String getWinnerName() {
        return winnerName;
    }
    
    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }
    
    public double getWinningPrice() {
        return winningPrice;
    }
    
    public void setWinningPrice(double winningPrice) {
        this.winningPrice = winningPrice;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "AuctionUpdate{" +
                "updateType=" + updateType +
                ", productName='" + productName + '\'' +
                ", currentPrice=" + currentPrice +
                ", message='" + message + '\'' +
                '}';
    }
}
