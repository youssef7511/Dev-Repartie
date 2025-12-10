# 🏛️ La Salle des Ventes Distribuée (e-Auction)

> **Module :** Développement d'Applications Réparties / Systèmes Distribués  
> **Type :** Examen Pratique / Projet de Fin de Semestre

---

## 📋 Table des Matières

1. [Description du Projet](#1-description-du-projet)
2. [Architecture Technique](#2-architecture-technique)
3. [Structure du Projet](#3-structure-du-projet)
4. [Fonctionnalités Requises](#4-fonctionnalités-requises)
5. [Instructions d'Installation](#5-instructions-dinstallation)
6. [Scénario d'Exécution](#6-scénario-dexécution)
7. [Critères d'Évaluation](#7-critères-dévaluation)
8. [Références TP](#8-références-tp)

---

## 1. Description du Projet

L'objectif de ce projet est de développer un **système d'enchères électroniques en temps réel**. Le système permet à plusieurs acheteurs de se connecter simultanément, de visualiser les objets en vente et de proposer des prix (enchérir).

### 🎯 Objectifs Pédagogiques

Ce projet utilise une **architecture hybride** combinant les trois paradigmes de communication :

| Technologie | Usage | Référence TP |
|-------------|-------|--------------|
| **TCP (Sockets)** | Transactions fiables (connexion, authentification, enchères) | TP2 |
| **UDP (Multicast)** | Diffusion temps-réel de l'état de la vente | TP4, TP5 |
| **Java RMI** | Administration distante du serveur | TP6 |

---

## 2. Architecture Technique

```
┌─────────────────────────────────────────────────────────────────┐
│                    ARCHITECTURE e-AUCTION                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────────┐         TCP/Socket          ┌─────────────┐  │
│   │   Client     │◄───────────────────────────►│             │  │
│   │  Acheteur A  │                             │             │  │
│   └──────────────┘                             │             │  │
│          ▲                                     │   SERVEUR   │  │
│          │ Multicast (225.1.1.1:6000)          │  D'ENCHÈRES │  │
│          ▼                                     │  (Port 5000)│  │
│   ┌──────────────┐         TCP/Socket          │             │  │
│   │   Client     │◄───────────────────────────►│             │  │
│   │  Acheteur B  │                             │             │  │
│   └──────────────┘                             └──────┬──────┘  │
│                                                       │         │
│                                                       │ RMI     │
│                                                       │ (1099)  │
│                                                       ▼         │
│                                                ┌─────────────┐  │
│                                                │    Admin    │  │
│                                                │   (RMI)     │  │
│                                                └─────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Composants

| Composant | Description | Technologies |
|-----------|-------------|--------------|
| **Server** | Serveur central d'enchères | TCP + Multicast + RMI |
| **Client Buyer** | Interface graphique Swing pour enchérir | TCP + Multicast Listener |
| **Client Admin** | Console d'administration | RMI |
| **Common** | Classes partagées (DTOs, interfaces) | Java Serialization |

---

## 3. Structure du Projet

```
e-auction/
├── pom.xml                          # POM parent Maven
├── README.md                        # Ce fichier
│
├── common/                          # Module partagé
│   ├── pom.xml
│   └── src/main/java/com/auction/common/
│       ├── constants/
│       │   └── NetworkConfig.java   # Configuration réseau
│       ├── dto/
│       │   ├── Message.java         # Messages TCP
│       │   ├── BidRequest.java      # Requête d'enchère
│       │   ├── AuctionUpdate.java   # Mise à jour Multicast
│       │   └── Product.java         # Produit
│       └── rmi/
│           └── IAuctionAdmin.java   # Interface RMI
│
├── server/                          # Module serveur
│   ├── pom.xml
│   └── src/main/java/com/auction/server/
│       ├── AuctionServer.java       # Point d'entrée
│       ├── AuctionManager.java      # Logique métier
│       ├── ClientHandler.java       # Thread par client
│       ├── MulticastBroadcaster.java# Diffusion Multicast
│       └── rmi/
│           └── AuctionAdminImpl.java# Implémentation RMI
│
├── client-buyer/                    # Module client acheteur
│   ├── pom.xml
│   └── src/main/java/com/auction/client/
│       ├── BuyerClient.java         # Interface Swing
│       └── MulticastListener.java   # Écoute Multicast
│
└── client-admin/                    # Module client admin
    ├── pom.xml
    └── src/main/java/com/auction/admin/
        └── AdminClient.java         # Console RMI
```

---

## 4. Fonctionnalités Requises

### Serveur d'Enchères

- ✅ Gestion multi-clients avec ThreadPool
- ✅ Synchronisation des enchères (protection concurrentielle)
- ✅ Diffusion Multicast des mises à jour
- ✅ Service RMI pour l'administration
- ✅ Robustesse (gestion des déconnexions)

### Client Acheteur

- ✅ Interface graphique Swing
- ✅ Connexion TCP avec authentification
- ✅ Envoi d'enchères
- ✅ Réception temps-réel via Multicast
- ✅ Affichage de l'historique

### Client Administrateur

- ✅ Connexion RMI
- ✅ Démarrer/Clôturer une enchère
- ✅ Voir le statut en cours
- ✅ Lister/Bannir des clients
- ✅ Consulter l'historique des ventes

---

## 5. Instructions d'Installation

### Prérequis

- **Java JDK 11** ou supérieur
- **Maven 3.6** ou supérieur

### Compilation

```bash
# Depuis le répertoire e-auction/
mvn clean install
```

### Exécution

**Étape 1 - Démarrer le Serveur :**
```bash
cd server
mvn exec:java
```

**Étape 2 - Démarrer le Client Admin (nouveau terminal) :**
```bash
cd client-admin
mvn exec:java
```

**Étape 3 - Démarrer les Clients Acheteurs (plusieurs terminaux) :**
```bash
cd client-buyer
mvn exec:java
```

---

## 6. Scénario d'Exécution

```
┌────────────────────────────────────────────────────────────────┐
│                      WORKFLOW D'ENCHÈRES                        │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. LANCEMENT                                                   │
│     └─► Démarrer le Serveur                                     │
│                                                                 │
│  2. CONNEXION                                                   │
│     └─► Client A & B se connectent (TCP)                        │
│     └─► Rejoignent automatiquement le groupe Multicast          │
│                                                                 │
│  3. MISE EN VENTE (Admin via RMI)                               │
│     └─► Menu 1: Démarrer "PC Portable", 500€                    │
│                                                                 │
│  4. DIFFUSION                                                   │
│     └─► Multicast: "PC Portable - Prix: 500€"                   │
│     └─► Tous les clients voient l'enchère                       │
│                                                                 │
│  5. ENCHÈRE                                                     │
│     └─► Client A clique "Enchérir" avec 550€                    │
│     └─► Message TCP envoyé au serveur                           │
│                                                                 │
│  6. VALIDATION (synchronized)                                   │
│     └─► Serveur vérifie: 550€ > 500€ ✓                          │
│     └─► Met à jour le prix courant                              │
│     └─► Diffuse via Multicast                                   │
│                                                                 │
│  7. MISE À JOUR                                                 │
│     └─► Client B reçoit via Multicast                           │
│     └─► Son interface affiche: "550€ par ClientA"               │
│                                                                 │
│  8. CLÔTURE (Admin via RMI)                                     │
│     └─► Menu 2: Clôturer l'enchère                              │
│     └─► Multicast: "VENDU à ClientA pour 550€!"                 │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

---

## 7. Critères d'Évaluation

| Critère | Points | Description |
|---------|--------|-------------|
| **Robustesse** | /4 | Le serveur ne plante pas si un client se déconnecte |
| **Exceptions** | /3 | Gestion correcte de `RemoteException`, `IOException` |
| **Qualité du Code** | /3 | Classes partagées, modularité, commentaires |
| **Concurrence** | /4 | Protection des données partagées (`synchronized`) |
| **TCP** | /3 | Authentification et envoi d'enchères |
| **Multicast** | /3 | Diffusion temps-réel fonctionnelle |
| **RMI** | /3 | Administration distante opérationnelle |
| **Interface** | /2 | Interface Swing fonctionnelle |

**Total : /25 points**

---

## 8. Références TP

| TP | Concepts | Application |
|----|----------|-------------|
| **TP2** | Sockets TCP/UDP, Sérialisation | Connexion client, objets `Message` |
| **TP3** | Multithreading, Swing | Thread par client, interface graphique |
| **TP4** | UDP Multicast | Diffusion des prix |
| **TP5** | Synchronisation, Sémaphores | Protection du prix courant |
| **TP6** | Java RMI | Interface d'administration |

---

## 📝 Notes Importantes

> ⚠️ **Concurrence** : La variable `currentPrice` est protégée par un `ReentrantLock` pour éviter les conditions de course.

> ⚠️ **Multicast** : L'adresse `225.1.1.1` est une adresse de groupe. Tous les clients rejoignent ce groupe automatiquement.

> ⚠️ **RMI** : Le registre RMI est créé automatiquement par le serveur sur le port `1099`.

---

## 👨‍💻 Auteur

Projet conçu pour le module de **Développement d'Applications Réparties**

---

*Bonne chance ! 🎯*
