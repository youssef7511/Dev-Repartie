package com.auction.server;

import com.auction.common.dto.*;
import com.auction.common.dto.Message.MessageType;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestionnaire central des enchères
 * Gère la synchronisation des accès concurrents.
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
    
    // Dernière enchère par client (par vente)
    private final Map<String, Double> lastBids;
    
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00 'TND'");
    
    public AuctionManager(MulticastBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
        this.salesHistory = Collections.synchronizedList(new ArrayList<>());
        this.connectedClients = new ConcurrentHashMap<>();
        this.bannedClients = Collections.synchronizedSet(new HashSet<>());
        this.lastBids = new ConcurrentHashMap<>();
    }
    
    /**
     * Démarre une nouvelle enchère
     * @param productName Nom du produit
     * @param description Description
     * @param startingPrice Prix de départ
     * @return true si l'enchère a été démarrée
     */
    public boolean startAuction(String productName, String description, double startingPrice) {
        bidLock.lock();
        try {
            // Vérifier qu'aucune enchère n'est en cours
            if (currentProduct != null && currentProduct.isActive()) {
                System.out.println("[AUCTION] Une enchère est déjà en cours");
                return false;
            }
            
            // Créer le nouveau produit
            String productId = UUID.randomUUID().toString().substring(0, 8);
            currentProduct = new Product(productId, productName, description, startingPrice);
            currentProduct.setActive(true);
            lastBids.clear();
            
            System.out.println("[AUCTION] Nouvelle enchère démarrée: " + productName + " - " + startingPrice + " TND");
            
            // Diffuser via Multicast
            AuctionUpdate update = AuctionUpdate.newAuction(productId, productName, description, startingPrice);
            broadcaster.broadcast(update);
            
            // Notifier tous les clients connectés via TCP
            Message notification = new Message(MessageType.AUCTION_START, "Nouvelle enchère: " + productName, update);
            broadcastToClients(notification);
            
            return true;
        } finally {
            bidLock.unlock();
        }
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
                    bid.getAmount() + " TND <= " + currentProduct.getCurrentPrice() + " TND)");
                return false;
            }
            
            // Accepter l'enchère
            currentProduct.setCurrentPrice(bid.getAmount());
            currentProduct.setHighestBidderId(bid.getClientId());
            currentProduct.setHighestBidderName(bid.getClientName());
            lastBids.put(bid.getClientId(), bid.getAmount());
            
            System.out.println("[AUCTION] Enchère acceptée: " + bid.getAmount() + " TND par " + bid.getClientName());
            
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
                "Nouvelle enchère: " + bid.getAmount() + " TND par " + bid.getClientName(), update);
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
    public Product stopAuction() {
        bidLock.lock();
        try {
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
                    " pour " + currentProduct.getCurrentPrice() + " TND");
                
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
            lastBids.clear();
            
            return soldProduct;
        } finally {
            bidLock.unlock();
        }
    }
    
    /**
     * Annule l'enchère en cours
     * @return true si l'enchère a été annulée
     */
    public boolean cancelAuction() {
        bidLock.lock();
        try {
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
            lastBids.clear();
            return true;
        } finally {
            bidLock.unlock();
        }
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
        lastBids.remove(clientId);
        
        // Notifier le client s'il est connecté
        ClientHandler handler = connectedClients.get(clientId);
        if (handler != null) {
            handler.forceDisconnect("Vous avez été banni par l'administrateur");
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
        lastBids.remove(clientId);
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
            String label = handler.getClientName() + " (" + handler.getClientId() + ")";
            Double bid = lastBids.get(handler.getClientId());
            if (bid != null) {
                label += " - Offre: " + priceFormat.format(bid);
            }
            names.add(label);
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
