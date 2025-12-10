package com.auction.client;

import com.auction.common.constants.NetworkConfig;
import com.auction.common.dto.*;
import com.auction.common.dto.Message.MessageType;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;

/**
 * Client Acheteur avec interface JavaFX
 * Se connecte au serveur via TCP et écoute le Multicast
 */
public class BuyerClientApp extends Application {

    // Connexion TCP
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String clientId;
    private String clientName;
    private boolean connected = false;

    // Listener Multicast
    private MulticastListener multicastListener;

    // Composants UI
    private TextField nameField;
    private Button connectButton;
    private Label statusLabel;
    private Label productLabel;
    private Label descriptionLabel;
    private Label priceLabel;
    private Label highestBidderLabel;
    private TextField bidField;
    private Button bidButton;
    private TextArea historyArea;
    private VBox auctionInfoBox;

    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00 €");

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🏛️ e-Auction - Client Acheteur");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // === TOP: Connexion ===
        root.setTop(createConnectionPanel());

        // === CENTER: Enchère ===
        root.setCenter(createAuctionPanel());

        // === BOTTOM: Historique ===
        root.setBottom(createHistoryPanel());

        Scene scene = new Scene(root, 700, 650);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            disconnect();
            Platform.exit();
        });
        primaryStage.show();
    }

    /**
     * Crée le panneau de connexion
     */
    private VBox createConnectionPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Label title = new Label("🔌 Connexion au Serveur");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);

        nameField = new TextField();
        nameField.setPromptText("Entrez votre nom");
        nameField.setPrefWidth(200);
        nameField.setStyle("-fx-background-radius: 5;");

        connectButton = new Button("Se connecter");
        connectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        connectButton.setPrefWidth(130);
        connectButton.setOnAction(e -> toggleConnection());

        statusLabel = new Label("⚫ Déconnecté");
        statusLabel.setTextFill(Color.web("#d32f2f"));
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        row.getChildren().addAll(new Label("Nom:"), nameField, connectButton, new Separator(), statusLabel);
        box.getChildren().addAll(title, row);

        return box;
    }

    /**
     * Crée le panneau d'enchère principal
     */
    private VBox createAuctionPanel() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        VBox.setMargin(box, new Insets(15, 0, 15, 0));

        Label title = new Label("🔨 Enchère en Cours");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));

        // Info sur le produit
        auctionInfoBox = new VBox(10);
        auctionInfoBox.setAlignment(Pos.CENTER);
        auctionInfoBox.setPadding(new Insets(25));
        auctionInfoBox.setStyle("-fx-background-color: linear-gradient(to bottom, #e8f5e9, #c8e6c9); -fx-background-radius: 10;");

        productLabel = new Label("En attente d'une enchère...");
        productLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        productLabel.setTextFill(Color.web("#37474f"));

        descriptionLabel = new Label("");
        descriptionLabel.setTextFill(Color.web("#607d8b"));
        descriptionLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));

        priceLabel = new Label("---.-- €");
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 42));
        priceLabel.setTextFill(Color.web("#2e7d32"));

        highestBidderLabel = new Label("Meilleur enchérisseur: -");
        highestBidderLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        highestBidderLabel.setTextFill(Color.web("#455a64"));

        auctionInfoBox.getChildren().addAll(productLabel, descriptionLabel, priceLabel, highestBidderLabel);

        // Zone d'enchère
        HBox bidBox = new HBox(15);
        bidBox.setAlignment(Pos.CENTER);
        bidBox.setPadding(new Insets(20, 15, 10, 15));
        bidBox.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 8;");

        Label bidLabel = new Label("💰 Votre enchère:");
        bidLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        bidField = new TextField();
        bidField.setPromptText("Montant (€)");
        bidField.setPrefWidth(150);
        bidField.setStyle("-fx-background-radius: 5; -fx-font-size: 14px;");
        bidField.setOnAction(e -> placeBid());

        bidButton = new Button("Enchérir!");
        bidButton.setStyle("-fx-background-color: linear-gradient(to bottom, #ff9800, #f57c00); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        bidButton.setPrefWidth(130);
        bidButton.setPrefHeight(35);
        bidButton.setDisable(true);
        bidButton.setOnAction(e -> placeBid());

        bidBox.getChildren().addAll(bidLabel, bidField, bidButton);

        box.getChildren().addAll(title, auctionInfoBox, bidBox);
        return box;
    }

    /**
     * Crée le panneau d'historique
     */
    private VBox createHistoryPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        box.setPrefHeight(200);

        Label title = new Label("📜 Historique des Événements");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setFont(Font.font("Consolas", 12));
        historyArea.setStyle("-fx-control-inner-background: #fafafa; -fx-background-radius: 5;");
        historyArea.setWrapText(true);
        VBox.setVgrow(historyArea, Priority.ALWAYS);

        box.getChildren().addAll(title, historyArea);
        return box;
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
     * Se connecte au serveur TCP
     */
    private void connect() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez entrer votre nom");
            return;
        }

        try {
            addHistory("🔄 Connexion au serveur " + NetworkConfig.SERVER_HOST + ":" + NetworkConfig.TCP_PORT + "...");
            
            socket = new Socket(NetworkConfig.SERVER_HOST, NetworkConfig.TCP_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            // Envoyer la demande de connexion
            Message loginRequest = new Message(MessageType.LOGIN_REQUEST, name);
            output.writeObject(loginRequest);
            output.flush();

            // Démarrer le thread de réception TCP
            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();

            // Démarrer le listener Multicast
            multicastListener = new MulticastListener(this::handleMulticastUpdate);
            multicastListener.start();

            this.clientName = name;
            updateConnectionStatus(true);

        } catch (IOException e) {
            addHistory("❌ Erreur de connexion: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur de connexion", 
                "Impossible de se connecter au serveur:\n" + e.getMessage() + 
                "\n\nVérifiez que le serveur est démarré.");
        }
    }

    /**
     * Se déconnecte du serveur
     */
    private void disconnect() {
        if (connected) {
            try {
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
            multicastListener = null;
        }

        // Fermer les connexions
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignorer
        }

        Platform.runLater(() -> {
            updateConnectionStatus(false);
            addHistory("🔌 Déconnecté du serveur");
        });
    }

    /**
     * Thread de réception des messages TCP
     */
    private void receiveMessages() {
        try {
            while (connected && socket != null && !socket.isClosed()) {
                Message message = (Message) input.readObject();
                Platform.runLater(() -> handleMessage(message));
            }
        } catch (EOFException e) {
            Platform.runLater(() -> {
                updateConnectionStatus(false);
                addHistory("⚠️ Connexion fermée par le serveur");
            });
        } catch (IOException | ClassNotFoundException e) {
            if (connected) {
                Platform.runLater(() -> {
                    updateConnectionStatus(false);
                    addHistory("❌ Erreur de réception: " + e.getMessage());
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
                addHistory("✅ Connecté avec succès! Votre ID: " + clientId);
                break;

            case BID_RESPONSE:
                boolean success = (Boolean) message.getData();
                if (success) {
                    addHistory("✓ " + message.getContent());
                } else {
                    addHistory("✗ " + message.getContent());
                    showAlert(Alert.AlertType.WARNING, "Enchère refusée", message.getContent());
                }
                break;

            case AUCTION_START:
            case AUCTION_UPDATE:
                if (message.getData() instanceof AuctionUpdate) {
                    updateAuctionDisplay((AuctionUpdate) message.getData());
                }
                addHistory("📢 " + message.getContent());
                break;

            case AUCTION_END:
                if (message.getData() instanceof AuctionUpdate) {
                    handleAuctionEnd((AuctionUpdate) message.getData());
                }
                addHistory("🏁 " + message.getContent());
                break;

            case ERROR:
                addHistory("⚠️ ERREUR: " + message.getContent());
                showAlert(Alert.AlertType.ERROR, "Erreur", message.getContent());
                break;

            default:
                addHistory("📨 " + message.getContent());
        }
    }

    /**
     * Traite une mise à jour reçue via Multicast
     */
    private void handleMulticastUpdate(AuctionUpdate update) {
        Platform.runLater(() -> {
            updateAuctionDisplay(update);
            addHistory("📡 [MULTICAST] " + update.getMessage());
        });
    }

    /**
     * Met à jour l'affichage de l'enchère
     */
    private void updateAuctionDisplay(AuctionUpdate update) {
        switch (update.getUpdateType()) {
            case NEW_AUCTION:
                productLabel.setText("🎁 " + update.getProductName());
                descriptionLabel.setText(update.getProductDescription() != null ? update.getProductDescription() : "");
                priceLabel.setText(priceFormat.format(update.getCurrentPrice()));
                highestBidderLabel.setText("Meilleur enchérisseur: Aucun");
                highestBidderLabel.setTextFill(Color.web("#455a64"));
                bidButton.setDisable(false);
                bidField.setText(String.valueOf((int) (update.getCurrentPrice() + 10)));
                auctionInfoBox.setStyle("-fx-background-color: linear-gradient(to bottom, #e8f5e9, #c8e6c9); -fx-background-radius: 10;");
                break;

            case NEW_BID:
                priceLabel.setText(priceFormat.format(update.getCurrentPrice()));
                String bidder = update.getHighestBidderName();
                
                if (update.getHighestBidder() != null && update.getHighestBidder().equals(clientId)) {
                    // C'est nous qui menons!
                    bidder += " (VOUS! 🎉)";
                    highestBidderLabel.setTextFill(Color.web("#2e7d32"));
                    auctionInfoBox.setStyle("-fx-background-color: linear-gradient(to bottom, #c8e6c9, #a5d6a7); -fx-background-radius: 10;");
                } else {
                    // Quelqu'un d'autre mène
                    highestBidderLabel.setTextFill(Color.web("#c62828"));
                    auctionInfoBox.setStyle("-fx-background-color: linear-gradient(to bottom, #ffebee, #ffcdd2); -fx-background-radius: 10;");
                }
                
                highestBidderLabel.setText("Meilleur enchérisseur: " + bidder);
                bidField.setText(String.valueOf((int) (update.getCurrentPrice() + 10)));
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
        bidButton.setDisable(true);

        if (update.getUpdateType() == AuctionUpdate.UpdateType.AUCTION_CLOSED && update.getWinnerId() != null) {
            if (update.getWinnerId().equals(clientId)) {
                // Nous avons gagné!
                auctionInfoBox.setStyle("-fx-background-color: linear-gradient(to bottom, #fff9c4, #fff59d); -fx-background-radius: 10;");
                showAlert(Alert.AlertType.INFORMATION, "🎉 Félicitations!", 
                    "Vous avez remporté l'enchère!\n\n" +
                    "Produit: " + update.getProductName() + "\n" +
                    "Prix: " + priceFormat.format(update.getWinningPrice()));
            } else {
                auctionInfoBox.setStyle("-fx-background-color: linear-gradient(to bottom, #eceff1, #cfd8dc); -fx-background-radius: 10;");
            }
            
            productLabel.setText("🏆 VENDU: " + update.getProductName());
            highestBidderLabel.setText("Gagnant: " + update.getWinnerName() + " - " + priceFormat.format(update.getWinningPrice()));
            highestBidderLabel.setTextFill(Color.web("#1565c0"));
        } else {
            // Enchère annulée ou sans gagnant
            productLabel.setText("En attente d'une enchère...");
            priceLabel.setText("---.-- €");
            highestBidderLabel.setText("Meilleur enchérisseur: -");
            highestBidderLabel.setTextFill(Color.web("#455a64"));
            auctionInfoBox.setStyle("-fx-background-color: linear-gradient(to bottom, #e8f5e9, #c8e6c9); -fx-background-radius: 10;");
        }
    }

    /**
     * Place une enchère
     */
    private void placeBid() {
        if (!connected || clientId == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Vous devez être connecté pour enchérir");
            return;
        }

        String bidText = bidField.getText().trim();
        double amount;
        try {
            amount = Double.parseDouble(bidText);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Montant invalide", "Veuillez entrer un montant valide (nombre positif)");
            return;
        }

        try {
            BidRequest bid = new BidRequest(clientId, clientName, null, amount);
            Message bidMessage = new Message(MessageType.BID_REQUEST, "Enchère: " + amount + "€", bid);
            output.writeObject(bidMessage);
            output.flush();
            output.reset();

            addHistory("➡️ Enchère envoyée: " + priceFormat.format(amount));

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'envoi de l'enchère:\n" + e.getMessage());
        }
    }

    /**
     * Met à jour le statut de connexion dans l'UI
     */
    private void updateConnectionStatus(boolean isConnected) {
        this.connected = isConnected;

        if (isConnected) {
            statusLabel.setText("🟢 Connecté");
            statusLabel.setTextFill(Color.web("#2e7d32"));
            connectButton.setText("Se déconnecter");
            connectButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            nameField.setDisable(true);
            bidButton.setDisable(false);
        } else {
            statusLabel.setText("🔴 Déconnecté");
            statusLabel.setTextFill(Color.web("#d32f2f"));
            connectButton.setText("Se connecter");
            connectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            nameField.setDisable(false);
            bidButton.setDisable(true);
            productLabel.setText("En attente d'une enchère...");
            priceLabel.setText("---.-- €");
            highestBidderLabel.setText("Meilleur enchérisseur: -");
            auctionInfoBox.setStyle("-fx-background-color: linear-gradient(to bottom, #e8f5e9, #c8e6c9); -fx-background-radius: 10;");
        }
    }

    /**
     * Ajoute une entrée dans l'historique
     */
    private void addHistory(String text) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
        historyArea.appendText("[" + timestamp + "] " + text + "\n");
        historyArea.setScrollTop(Double.MAX_VALUE);
    }

    /**
     * Affiche une alerte
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
