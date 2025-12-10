package com.auction.admin;

import com.auction.common.constants.NetworkConfig;
import com.auction.common.dto.Product;
import com.auction.common.rmi.IAuctionAdmin;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

/**
 * Client Administrateur RMI
 * Permet de contrôler le serveur d'enchères à distance
 */
public class AdminClient {
    
    private IAuctionAdmin auctionAdmin;
    private Scanner scanner;
    private boolean running;
    
    public AdminClient() {
        this.scanner = new Scanner(System.in);
        this.running = false;
    }
    
    /**
     * Se connecte au serveur RMI
     */
    public boolean connect() {
        try {
            System.out.println("Connexion au serveur RMI...");
            Registry registry = LocateRegistry.getRegistry(
                NetworkConfig.SERVER_HOST, 
                NetworkConfig.RMI_PORT
            );
            
            auctionAdmin = (IAuctionAdmin) registry.lookup(NetworkConfig.RMI_SERVICE_NAME);
            
            // Test de connexion
            if (auctionAdmin.ping()) {
                System.out.println("✓ Connecté au serveur d'enchères!");
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("✗ Erreur de connexion: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Démarre la boucle interactive
     */
    public void start() {
        if (!connect()) {
            System.out.println("Impossible de se connecter au serveur. Vérifiez que le serveur est démarré.");
            return;
        }
        
        running = true;
        
        printHeader();
        
        while (running) {
            printMenu();
            
            String choice = scanner.nextLine().trim();
            
            try {
                handleChoice(choice);
            } catch (Exception e) {
                System.err.println("Erreur: " + e.getMessage());
            }
            
            System.out.println();
        }
    }
    
    /**
     * Affiche l'en-tête
     */
    private void printHeader() {
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║     CONSOLE D'ADMINISTRATION e-Auction     ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.println("║  Gérez les enchères depuis cette console   ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
    }
    
    /**
     * Affiche le menu principal
     */
    private void printMenu() {
        System.out.println("┌──────────────────────────────────────┐");
        System.out.println("│            MENU PRINCIPAL            │");
        System.out.println("├──────────────────────────────────────┤");
        System.out.println("│  1. Démarrer une nouvelle enchère    │");
        System.out.println("│  2. Clôturer l'enchère en cours      │");
        System.out.println("│  3. Annuler l'enchère en cours       │");
        System.out.println("│  4. Voir le statut de l'enchère      │");
        System.out.println("│  5. Lister les clients connectés     │");
        System.out.println("│  6. Bannir un client                 │");
        System.out.println("│  7. Voir l'historique des ventes     │");
        System.out.println("│  0. Quitter                          │");
        System.out.println("└──────────────────────────────────────┘");
        System.out.print("Votre choix: ");
    }
    
    /**
     * Traite le choix de l'utilisateur
     */
    private void handleChoice(String choice) throws Exception {
        switch (choice) {
            case "1":
                startNewAuction();
                break;
            case "2":
                stopAuction();
                break;
            case "3":
                cancelAuction();
                break;
            case "4":
                showAuctionStatus();
                break;
            case "5":
                listConnectedClients();
                break;
            case "6":
                banClient();
                break;
            case "7":
                showSalesHistory();
                break;
            case "0":
                running = false;
                System.out.println("Au revoir!");
                break;
            default:
                System.out.println("Choix invalide. Veuillez réessayer.");
        }
    }
    
    /**
     * Démarre une nouvelle enchère
     */
    private void startNewAuction() throws Exception {
        System.out.println("\n=== NOUVELLE ENCHÈRE ===");
        
        System.out.print("Nom du produit: ");
        String name = scanner.nextLine().trim();
        
        if (name.isEmpty()) {
            System.out.println("Nom invalide!");
            return;
        }
        
        System.out.print("Description: ");
        String description = scanner.nextLine().trim();
        
        System.out.print("Prix de départ (TND): ");
        double price;
        try {
            price = Double.parseDouble(scanner.nextLine().trim());
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Prix invalide!");
            return;
        }
        
        boolean success = auctionAdmin.startAuction(name, description, price);
        
        if (success) {
            System.out.println("\n✓ Enchère démarrée avec succès!");
            System.out.println("  Produit: " + name);
            System.out.println("  Prix de départ: " + price + " TND");
        } else {
            System.out.println("\n✗ Impossible de démarrer l'enchère.");
            System.out.println("  Une enchère est peut-être déjà en cours.");
        }
    }
    
    /**
     * Clôture l'enchère en cours
     */
    private void stopAuction() throws Exception {
        System.out.println("\n=== CLÔTURER L'ENCHÈRE ===");
        
        System.out.print("Confirmer la clôture? (o/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (!confirm.equals("o") && !confirm.equals("oui")) {
            System.out.println("Opération annulée.");
            return;
        }
        
        Product result = auctionAdmin.stopAuction();
        
        if (result != null) {
            System.out.println("\n✓ Enchère clôturée!");
            System.out.println("  Produit: " + result.getName());
            System.out.println("  Prix final: " + result.getCurrentPrice() + " TND");
            
            if (result.getHighestBidderName() != null) {
                System.out.println("  Gagnant: " + result.getHighestBidderName());
            } else {
                System.out.println("  Aucun enchérisseur");
            }
        } else {
            System.out.println("\n✗ Aucune enchère en cours à clôturer.");
        }
    }
    
    /**
     * Annule l'enchère en cours
     */
    private void cancelAuction() throws Exception {
        System.out.println("\n=== ANNULER L'ENCHÈRE ===");
        
        System.out.print("Confirmer l'annulation? (o/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (!confirm.equals("o") && !confirm.equals("oui")) {
            System.out.println("Opération annulée.");
            return;
        }
        
        boolean success = auctionAdmin.cancelAuction();
        
        if (success) {
            System.out.println("\n✓ Enchère annulée avec succès!");
        } else {
            System.out.println("\n✗ Aucune enchère en cours à annuler.");
        }
    }
    
    /**
     * Affiche le statut de l'enchère en cours
     */
    private void showAuctionStatus() throws Exception {
        System.out.println("\n=== STATUT DE L'ENCHÈRE ===");
        
        Product current = auctionAdmin.getAuctionStatus();
        
        if (current != null && current.isActive()) {
            System.out.println("┌────────────────────────────────────┐");
            System.out.println("│ ENCHÈRE EN COURS                   │");
            System.out.println("├────────────────────────────────────┤");
            System.out.println("│ Produit: " + padRight(current.getName(), 26) + "│");
            System.out.println("│ Prix actuel: " + padRight(current.getCurrentPrice() + " TND", 22) + "│");
            String bidder = current.getHighestBidderName() != null ? current.getHighestBidderName() : "-";
            System.out.println("│ Meilleur enchérisseur: " + padRight(bidder, 12) + "│");
            System.out.println("└────────────────────────────────────┘");
        } else {
            System.out.println("Aucune enchère en cours.");
        }
    }
    
    /**
     * Liste les clients connectés
     */
    private void listConnectedClients() throws Exception {
        System.out.println("\n=== CLIENTS CONNECTÉS ===");
        
        List<String> clients = auctionAdmin.getConnectedClients();
        
        if (clients.isEmpty()) {
            System.out.println("Aucun client connecté.");
        } else {
            System.out.println("Nombre de clients: " + clients.size());
            System.out.println("─────────────────────────────");
            for (int i = 0; i < clients.size(); i++) {
                System.out.println((i + 1) + ". " + clients.get(i));
            }
            System.out.println("─────────────────────────────");
        }
    }
    
    /**
     * Bannit un client
     */
    private void banClient() throws Exception {
        System.out.println("\n=== BANNIR UN CLIENT ===");
        
        // Afficher d'abord la liste
        listConnectedClients();
        
        System.out.print("\nID du client à bannir: ");
        String clientId = scanner.nextLine().trim();
        
        if (clientId.isEmpty()) {
            System.out.println("ID invalide!");
            return;
        }
        
        System.out.print("Confirmer le bannissement? (o/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (!confirm.equals("o") && !confirm.equals("oui")) {
            System.out.println("Opération annulée.");
            return;
        }
        
        boolean success = auctionAdmin.banClient(clientId);
        
        if (success) {
            System.out.println("\n✓ Client banni avec succès!");
        } else {
            System.out.println("\n✗ Impossible de bannir ce client.");
        }
    }
    
    /**
     * Affiche l'historique des ventes
     */
    private void showSalesHistory() throws Exception {
        System.out.println("\n=== HISTORIQUE DES VENTES ===");
        
        List<Product> history = auctionAdmin.getSalesHistory();
        
        if (history.isEmpty()) {
            System.out.println("Aucune vente enregistrée.");
        } else {
            System.out.println("─────────────────────────────────────────────────────────");
            System.out.printf("%-5s %-20s %-15s %-15s%n", "N°", "Produit", "Prix final", "Gagnant");
            System.out.println("─────────────────────────────────────────────────────────");
            
            for (int i = 0; i < history.size(); i++) {
                Product p = history.get(i);
                String winner = p.getHighestBidderName() != null ? p.getHighestBidderName() : "Aucun";
                System.out.printf("%-5d %-20s %-15s %-15s%n", 
                    (i + 1), 
                    truncate(p.getName(), 20), 
                    p.getCurrentPrice() + " TND",
                    truncate(winner, 15));
            }
            
            System.out.println("─────────────────────────────────────────────────────────");
            System.out.println("Total des ventes: " + history.size());
        }
    }
    
    // Méthodes utilitaires
    
    private String padRight(String s, int length) {
        if (s.length() >= length) return s.substring(0, length);
        return String.format("%-" + length + "s", s);
    }
    
    private String truncate(String s, int length) {
        if (s.length() <= length) return s;
        return s.substring(0, length - 3) + "...";
    }
    
    public static void main(String[] args) {
        System.out.println("Démarrage du client administrateur...\n");
        
        AdminClient client = new AdminClient();
        client.start();
    }
}
