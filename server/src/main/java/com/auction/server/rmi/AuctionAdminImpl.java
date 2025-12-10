package com.auction.server.rmi;

import com.auction.common.dto.Product;
import com.auction.common.rmi.IAuctionAdmin;
import com.auction.server.AuctionManager;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * Implémentation du service RMI pour l'administration des enchères (cf. TP6)
 */
public class AuctionAdminImpl extends UnicastRemoteObject implements IAuctionAdmin {
    
    private static final long serialVersionUID = 1L;
    
    private final AuctionManager auctionManager;
    
    public AuctionAdminImpl(AuctionManager auctionManager) throws RemoteException {
        super();
        this.auctionManager = auctionManager;
    }
    
    @Override
    public boolean startAuction(String productName, String description, double startingPrice) throws RemoteException {
        System.out.println("[RMI] Demande de démarrage d'enchère: " + productName);
        return auctionManager.startAuction(productName, description, startingPrice);
    }
    
    @Override
    public Product stopAuction() throws RemoteException {
        System.out.println("[RMI] Demande de clôture d'enchère");
        return auctionManager.stopAuction();
    }
    
    @Override
    public boolean banClient(String clientId) throws RemoteException {
        System.out.println("[RMI] Demande de bannissement: " + clientId);
        return auctionManager.banClient(clientId);
    }
    
    @Override
    public Product getAuctionStatus() throws RemoteException {
        return auctionManager.getCurrentProduct();
    }
    
    @Override
    public List<String> getConnectedClients() throws RemoteException {
        return auctionManager.getConnectedClientNames();
    }
    
    @Override
    public List<Product> getSalesHistory() throws RemoteException {
        return auctionManager.getSalesHistory();
    }
    
    @Override
    public boolean cancelAuction() throws RemoteException {
        System.out.println("[RMI] Demande d'annulation d'enchère");
        return auctionManager.cancelAuction();
    }
    
    @Override
    public boolean ping() throws RemoteException {
        return true;
    }
}
