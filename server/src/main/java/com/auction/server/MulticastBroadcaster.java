package com.auction.server;

import com.auction.common.constants.NetworkConfig;
import com.auction.common.dto.AuctionUpdate;

import java.io.*;
import java.net.*;

/**
 * Diffuseur Multicast pour envoyer les mises à jour en temps réel
 * à tous les clients connectés
 */
public class MulticastBroadcaster {
    
    private final String multicastGroup;
    private final int multicastPort;
    private MulticastSocket socket;
    private InetAddress group;
    
    public MulticastBroadcaster() {
        this.multicastGroup = NetworkConfig.MULTICAST_GROUP;
        this.multicastPort = NetworkConfig.MULTICAST_PORT;
        initialize();
    }
    
    /**
     * Initialise le socket Multicast
     */
    private void initialize() {
        try {
            socket = new MulticastSocket();
            group = InetAddress.getByName(multicastGroup);
            System.out.println("[MULTICAST] Broadcaster initialisé sur " + multicastGroup + ":" + multicastPort);
        } catch (IOException e) {
            System.err.println("[MULTICAST] Erreur d'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Diffuse une mise à jour d'enchère à tous les clients
     * @param update La mise à jour à diffuser
     */
    public void broadcast(AuctionUpdate update) {
        if (socket == null || socket.isClosed()) {
            System.err.println("[MULTICAST] Socket non disponible");
            return;
        }
        
        try {
            // Sérialiser l'objet AuctionUpdate
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(update);
            oos.flush();
            
            byte[] data = baos.toByteArray();
            
            // Créer et envoyer le datagramme
            DatagramPacket packet = new DatagramPacket(
                data, 
                data.length, 
                group, 
                multicastPort
            );
            
            socket.send(packet);
            
            System.out.println("[MULTICAST] Diffusé: " + update.getMessage());
            
        } catch (IOException e) {
            System.err.println("[MULTICAST] Erreur de diffusion: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Diffuse un message simple (texte)
     * @param message Le message à diffuser
     */
    public void broadcastMessage(String message) {
        if (socket == null || socket.isClosed()) {
            System.err.println("[MULTICAST] Socket non disponible");
            return;
        }
        
        try {
            byte[] data = message.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(
                data, 
                data.length, 
                group, 
                multicastPort
            );
            
            socket.send(packet);
            System.out.println("[MULTICAST] Message diffusé: " + message);
            
        } catch (IOException e) {
            System.err.println("[MULTICAST] Erreur de diffusion: " + e.getMessage());
        }
    }
    
    /**
     * Ferme le socket Multicast
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            System.out.println("[MULTICAST] Broadcaster fermé");
        }
    }
}
