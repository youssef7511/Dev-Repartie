package com.auction.server;

import com.auction.common.constants.NetworkConfig;
import com.auction.server.rmi.AuctionAdminImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serveur principal d'enchères
 * Gère les connexions TCP des clients et expose le service RMI pour l'administration
 */
public class AuctionServer {
    
    private final int tcpPort;
    private final ExecutorService threadPool;
    private final AuctionManager auctionManager;
    private final MulticastBroadcaster broadcaster;
    private ServerSocket serverSocket;
    private boolean running;
    
    public AuctionServer() {
        this.tcpPort = NetworkConfig.TCP_PORT;
        this.threadPool = Executors.newFixedThreadPool(10); // ThreadPool limité à 10 clients
        this.broadcaster = new MulticastBroadcaster();
        this.auctionManager = new AuctionManager(broadcaster);
        this.running = false;
    }
    
    /**
     * Démarre le serveur
     */
    public void start() {
        try {
            // Démarrer le service RMI
            startRmiService();
            
            // Démarrer le serveur TCP
            startTcpServer();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Démarre le service RMI pour l'administration
     */
    private void startRmiService() {
        try {
            AuctionAdminImpl adminService = new AuctionAdminImpl(auctionManager);
            
            // Créer ou obtenir le registre RMI
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(NetworkConfig.RMI_PORT);
                System.out.println("[RMI] Registre RMI créé sur le port " + NetworkConfig.RMI_PORT);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(NetworkConfig.RMI_PORT);
                System.out.println("[RMI] Registre RMI existant utilisé sur le port " + NetworkConfig.RMI_PORT);
            }
            
            // Enregistrer le service
            registry.rebind(NetworkConfig.RMI_SERVICE_NAME, adminService);
            System.out.println("[RMI] Service '" + NetworkConfig.RMI_SERVICE_NAME + "' enregistré avec succès");
            
        } catch (Exception e) {
            System.err.println("[RMI] Erreur lors du démarrage du service RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Démarre le serveur TCP pour les clients
     */
    private void startTcpServer() {
        try {
            serverSocket = new ServerSocket(tcpPort);
            running = true;
            
            System.out.println("=========================================");
            System.out.println("   SERVEUR D'ENCHÈRES e-Auction");
            System.out.println("=========================================");
            System.out.println("[TCP] Serveur démarré sur le port " + tcpPort);
            System.out.println("[MULTICAST] Groupe: " + NetworkConfig.MULTICAST_GROUP + ":" + NetworkConfig.MULTICAST_PORT);
            System.out.println("[RMI] Service: " + NetworkConfig.RMI_SERVICE_NAME);
            System.out.println("=========================================");
            System.out.println("En attente de connexions...\n");
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[TCP] Nouvelle connexion de: " + clientSocket.getInetAddress());
                    
                    // Créer un handler pour ce client et le soumettre au pool
                    ClientHandler handler = new ClientHandler(clientSocket, auctionManager);
                    threadPool.submit(handler);
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[TCP] Erreur lors de l'acceptation d'une connexion: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("[TCP] Erreur lors du démarrage du serveur TCP: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Arrête le serveur proprement
     */
    public void stop() {
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture du serveur: " + e.getMessage());
        }
        
        threadPool.shutdown();
        broadcaster.close();
        
        System.out.println("\n[SERVEUR] Arrêt du serveur d'enchères");
    }
    
    public AuctionManager getAuctionManager() {
        return auctionManager;
    }
    
    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();
        
        // Ajouter un hook pour arrêter proprement le serveur
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nArrêt en cours...");
            server.stop();
        }));
        
        server.start();
    }
}
