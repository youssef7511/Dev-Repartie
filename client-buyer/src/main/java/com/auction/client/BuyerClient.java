package com.auction.client;

import com.auction.common.constants.NetworkConfig;
import com.auction.common.dto.*;
import com.auction.common.dto.Message.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Client Acheteur avec interface Swing
 * Se connecte au serveur via TCP et écoute le Multicast (cf. TP3, TP4)
 */
public class BuyerClient extends JFrame {
    
    private static final long serialVersionUID = 1L;
    
    // Connexion TCP
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String clientId;
    private String clientName;
    private boolean connected;
    
    // Listener Multicast
    private MulticastListener multicastListener;
    
    // Composants UI
    private JTextField nameField;
    private JButton connectButton;
    private JLabel statusLabel;
    private JLabel productLabel;
    private JLabel priceLabel;
    private JLabel highestBidderLabel;
    private JTextField bidField;
    private JButton bidButton;
    private JTextArea historyArea;
    private JPanel auctionPanel;
    
    // État de l'enchère
    private Product currentProduct;
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00 €");
    
    public BuyerClient() {
        initializeUI();
        this.connected = false;
    }
    
    /**
     * Initialise l'interface graphique Swing
     */
    private void initializeUI() {
        setTitle("e-Auction - Client Acheteur");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        
        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // === Panel de connexion (Nord) ===
        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Connexion"));
        
        connectionPanel.add(new JLabel("Nom:"));
        nameField = new JTextField(15);
        connectionPanel.add(nameField);
        
        connectButton = new JButton("Se connecter");
        connectButton.addActionListener(e -> toggleConnection());
        connectionPanel.add(connectButton);
        
        statusLabel = new JLabel("Déconnecté");
        statusLabel.setForeground(Color.RED);
        connectionPanel.add(statusLabel);
        
        mainPanel.add(connectionPanel, BorderLayout.NORTH);
        
        // === Panel d'enchère (Centre) ===
        auctionPanel = new JPanel(new BorderLayout(10, 10));
        auctionPanel.setBorder(BorderFactory.createTitledBorder("Enchère en cours"));
        
        // Informations sur le produit
        JPanel productPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        
        productLabel = new JLabel("Aucun produit en vente", SwingConstants.CENTER);
        productLabel.setFont(new Font("Arial", Font.BOLD, 16));
        productPanel.add(productLabel);
        
        priceLabel = new JLabel("Prix actuel: -", SwingConstants.CENTER);
        priceLabel.setFont(new Font("Arial", Font.BOLD, 24));
        priceLabel.setForeground(new Color(0, 100, 0));
        productPanel.add(priceLabel);
        
        highestBidderLabel = new JLabel("Meilleur enchérisseur: -", SwingConstants.CENTER);
        productPanel.add(highestBidderLabel);
        
        auctionPanel.add(productPanel, BorderLayout.CENTER);
        
        // Panel d'enchère
        JPanel bidPanel = new JPanel(new FlowLayout());
        bidPanel.add(new JLabel("Votre enchère (€):"));
        bidField = new JTextField(10);
        bidField.addActionListener(e -> placeBid());
        bidPanel.add(bidField);
        
        bidButton = new JButton("Enchérir");
        bidButton.setEnabled(false);
        bidButton.addActionListener(e -> placeBid());
        bidPanel.add(bidButton);
        
        auctionPanel.add(bidPanel, BorderLayout.SOUTH);
        
        mainPanel.add(auctionPanel, BorderLayout.CENTER);
        
        // === Panel historique (Sud) ===
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Historique"));
        
        historyArea = new JTextArea(8, 50);
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(historyArea);
        historyPanel.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(historyPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Gestion de la fermeture
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }
    
    /**
     * Bascule entre connexion et déconnexion
     */
    private void toggleConnection() {
        if (!connected) {
            connect();
        } else {
            disconnect();
        }
    }
    
    /**
     * Se connecte au serveur
     */
    private void connect() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer votre nom", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // Connexion TCP
            socket = new Socket(NetworkConfig.SERVER_HOST, NetworkConfig.TCP_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            
            // Envoyer la demande de connexion
            Message loginRequest = new Message(MessageType.LOGIN_REQUEST, name);
            output.writeObject(loginRequest);
            output.flush();
            
            // Démarrer le thread de réception TCP
            new Thread(this::receiveMessages).start();
            
            // Démarrer le listener Multicast
            multicastListener = new MulticastListener(this::handleMulticastUpdate);
            multicastListener.start();
            
            this.clientName = name;
            updateConnectionStatus(true);
            addHistory("Connexion en cours...");
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Impossible de se connecter au serveur:\n" + e.getMessage(), 
                "Erreur de connexion", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Se déconnecte du serveur
     */
    private void disconnect() {
        if (connected) {
            try {
                // Envoyer un message de déconnexion
                if (output != null) {
                    Message disconnectMsg = new Message(MessageType.DISCONNECT, "");
                    output.writeObject(disconnectMsg);
                    output.flush();
                }
            } catch (IOException e) {
                // Ignorer
            }
        }
        
        // Arrêter le listener Multicast
        if (multicastListener != null) {
            multicastListener.stop();
        }
        
        // Fermer les connexions
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Ignorer
        }
        
        updateConnectionStatus(false);
        addHistory("Déconnecté du serveur");
    }
    
    /**
     * Thread de réception des messages TCP
     */
    private void receiveMessages() {
        try {
            while (connected && !socket.isClosed()) {
                Message message = (Message) input.readObject();
                SwingUtilities.invokeLater(() -> handleMessage(message));
            }
        } catch (EOFException e) {
            // Connexion fermée par le serveur
            SwingUtilities.invokeLater(() -> {
                updateConnectionStatus(false);
                addHistory("Connexion fermée par le serveur");
            });
        } catch (IOException | ClassNotFoundException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    updateConnectionStatus(false);
                    addHistory("Erreur de connexion: " + e.getMessage());
                });
            }
        }
    }
    
    /**
     * Traite un message reçu via TCP
     */
    private void handleMessage(Message message) {
        switch (message.getType()) {
            case LOGIN_RESPONSE:
                clientId = (String) message.getData();
                addHistory("Connecté avec succès! ID: " + clientId);
                break;
                
            case BID_RESPONSE:
                boolean success = (Boolean) message.getData();
                if (success) {
                    addHistory("✓ " + message.getContent());
                } else {
                    addHistory("✗ " + message.getContent());
                }
                break;
                
            case AUCTION_START:
            case AUCTION_UPDATE:
                if (message.getData() instanceof AuctionUpdate) {
                    updateAuctionDisplay((AuctionUpdate) message.getData());
                }
                addHistory(message.getContent());
                break;
                
            case AUCTION_END:
                if (message.getData() instanceof AuctionUpdate) {
                    AuctionUpdate update = (AuctionUpdate) message.getData();
                    handleAuctionEnd(update);
                }
                addHistory("🏁 " + message.getContent());
                break;
                
            case ERROR:
                addHistory("⚠ ERREUR: " + message.getContent());
                JOptionPane.showMessageDialog(this, message.getContent(), "Erreur", JOptionPane.WARNING_MESSAGE);
                break;
                
            default:
                addHistory(message.getContent());
        }
    }
    
    /**
     * Traite une mise à jour reçue via Multicast
     */
    private void handleMulticastUpdate(AuctionUpdate update) {
        SwingUtilities.invokeLater(() -> {
            updateAuctionDisplay(update);
            addHistory("[MULTICAST] " + update.getMessage());
        });
    }
    
    /**
     * Met à jour l'affichage de l'enchère
     */
    private void updateAuctionDisplay(AuctionUpdate update) {
        switch (update.getUpdateType()) {
            case NEW_AUCTION:
                productLabel.setText(update.getProductName());
                priceLabel.setText("Prix: " + priceFormat.format(update.getCurrentPrice()));
                highestBidderLabel.setText("Meilleur enchérisseur: -");
                bidButton.setEnabled(true);
                bidField.setText(String.valueOf((int)(update.getCurrentPrice() + 10)));
                break;
                
            case NEW_BID:
                priceLabel.setText("Prix: " + priceFormat.format(update.getCurrentPrice()));
                String bidderDisplay = update.getHighestBidderName();
                if (update.getHighestBidder() != null && update.getHighestBidder().equals(clientId)) {
                    bidderDisplay += " (VOUS!)";
                    highestBidderLabel.setForeground(new Color(0, 150, 0));
                } else {
                    highestBidderLabel.setForeground(Color.BLACK);
                }
                highestBidderLabel.setText("Meilleur enchérisseur: " + bidderDisplay);
                bidField.setText(String.valueOf((int)(update.getCurrentPrice() + 10)));
                break;
                
            case AUCTION_CLOSED:
            case AUCTION_CANCELLED:
                handleAuctionEnd(update);
                break;
        }
    }
    
    /**
     * Gère la fin d'une enchère
     */
    private void handleAuctionEnd(AuctionUpdate update) {
        bidButton.setEnabled(false);
        
        if (update.getUpdateType() == AuctionUpdate.UpdateType.AUCTION_CLOSED && update.getWinnerId() != null) {
            if (update.getWinnerId().equals(clientId)) {
                // C'est nous le gagnant!
                JOptionPane.showMessageDialog(this, 
                    "🎉 Félicitations!\nVous avez remporté " + update.getProductName() + 
                    " pour " + priceFormat.format(update.getWinningPrice()),
                    "Enchère remportée!",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            productLabel.setText("VENDU: " + update.getProductName());
            highestBidderLabel.setText("Gagnant: " + update.getWinnerName() + " - " + priceFormat.format(update.getWinningPrice()));
        } else {
            productLabel.setText("Aucun produit en vente");
            priceLabel.setText("Prix actuel: -");
            highestBidderLabel.setText("Meilleur enchérisseur: -");
        }
    }
    
    /**
     * Place une enchère
     */
    private void placeBid() {
        if (!connected || clientId == null) {
            JOptionPane.showMessageDialog(this, "Vous devez être connecté pour enchérir", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String bidText = bidField.getText().trim();
        double amount;
        try {
            amount = Double.parseDouble(bidText);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer un montant valide", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            BidRequest bid = new BidRequest(clientId, clientName, null, amount);
            Message bidMessage = new Message(MessageType.BID_REQUEST, "Enchère: " + amount + "€", bid);
            output.writeObject(bidMessage);
            output.flush();
            output.reset();
            
            addHistory("→ Enchère envoyée: " + priceFormat.format(amount));
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de l'envoi de l'enchère: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Met à jour le statut de connexion dans l'UI
     */
    private void updateConnectionStatus(boolean isConnected) {
        this.connected = isConnected;
        
        if (isConnected) {
            statusLabel.setText("Connecté");
            statusLabel.setForeground(new Color(0, 128, 0));
            connectButton.setText("Se déconnecter");
            nameField.setEnabled(false);
            bidButton.setEnabled(true);
        } else {
            statusLabel.setText("Déconnecté");
            statusLabel.setForeground(Color.RED);
            connectButton.setText("Se connecter");
            nameField.setEnabled(true);
            bidButton.setEnabled(false);
            productLabel.setText("Aucun produit en vente");
            priceLabel.setText("Prix actuel: -");
            highestBidderLabel.setText("Meilleur enchérisseur: -");
        }
    }
    
    /**
     * Ajoute une entrée dans l'historique
     */
    private void addHistory(String text) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
        historyArea.append("[" + timestamp + "] " + text + "\n");
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
    }
    
    public static void main(String[] args) {
        // Utiliser le Look and Feel du système
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Ignorer
        }
        
        SwingUtilities.invokeLater(() -> {
            BuyerClient client = new BuyerClient();
            client.setVisible(true);
        });
    }
}
