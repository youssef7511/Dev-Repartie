package com.auction.common.rmi;

import com.auction.common.dto.Product;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface RMI pour l'administration des enchères
 */
public interface IAuctionAdmin extends Remote {
    
    /**
     * Démarre une nouvelle enchère pour un produit
     * @param productName Nom du produit
     * @param description Description du produit
     * @param startingPrice Prix de départ
     * @return true si l'enchère a été démarrée avec succès
     */
    boolean startAuction(String productName, String description, double startingPrice) throws RemoteException;
    
    /**
     * Clôture l'enchère en cours
     * @return Le produit vendu avec les informations du gagnant, null si pas de vente en cours
     */
    Product stopAuction() throws RemoteException;
    
    /**
     * Bannit un client du système
     * @param clientId Identifiant du client à bannir
     * @return true si le client a été banni avec succès
     */
    boolean banClient(String clientId) throws RemoteException;
    
    /**
     * Obtient le statut actuel de l'enchère
     * @return Le produit actuellement en vente, null si pas de vente en cours
     */
    Product getAuctionStatus() throws RemoteException;
    
    /**
     * Obtient la liste des clients connectés
     * @return Libellés des clients connectés (nom + ID, dernière offre éventuelle)
     */
    List<String> getConnectedClients() throws RemoteException;
    
    /**
     * Obtient l'historique des ventes
     * @return Liste des produits vendus
     */
    List<Product> getSalesHistory() throws RemoteException;
    
    /**
     * Annule l'enchère en cours sans déclarer de gagnant
     * @return true si l'enchère a été annulée
     */
    boolean cancelAuction() throws RemoteException;
    
    /**
     * Vérifie si le serveur est actif
     * @return true si le serveur fonctionne
     */
    boolean ping() throws RemoteException;
}
