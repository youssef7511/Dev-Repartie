package com.auction.server;

import com.auction.common.dto.*;
import com.auction.common.dto.Message.MessageType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestionnaire central des enchères
 * Gère la synchronisation des accès concurrents (cf. TP5)
 */
public class AuctionManager {
    
    // Verrou pour la synchronisation des enchères
    private final ReentrantLock bidLock = new ReentrantLock();
    
    // Produit actuellement en vente
    private Product currentProduct;
    
    // Historique des ventes
    private final List<Product> salesHistory;
    
    // Clients connectés (clientId -> ClientHandler)
    private final Map<String, ClientHandler> connectedClients;
    
    // Clients bannis
    private final Set<String> bannedClients;
    
    // Diffuseur Multicast
    private final MulticastBroadcaster broadcaster;
    
    public AuctionManager(MulticastBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
        this.salesHistory = Collections.synchronizedList(new ArrayList<>());
        this.connectedClients = new ConcurrentHashMap<>();
        this.bannedClients = Collections.synchronizedSet(new HashSet<>());
    }
    
    /**
     * Démarre une nouvelle enchère
     * @param productName Nom du produit
     * @param description Description
     * @param startingPrice Prix de départ
     * @return true si l'enchère a été démarrée
     */
    public synchronized boolean startAuction(String productName, String description, double startingPrice) {
        // Vérifier qu'aucune enchère n'est en cours
        if (currentProduct != null && currentProduct.isActive()) {
            System.out.println("[AUCTION] Une enchère est déjà en cours");
            return false;
        }
        
        // Créer le nouveau produit
        String productId = UUID.randomUUID().toString().substring(0, 8);
        currentProduct = new Product(productId, productName, description, startingPrice);
        currentProduct.setActive(true);
        
        System.out.println("[AUCTION] Nouvelle enchère démarrée: " + productName + " - " + startingPrice + "€");
        
        // Diffuser via Multicast
        AuctionUpdate update = AuctionUpdate.newAuction(productId, productName, description, startingPrice);
        broadcaster.broadcast(update);
        
        // Notifier tous les clients connectés via TCP
        Message notification = new Message(MessageType.AUCTION_START, "Nouvelle enchère: " + productName, update);
        broadcastToClients(notification);
        
        return true;
    }
    
    /**
     * Place une enchère (synchronisé avec verrou)
     * @param bid La requête d'enchère
     * @return true si l'enchère est acceptée
     */
    public boolean placeBid(BidRequest bid) {
        bidLock.lock();
        try {
            // Vérifications
            if (currentProduct == null || !currentProduct.isActive()) {
                System.out.println("[AUCTION] Enchère refusée: pas de vente en cours");
                return false;
            }
            
            if (bid.getAmount() <= currentProduct.getCurrentPrice()) {
                System.out.println("[AUCTION] Enchère refusée: montant insuffisant (" + 
                    bid.getAmount() + "€ <= " + currentProduct.getCurrentPrice() + "€)");
                return false;
            }
            
            // Accepter l'enchère
            currentProduct.setCurrentPrice(bid.getAmount());
            currentProduct.setHighestBidderId(bid.getClientId());
            currentProduct.setHighestBidderName(bid.getClientName());
            
            System.out.println("[AUCTION] Enchère acceptée: " + bid.getAmount() + "€ par " + bid.getClientName());
            
            // Diffuser la mise à jour via Multicast
            AuctionUpdate update = AuctionUpdate.newBid(
                currentProduct.getId(),
                currentProduct.getName(),
                bid.getAmount(),
                bid.getClientId(),
                bid.getClientName()
            );
            broadcaster.broadcast(update);
            
            // Notifier tous les clients connectés via TCP
            Message notification = new Message(MessageType.AUCTION_UPDATE, 
                "Nouvelle enchère: " + bid.getAmount() + "€ par " + bid.getClientName(), update);
            broadcastToClients(notification);
            
            return true;
            
        } finally {
            bidLock.unlock();
        }
    }
    
    /**
     * Clôture l'enchère en cours
     * @return Le produit vendu
     */
    public synchronized Product stopAuction() {
        if (currentProduct == null || !currentProduct.isActive()) {
            System.out.println("[AUCTION] Pas d'enchère à clôturer");
            return null;
        }
        
        currentProduct.setActive(false);
        
        // Préparer le résultat
        Product soldProduct = currentProduct;
        
        // Diffuser la fin de l'enchère
        if (currentProduct.getHighestBidderId() != null) {
            System.out.println("[AUCTION] Enchère terminée: " + currentProduct.getName() + 
                " vendu à " + currentProduct.getHighestBidderName() + 
                " pour " + currentProduct.getCurrentPrice() + "€");
            
            AuctionUpdate update = AuctionUpdate.auctionClosed(
                currentProduct.getId(),
                currentProduct.getName(),
                currentProduct.getHighestBidderId(),
                currentProduct.getHighestBidderName(),
                currentProduct.getCurrentPrice()
            );
            broadcaster.broadcast(update);
            
            Message notification = new Message(MessageType.AUCTION_END, 
                "VENDU! " + currentProduct.getName() + " à " + currentProduct.getHighestBidderName(), update);
            broadcastToClients(notification);
            
        } else {
            System.out.println("[AUCTION] Enchère terminée sans enchérisseur");
            
            AuctionUpdate update = new AuctionUpdate();
            update.setUpdateType(AuctionUpdate.UpdateType.AUCTION_CLOSED);
            update.setProductId(currentProduct.getId());
            update.setProductName(currentProduct.getName());
            update.setMessage("Enchère terminée sans vente");
            broadcaster.broadcast(update);
            
            Message notification = new Message(MessageType.AUCTION_END, 
                "Enchère terminée sans vente", update);
            broadcastToClients(notification);
        }
        
        // Ajouter à l'historique
        salesHistory.add(soldProduct);
        
        // Réinitialiser le produit courant
        currentProduct = null;
        
        return soldProduct;
    }
    
    /**
     * Annule l'enchère en cours
     * @return true si l'enchère a été annulée
     */
    public synchronized boolean cancelAuction() {
        if (currentProduct == null || !currentProduct.isActive()) {
            return false;
        }
        
        System.out.println("[AUCTION] Enchère annulée: " + currentProduct.getName());
        
        AuctionUpdate update = new AuctionUpdate();
        update.setUpdateType(AuctionUpdate.UpdateType.AUCTION_CANCELLED);
        update.setProductId(currentProduct.getId());
        update.setProductName(currentProduct.getName());
        update.setMessage("Enchère annulée par l'administrateur");
        broadcaster.broadcast(update);
        
        Message notification = new Message(MessageType.AUCTION_END, "Enchère annulée", update);
        broadcastToClients(notification);
        
        currentProduct = null;
        return true;
    }
    
    /**
     * Bannit un client
     * @param clientId ID du client
     * @return true si le client a été banni
     */
    public boolean banClient(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            return false;
        }
        
        bannedClients.add(clientId);
        
        // Notifier le client s'il est connecté
        ClientHandler handler = connectedClients.get(clientId);
        if (handler != null) {
            Message notification = new Message(MessageType.ERROR, "Vous avez été banni par l'administrateur");
            handler.sendMessage(notification);
        }
        
        System.out.println("[AUCTION] Client banni: " + clientId);
        return true;
    }
    
    /**
     * Vérifie si un client est banni
     */
    public boolean isClientBanned(String clientId) {
        return bannedClients.contains(clientId);
    }
    
    /**
     * Enregistre un nouveau client
     */
    public void registerClient(String clientId, String clientName, ClientHandler handler) {
        connectedClients.put(clientId, handler);
        System.out.println("[AUCTION] Client enregistré: " + clientName + " (" + clientId + ")");
    }
    
    /**
     * Désenregistre un client
     */
    public void unregisterClient(String clientId) {
        connectedClients.remove(clientId);
        System.out.println("[AUCTION] Client désenregistré: " + clientId);
    }
    
    /**
     * Diffuse un message à tous les clients connectés via TCP
     */
    private void broadcastToClients(Message message) {
        for (ClientHandler handler : connectedClients.values()) {
            if (handler.isConnected()) {
                handler.sendMessage(message);
            }
        }
    }
    
    // Getters
    
    public Product getCurrentProduct() {
        return currentProduct;
    }
    
    public List<String> getConnectedClientIds() {
        return new ArrayList<>(connectedClients.keySet());
    }
    
    public List<String> getConnectedClientNames() {
        List<String> names = new ArrayList<>();
        for (ClientHandler handler : connectedClients.values()) {
            names.add(handler.getClientName() + " (" + handler.getClientId() + ")");
        }
        return names;
    }
    
    public List<Product> getSalesHistory() {
        return new ArrayList<>(salesHistory);
    }
    
    public int getConnectedClientCount() {
        return connectedClients.size();
    }
}
