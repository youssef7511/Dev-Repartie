package com.auction.admin;

import com.auction.common.constants.NetworkConfig;
import com.auction.common.dto.Product;
import com.auction.common.rmi.IAuctionAdmin;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

/**
 * Client Administrateur avec interface JavaFX
 * Se connecte au serveur via RMI pour gérer les enchères
 */
public class AdminClientApp extends Application {

    private IAuctionAdmin auctionAdmin;
    private boolean connected = false;

    // Composants UI
    private Label statusLabel;
    private Button connectButton;
    private Label currentAuctionLabel;
    private Label currentPriceLabel;
    private Label currentBidderLabel;
    private ListView<String> clientsListView;
    private TableView<Product> historyTable;
    private TextArea logArea;
    private VBox controlsBox;
    private Timeline clientsRefreshTimeline;

    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00 'TND'");

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🛠️ e-Auction - Console Administration");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #263238;");

        // === TOP: Connexion ===
        root.setTop(createConnectionPanel());

        // === CENTER: Dashboard ===
        root.setCenter(createDashboard());

        // === BOTTOM: Log ===
        root.setBottom(createLogPanel());

        Scene scene = new Scene(root, 900, 750);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> Platform.exit());
        primaryStage.show();
    }

    /**
     * Crée le panneau de connexion
     */
    private HBox createConnectionPanel() {
        HBox box = new HBox(20);
        box.setPadding(new Insets(15));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: #37474f; -fx-background-radius: 10;");

        Label title = new Label("🛠️ Console d'Administration e-Auction");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        connectButton = new Button("🔌 Connecter au serveur");
        connectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        connectButton.setPrefWidth(180);
        connectButton.setOnAction(e -> toggleConnection());

        statusLabel = new Label("⚫ Non connecté");
        statusLabel.setTextFill(Color.web("#ef5350"));
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        box.getChildren().addAll(title, spacer, connectButton, statusLabel);
        return box;
    }

    /**
     * Crée le dashboard principal
     */
    private HBox createDashboard() {
        HBox dashboard = new HBox(15);
        dashboard.setPadding(new Insets(15, 0, 15, 0));

        // Colonne gauche: Contrôles
        VBox leftColumn = new VBox(15);
        leftColumn.setPrefWidth(350);
        leftColumn.getChildren().addAll(createAuctionStatusPanel(), createControlsPanel(), createClientsPanel());

        // Colonne droite: Historique
        VBox rightColumn = new VBox(15);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        rightColumn.getChildren().add(createHistoryPanel());

        dashboard.getChildren().addAll(leftColumn, rightColumn);
        return dashboard;
    }

    /**
     * Crée le panneau de statut de l'enchère
     */
    private VBox createAuctionStatusPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #455a64; -fx-background-radius: 10;");

        Label title = new Label("📊 Enchère en Cours");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.WHITE);

        currentAuctionLabel = new Label("Aucune enchère");
        currentAuctionLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        currentAuctionLabel.setTextFill(Color.web("#80cbc4"));

        currentPriceLabel = new Label("Prix: ---.-- TND");
        currentPriceLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        currentPriceLabel.setTextFill(Color.web("#a5d6a7"));

        currentBidderLabel = new Label("Enchérisseur: -");
        currentBidderLabel.setTextFill(Color.web("#b0bec5"));

        Button refreshButton = new Button("🔄 Actualiser");
        refreshButton.setStyle("-fx-background-color: #607d8b; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        refreshButton.setOnAction(e -> refreshAuctionStatus());

        box.getChildren().addAll(title, new Separator(), currentAuctionLabel, currentPriceLabel, currentBidderLabel, refreshButton);
        return box;
    }

    /**
     * Crée le panneau de contrôles
     */
    private VBox createControlsPanel() {
        controlsBox = new VBox(10);
        controlsBox.setPadding(new Insets(15));
        controlsBox.setStyle("-fx-background-color: #455a64; -fx-background-radius: 10;");
        controlsBox.setDisable(true);

        Label title = new Label("🎮 Contrôles");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.WHITE);

        // Bouton Nouvelle Enchère
        Button startButton = new Button("🆕 Nouvelle Enchère");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;");
        startButton.setPrefWidth(200);
        startButton.setOnAction(e -> showNewAuctionDialog());

        // Bouton Clôturer
        Button stopButton = new Button("🏁 Clôturer l'Enchère");
        stopButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;");
        stopButton.setPrefWidth(200);
        stopButton.setOnAction(e -> stopAuction());

        // Bouton Annuler
        Button cancelButton = new Button("❌ Annuler l'Enchère");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;");
        cancelButton.setPrefWidth(200);
        cancelButton.setOnAction(e -> cancelAuction());

        controlsBox.getChildren().addAll(title, new Separator(), startButton, stopButton, cancelButton);
        return controlsBox;
    }

    /**
     * Crée le panneau des clients
     */
    private VBox createClientsPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #455a64; -fx-background-radius: 10;");
        VBox.setVgrow(box, Priority.ALWAYS);

        Label title = new Label("👥 Clients Connectés");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.WHITE);

        clientsListView = new ListView<>();
        clientsListView.setStyle("-fx-background-color: #546e7a; -fx-control-inner-background: #546e7a;");
        clientsListView.setPrefHeight(150);
        VBox.setVgrow(clientsListView, Priority.ALWAYS);

        HBox buttonsBox = new HBox(10);
        Button refreshClientsBtn = new Button("🔄 Actualiser");
        refreshClientsBtn.setStyle("-fx-background-color: #607d8b; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        refreshClientsBtn.setOnAction(e -> refreshClientsList());

        Button banButton = new Button("🚫 Bannir");
        banButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        banButton.setOnAction(e -> banSelectedClient());

        buttonsBox.getChildren().addAll(refreshClientsBtn, banButton);

        box.getChildren().addAll(title, new Separator(), clientsListView, buttonsBox);
        return box;
    }

    /**
     * Crée le panneau d'historique
     */
    private VBox createHistoryPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #455a64; -fx-background-radius: 10;");
        VBox.setVgrow(box, Priority.ALWAYS);

        Label title = new Label("📜 Historique des Ventes");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.WHITE);

        historyTable = new TableView<>();
        historyTable.setStyle("-fx-background-color: #546e7a;");
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        TableColumn<Product, String> nameCol = new TableColumn<>("Produit");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<Product, Double> priceCol = new TableColumn<>("Prix Final");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        priceCol.setPrefWidth(120);
        priceCol.setCellFactory(col -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : priceFormat.format(item));
            }
        });

        TableColumn<Product, String> winnerCol = new TableColumn<>("Gagnant");
        winnerCol.setCellValueFactory(new PropertyValueFactory<>("highestBidderName"));
        winnerCol.setPrefWidth(150);

        historyTable.getColumns().addAll(nameCol, priceCol, winnerCol);

        Button refreshHistoryBtn = new Button("🔄 Actualiser l'historique");
        refreshHistoryBtn.setStyle("-fx-background-color: #607d8b; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        refreshHistoryBtn.setOnAction(e -> refreshHistory());

        box.getChildren().addAll(title, new Separator(), historyTable, refreshHistoryBtn);
        return box;
    }

    /**
     * Crée le panneau de log
     */
    private VBox createLogPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #37474f; -fx-background-radius: 10;");
        box.setPrefHeight(150);

        Label title = new Label("📝 Journal des Opérations");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.WHITE);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setFont(Font.font("Consolas", 11));
        logArea.setStyle("-fx-control-inner-background: #263238; -fx-text-fill: #b0bec5;");
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        box.getChildren().addAll(title, logArea);
        return box;
    }

    /**
     * Connexion/Déconnexion RMI
     */
    private void toggleConnection() {
        if (!connected) {
            connect();
        } else {
            disconnect();
        }
    }

    /**
     * Se connecte au serveur RMI
     */
    private void connect() {
        try {
            addLog("🔄 Connexion au serveur RMI...");
            
            Registry registry = LocateRegistry.getRegistry(
                NetworkConfig.SERVER_HOST,
                NetworkConfig.RMI_PORT
            );

            auctionAdmin = (IAuctionAdmin) registry.lookup(NetworkConfig.RMI_SERVICE_NAME);

            if (auctionAdmin.ping()) {
                connected = true;
                updateConnectionStatus(true);
                addLog("✅ Connecté au serveur d'enchères!");
                
                // Charger les données initiales
                refreshAuctionStatus();
                refreshClientsList();
                refreshHistory();
                startClientsAutoRefresh();
            }

        } catch (Exception e) {
            addLog("❌ Erreur de connexion: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur de connexion",
                "Impossible de se connecter au serveur RMI:\n" + e.getMessage() +
                "\n\nVérifiez que le serveur est démarré.");
        }
    }

    /**
     * Se déconnecte
     */
    private void disconnect() {
        auctionAdmin = null;
        connected = false;
        stopClientsAutoRefresh();
        updateConnectionStatus(false);
        addLog("🔌 Déconnecté du serveur");
    }

    /**
     * Met à jour le statut de connexion
     */
    private void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            statusLabel.setText("🟢 Connecté");
            statusLabel.setTextFill(Color.web("#a5d6a7"));
            connectButton.setText("🔌 Déconnecter");
            connectButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            controlsBox.setDisable(false);
        } else {
            statusLabel.setText("🔴 Non connecté");
            statusLabel.setTextFill(Color.web("#ef5350"));
            connectButton.setText("🔌 Connecter au serveur");
            connectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            controlsBox.setDisable(true);
            currentAuctionLabel.setText("Aucune enchère");
            currentPriceLabel.setText("Prix: ---.-- TND");
            currentBidderLabel.setText("Enchérisseur: -");
            clientsListView.getItems().clear();
        }
    }

    /**
     * Actualise le statut de l'enchère
     */
    private void refreshAuctionStatus() {
        if (!connected) return;
        
        try {
            Product current = auctionAdmin.getAuctionStatus();
            
            if (current != null && current.isActive()) {
                currentAuctionLabel.setText("🎁 " + current.getName());
                currentPriceLabel.setText("Prix: " + priceFormat.format(current.getCurrentPrice()));
                String bidder = current.getHighestBidderName() != null ? current.getHighestBidderName() : "Aucun";
                currentBidderLabel.setText("Enchérisseur: " + bidder);
            } else {
                currentAuctionLabel.setText("Aucune enchère en cours");
                currentPriceLabel.setText("Prix: ---.-- TND");
                currentBidderLabel.setText("Enchérisseur: -");
            }
            
        } catch (Exception e) {
            addLog("❌ Erreur: " + e.getMessage());
        }
    }

    /**
     * Actualise la liste des clients
     */
    private void refreshClientsList() {
        if (!connected) return;
        
        try {
            List<String> clients = auctionAdmin.getConnectedClients();
            clientsListView.setItems(FXCollections.observableArrayList(clients));
            addLog("👥 " + clients.size() + " client(s) connecté(s)");
        } catch (Exception e) {
            addLog("❌ Erreur: " + e.getMessage());
        }
    }

    /**
     * Lance un rafraîchissement périodique de la liste des clients connectés.
     */
    private void startClientsAutoRefresh() {
        if (clientsRefreshTimeline != null) {
            clientsRefreshTimeline.stop();
        }
        clientsRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> refreshClientsList()));
        clientsRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        clientsRefreshTimeline.play();
    }

    /**
     * Arrête le rafraîchissement périodique des clients.
     */
    private void stopClientsAutoRefresh() {
        if (clientsRefreshTimeline != null) {
            clientsRefreshTimeline.stop();
            clientsRefreshTimeline = null;
        }
    }

    /**
     * Actualise l'historique
     */
    private void refreshHistory() {
        if (!connected) return;
        
        try {
            List<Product> history = auctionAdmin.getSalesHistory();
            historyTable.setItems(FXCollections.observableArrayList(history));
            addLog("📜 " + history.size() + " vente(s) dans l'historique");
        } catch (Exception e) {
            addLog("❌ Erreur: " + e.getMessage());
        }
    }

    /**
     * Affiche le dialogue de nouvelle enchère
     */
    private void showNewAuctionDialog() {
        if (!connected) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle Enchère");
        dialog.setHeaderText("Créer une nouvelle vente aux enchères");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Nom du produit");
        
        TextField descField = new TextField();
        descField.setPromptText("Description");
        
        TextField priceField = new TextField();
        priceField.setPromptText("Prix de départ (TND)");

        grid.add(new Label("Produit:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Prix de départ:"), 0, 2);
        grid.add(priceField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            String priceText = priceField.getText().trim();

            if (name.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Le nom du produit est requis");
                return;
            }

            try {
                double price = Double.parseDouble(priceText);
                if (price <= 0) throw new NumberFormatException();

                boolean success = auctionAdmin.startAuction(name, desc, price);
                
                if (success) {
                    addLog("✅ Enchère démarrée: " + name + " à " + priceFormat.format(price));
                    refreshAuctionStatus();
                } else {
                    addLog("⚠️ Impossible de démarrer l'enchère (une enchère est déjà en cours?)");
                    showAlert(Alert.AlertType.WARNING, "Échec", "Une enchère est peut-être déjà en cours.");
                }

            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Prix invalide", "Veuillez entrer un prix valide");
            } catch (Exception e) {
                addLog("❌ Erreur: " + e.getMessage());
            }
        }
    }

    /**
     * Clôture l'enchère
     */
    private void stopAuction() {
        if (!connected) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Clôturer l'enchère");
        confirm.setContentText("Voulez-vous vraiment clôturer l'enchère en cours?");

        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Product sold = auctionAdmin.stopAuction();
                
                if (sold != null) {
                    String winner = sold.getHighestBidderName() != null ? sold.getHighestBidderName() : "Aucun";
                    addLog("🏁 Enchère clôturée: " + sold.getName() + " - Gagnant: " + winner + " - Prix: " + priceFormat.format(sold.getCurrentPrice()));
                    showAlert(Alert.AlertType.INFORMATION, "Enchère terminée",
                        "Produit: " + sold.getName() + "\n" +
                        "Gagnant: " + winner + "\n" +
                        "Prix final: " + priceFormat.format(sold.getCurrentPrice()));
                } else {
                    addLog("⚠️ Aucune enchère à clôturer");
                }
                
                refreshAuctionStatus();
                refreshHistory();

            } catch (Exception e) {
                addLog("❌ Erreur: " + e.getMessage());
            }
        }
    }

    /**
     * Annule l'enchère
     */
    private void cancelAuction() {
        if (!connected) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Annuler l'enchère");
        confirm.setContentText("Voulez-vous vraiment ANNULER l'enchère en cours?\nAucun gagnant ne sera déclaré.");

        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = auctionAdmin.cancelAuction();
                
                if (success) {
                    addLog("❌ Enchère annulée");
                } else {
                    addLog("⚠️ Aucune enchère à annuler");
                }
                
                refreshAuctionStatus();

            } catch (Exception e) {
                addLog("❌ Erreur: " + e.getMessage());
            }
        }
    }

    /**
     * Bannit le client sélectionné
     */
    private void banSelectedClient() {
        if (!connected) return;

        String selected = clientsListView.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un client à bannir");
            return;
        }

        // Extraire l'ID du format "Nom (ID)"
        String clientId = selected.substring(selected.lastIndexOf("(") + 1, selected.lastIndexOf(")"));

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Bannir un client");
        confirm.setContentText("Voulez-vous vraiment bannir ce client?\n" + selected);

        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = auctionAdmin.banClient(clientId);
                
                if (success) {
                    addLog("🚫 Client banni: " + selected);
                    refreshClientsList();
                } else {
                    addLog("⚠️ Impossible de bannir ce client");
                }

            } catch (Exception e) {
                addLog("❌ Erreur: " + e.getMessage());
            }
        }
    }

    /**
     * Ajoute un message au log
     */
    private void addLog(String message) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
        Platform.runLater(() -> {
            logArea.appendText("[" + timestamp + "] " + message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
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
