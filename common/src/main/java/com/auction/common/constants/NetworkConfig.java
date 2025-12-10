package com.auction.common.constants;

/**
 * Configuration réseau partagée entre tous les modules
 */
public class NetworkConfig {
    
    // Configuration TCP
    public static final String SERVER_HOST = "localhost";
    public static final int TCP_PORT = 5000;
    
    // Configuration Multicast
    public static final String MULTICAST_GROUP = "225.1.1.1";
    public static final int MULTICAST_PORT = 6000;
    
    // Configuration RMI
    public static final int RMI_PORT = 1099;
    public static final String RMI_SERVICE_NAME = "AuctionAdmin";
    
    // Timeouts
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 30000;
    
    private NetworkConfig() {
        // Classe utilitaire non instanciable
    }
}
