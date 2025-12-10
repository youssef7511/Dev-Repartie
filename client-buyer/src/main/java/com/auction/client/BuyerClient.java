package com.auction.client;

import com.auction.common.constants.NetworkConfig;
import com.auction.common.dto.AuctionUpdate;
import com.auction.common.dto.BidRequest;
import com.auction.common.dto.Message;
import com.auction.common.dto.Message.MessageType;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Scanner;

/**
 * Client Acheteur en ligne de commande (legacy console)
 * Connexion TCP + écoute Multicast, sans interface graphique.
 */
public class BuyerClient {

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String clientId;
    private String clientName;
    private boolean connected;

    private MulticastListener multicastListener;
    private final Scanner scanner = new Scanner(System.in);
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00 'TND'");

    public static void main(String[] args) {
        new BuyerClient().start();
    }

    private void start() {
        System.out.println("=== Client Acheteur (console) ===");
        System.out.print("Votre nom: ");
        clientName = scanner.nextLine().trim();
        if (clientName.isEmpty()) {
            System.out.println("Nom invalide. Arrêt.");
            return;
        }

        if (!connect()) {
            return;
        }

        mainLoop();
        disconnect();
    }

    private boolean connect() {
        try {
            socket = new Socket(NetworkConfig.SERVER_HOST, NetworkConfig.TCP_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            connected = true;

            Message loginRequest = new Message(MessageType.LOGIN_REQUEST, clientName);
            output.writeObject(loginRequest);
            output.flush();

            new Thread(this::receiveMessages, "buyer-console-recv").start();

            multicastListener = new MulticastListener(this::handleMulticastUpdate);
            multicastListener.start();

            System.out.println("Connexion établie. Utilisez un montant pour enchérir, ou /quit pour sortir.");
            return true;
        } catch (IOException e) {
            System.out.println("Impossible de se connecter au serveur: " + e.getMessage());
            return false;
        }
    }

    private void mainLoop() {
        while (connected) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("/quit") || line.equalsIgnoreCase("/exit")) {
                break;
            }
            if (line.isEmpty()) {
                continue;
            }
            double amount;
            try {
                amount = Double.parseDouble(line);
                if (amount <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                System.out.println("Montant invalide. Entrez un nombre positif ou /quit.");
                continue;
            }
            sendBid(amount);
        }
    }

    private void sendBid(double amount) {
        try {
            BidRequest bid = new BidRequest(clientId, clientName, null, amount);
            Message bidMessage = new Message(MessageType.BID_REQUEST, "Enchère: " + amount + " TND", bid);
            output.writeObject(bidMessage);
            output.flush();
            output.reset();
            System.out.println("Enchère envoyée: " + priceFormat.format(amount));
        } catch (IOException e) {
            System.out.println("Erreur lors de l'envoi de l'enchère: " + e.getMessage());
        }
    }

    private void receiveMessages() {
        try {
            while (connected && !socket.isClosed()) {
                Message message = (Message) input.readObject();
                handleMessage(message);
            }
        } catch (EOFException e) {
            System.out.println("Connexion fermée par le serveur.");
        } catch (IOException | ClassNotFoundException e) {
            if (connected) {
                System.out.println("Erreur de réception: " + e.getMessage());
            }
        } finally {
            connected = false;
        }
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case LOGIN_RESPONSE:
                clientId = (String) message.getData();
                System.out.println("Connecté. ID: " + clientId);
                break;
            case BID_RESPONSE:
                boolean success = (Boolean) message.getData();
                System.out.println((success ? "[OK] " : "[REFUS] ") + message.getContent());
                break;
            case AUCTION_START:
            case AUCTION_UPDATE:
                if (message.getData() instanceof AuctionUpdate) {
                    AuctionUpdate update = (AuctionUpdate) message.getData();
                    printAuctionUpdate(update);
                }
                break;
            case AUCTION_END:
                if (message.getData() instanceof AuctionUpdate) {
                    AuctionUpdate update = (AuctionUpdate) message.getData();
                    System.out.println("Fin d'enchère: " + update.getMessage());
                } else {
                    System.out.println("Fin d'enchère: " + message.getContent());
                }
                break;
            case ERROR:
                System.out.println("[ERREUR] " + message.getContent());
                break;
            default:
                System.out.println(message.getContent());
        }
    }

    private void handleMulticastUpdate(AuctionUpdate update) {
        printAuctionUpdate(update);
    }

    private void printAuctionUpdate(AuctionUpdate update) {
        switch (update.getUpdateType()) {
            case NEW_AUCTION:
                System.out.println("[NOUVELLE ENCHÈRE] " + update.getProductName() +
                        " - départ " + priceFormat.format(update.getCurrentPrice()));
                break;
            case NEW_BID:
                System.out.println("[ENCHÈRE] " + update.getHighestBidderName() +
                        " -> " + priceFormat.format(update.getCurrentPrice()));
                break;
            case AUCTION_CLOSED:
                System.out.println("[CLOTURE] " + update.getProductName() +
                        " vendu à " + update.getWinnerName() +
                        " pour " + priceFormat.format(update.getWinningPrice()));
                break;
            case AUCTION_CANCELLED:
                System.out.println("[ANNULATION] " + update.getProductName() + " annulée.");
                break;
            default:
                System.out.println(update.getMessage());
        }
    }

    private void disconnect() {
        if (connected) {
            try {
                Message disconnectMsg = new Message(MessageType.DISCONNECT, "");
                output.writeObject(disconnectMsg);
                output.flush();
            } catch (IOException ignored) {
            }
        }

        connected = false;

        if (multicastListener != null) {
            multicastListener.stop();
        }

        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {
        }

        System.out.println("Déconnecté.");
    }
}
