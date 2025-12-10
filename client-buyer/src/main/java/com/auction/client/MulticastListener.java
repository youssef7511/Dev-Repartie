package com.auction.client;

import com.auction.common.constants.NetworkConfig;
import com.auction.common.dto.AuctionUpdate;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * Listener Multicast pour recevoir les mises à jour en temps réel (cf. TP4, TP5)
 */
public class MulticastListener implements Runnable {
    
    private final String multicastGroup;
    private final int multicastPort;
    private MulticastSocket socket;
    private InetAddress group;
    private Thread listenerThread;
    private boolean running;
    private final Consumer<AuctionUpdate> updateHandler;
    
    public MulticastListener(Consumer<AuctionUpdate> updateHandler) {
        this.multicastGroup = NetworkConfig.MULTICAST_GROUP;
        this.multicastPort = NetworkConfig.MULTICAST_PORT;
        this.updateHandler = updateHandler;
        this.running = false;
    }
    
    /**
     * Démarre le listener dans un thread séparé
     */
    public void start() {
        if (running) return;
        
        listenerThread = new Thread(this);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    /**
     * Arrête le listener
     */
    public void stop() {
        running = false;
        
        if (socket != null && !socket.isClosed()) {
            try {
                socket.leaveGroup(group);
            } catch (IOException e) {
                // Ignorer
            }
            socket.close();
        }
        
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
    
    @Override
    public void run() {
        try {
            // Initialiser le socket Multicast
            socket = new MulticastSocket(multicastPort);
            group = InetAddress.getByName(multicastGroup);
            socket.joinGroup(group);
            socket.setSoTimeout(1000); // Timeout pour permettre l'arrêt propre
            
            running = true;
            System.out.println("[MULTICAST] Écoute sur " + multicastGroup + ":" + multicastPort);
            
            byte[] buffer = new byte[4096];
            
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    // Désérialiser l'objet AuctionUpdate
                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object received = ois.readObject();
                    
                    if (received instanceof AuctionUpdate) {
                        AuctionUpdate update = (AuctionUpdate) received;
                        if (updateHandler != null) {
                            updateHandler.accept(update);
                        }
                    }
                    
                } catch (SocketTimeoutException e) {
                    // Timeout normal, continuer
                } catch (IOException | ClassNotFoundException e) {
                    if (running) {
                        System.err.println("[MULTICAST] Erreur de réception: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("[MULTICAST] Erreur d'initialisation: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.leaveGroup(group);
                } catch (IOException e) {
                    // Ignorer
                }
                socket.close();
            }
            System.out.println("[MULTICAST] Listener arrêté");
        }
    }
    
    public boolean isRunning() {
        return running;
    }
}
