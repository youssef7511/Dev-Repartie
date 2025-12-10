package com.auction.server;

import com.auction.common.dto.*;
import com.auction.common.dto.Message.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

/**
 * Gestionnaire de connexion pour un client TCP
 * Un thread par client (cf. TP3)
 */
public class ClientHandler implements Runnable {
    
    private final Socket socket;
    private final AuctionManager auctionManager;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String clientId;
    private String clientName;
    private boolean connected;
    
    public ClientHandler(Socket socket, AuctionManager auctionManager) {
        this.socket = socket;
        this.auctionManager = auctionManager;
        this.connected = false;
    }
    
    @Override
    public void run() {
        try {
            // Initialiser les flux (output en premier pour éviter le deadlock)
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            
            System.out.println("[CLIENT] Flux initialisés pour " + socket.getInetAddress());
            
            // Boucle principale de traitement des messages
            while (!socket.isClosed()) {
                try {
                    Message message = (Message) input.readObject();
                    handleMessage(message);
                } catch (EOFException e) {
                    // Client déconnecté
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("[CLIENT] Erreur de désérialisation: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.err.println("[CLIENT] Erreur de connexion: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    /**
     * Traite un message reçu du client
     */
    private void handleMessage(Message message) {
        System.out.println("[CLIENT " + (clientName != null ? clientName : "?") + "] Message reçu: " + message.getType());
        
        switch (message.getType()) {
            case LOGIN_REQUEST:
                handleLogin(message);
                break;
            case BID_REQUEST:
                handleBid(message);
                break;
            case DISCONNECT:
                disconnect();
                break;
            default:
                sendError("Type de message non supporté: " + message.getType());
        }
    }
    
    /**
     * Gère une demande de connexion
     */
    private void handleLogin(Message message) {
        String requestedName = message.getContent();
        
        if (requestedName == null || requestedName.trim().isEmpty()) {
            sendError("Nom d'utilisateur invalide");
            return;
        }
        
        // Générer un ID unique pour ce client
        this.clientId = UUID.randomUUID().toString().substring(0, 8);
        this.clientName = requestedName.trim();
        this.connected = true;
        
        // Enregistrer le client auprès du manager
        auctionManager.registerClient(clientId, clientName, this);
        
        // Envoyer la confirmation
        Message response = new Message(MessageType.LOGIN_RESPONSE, "Bienvenue " + clientName + "!");
        response.setData(clientId);
        sendMessage(response);
        
        // Envoyer l'état actuel de l'enchère si une vente est en cours
        Product currentProduct = auctionManager.getCurrentProduct();
        if (currentProduct != null && currentProduct.isActive()) {
            AuctionUpdate update = AuctionUpdate.newAuction(
                currentProduct.getId(),
                currentProduct.getName(),
                currentProduct.getDescription(),
                currentProduct.getStartingPrice()
            );
            update.setCurrentPrice(currentProduct.getCurrentPrice());
            update.setHighestBidder(currentProduct.getHighestBidderId());
            update.setHighestBidderName(currentProduct.getHighestBidderName());
            
            Message auctionInfo = new Message(MessageType.AUCTION_UPDATE, "Enchère en cours", update);
            sendMessage(auctionInfo);
        }
        
        System.out.println("[CLIENT] " + clientName + " (ID: " + clientId + ") connecté");
    }
    
    /**
     * Gère une enchère
     */
    private void handleBid(Message message) {
        if (!connected) {
            sendError("Vous devez vous connecter d'abord");
            return;
        }
        
        // Vérifier si le client est banni
        if (auctionManager.isClientBanned(clientId)) {
            sendError("Vous avez été banni de cette vente");
            return;
        }
        
        Object data = message.getData();
        if (!(data instanceof BidRequest)) {
            sendError("Format de requête d'enchère invalide");
            return;
        }
        
        BidRequest bid = (BidRequest) data;
        bid.setClientId(clientId);
        bid.setClientName(clientName);
        
        // Traiter l'enchère (synchronisé dans AuctionManager)
        boolean success = auctionManager.placeBid(bid);
        
        if (success) {
            Message response = new Message(MessageType.BID_RESPONSE, "Enchère acceptée: " + bid.getAmount() + "€");
            response.setData(true);
            sendMessage(response);
        } else {
            Message response = new Message(MessageType.BID_RESPONSE, "Enchère refusée - montant insuffisant");
            response.setData(false);
            sendMessage(response);
        }
    }
    
    /**
     * Envoie un message au client
     */
    public synchronized void sendMessage(Message message) {
        try {
            if (output != null && !socket.isClosed()) {
                output.writeObject(message);
                output.flush();
                output.reset(); // Important pour éviter les problèmes de cache
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Erreur d'envoi vers " + clientName + ": " + e.getMessage());
        }
    }
    
    /**
     * Envoie un message d'erreur au client
     */
    private void sendError(String errorMessage) {
        Message error = new Message(MessageType.ERROR, errorMessage);
        sendMessage(error);
    }
    
    /**
     * Déconnecte le client proprement
     */
    private void disconnect() {
        if (connected && clientId != null) {
            auctionManager.unregisterClient(clientId);
            System.out.println("[CLIENT] " + clientName + " déconnecté");
        }
        
        connected = false;
        
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignorer les erreurs de fermeture
        }
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getClientName() {
        return clientName;
    }
    
    public boolean isConnected() {
        return connected && !socket.isClosed();
    }
}
