# 🏛️ La Salle des Ventes Distribuée (e-Auction)

> **Module :** Développement d'Applications Réparties / Systèmes Distribués  
> **Type :** Examen Pratique / Projet de Fin de Semestre  
> **Auteur :** Youssef  
> **Date :** Décembre 2025

[![Java](https://img.shields.io/badge/Java-11%2B-orange)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-blue)](https://maven.apache.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.2-green)](https://openjfx.io/)

---

## 📋 Table des Matières

1. [Description du Projet](#1-description-du-projet)
2. [Architecture Technique](#2-architecture-technique)
3. [Structure du Projet](#3-structure-du-projet)
4. [Fonctionnalités](#4-fonctionnalités)
5. [Instructions d'Installation](#5-instructions-dinstallation)
6. [Guide d'Utilisation](#6-guide-dutilisation)
7. [Scénario d'Exécution](#7-scénario-dexécution)
8. [Captures d'Écran](#8-captures-décran)
9. [Technologies Utilisées](#9-technologies-utilisées)

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
│       ├── BuyerClientApp.java      # Interface JavaFX
│       ├── BuyerClient.java         # Version Swing (legacy)
│       └── MulticastListener.java   # Écoute Multicast
│
└── client-admin/                    # Module client admin
    ├── pom.xml
    └── src/main/java/com/auction/admin/
        ├── AdminClientApp.java      # Interface JavaFX
        └── AdminClient.java         # Console RMI (legacy)
```

---

## 4. Fonctionnalités

### Serveur d'Enchères

- ✅ Gestion multi-clients avec ThreadPool
- ✅ Synchronisation des enchères (protection concurrentielle)
- ✅ Diffusion Multicast des mises à jour
- ✅ Service RMI pour l'administration
- ✅ Robustesse (gestion des déconnexions)

### Client Acheteur (JavaFX)

- ✅ Interface graphique moderne JavaFX
- ✅ Connexion TCP avec authentification
- ✅ Envoi d'enchères en temps réel
- ✅ Réception temps-réel via Multicast
- ✅ Affichage de l'historique des enchères
- ✅ Liste des enchères actives
- ✅ Panel de connexion intuitif

### Client Administrateur (JavaFX + RMI)

- ✅ Dashboard d'administration avec thème sombre
- ✅ Connexion RMI sécurisée
- ✅ Démarrer/Clôturer une enchère
- ✅ Voir le statut en cours
- ✅ Lister/Bannir des clients connectés
- ✅ Tableau d'historique des ventes

---

## 5. Instructions d'Installation

### Prérequis

- **Java JDK 11** ou supérieur (testé avec Java 21)
- **Maven 3.6** ou supérieur
- **JavaFX 17.0.2** (inclus dans les dépendances Maven)

### Compilation

```powershell
# Cloner le dépôt
git clone https://github.com/Treshaun/Dev-Repartie.git
Set-Location Dev-Repartie

# Compiler tous les modules
mvn -DskipTests clean install
```

### Exécution

#### Version console (legacy)

```powershell
# Serveur
Start-Process powershell -ArgumentList '-NoExit','-Command','cd .\\server; mvn exec:java ''-Dexec.mainClass=com.auction.server.AuctionServer'''

# Admin (console)
Start-Process powershell -ArgumentList '-NoExit','-Command','cd .\\client-admin; mvn exec:java ''-Dexec.mainClass=com.auction.admin.AdminClient'''

# Acheteur (console)
Start-Process powershell -ArgumentList '-NoExit','-Command','cd .\\client-buyer; mvn exec:java ''-Dexec.mainClass=com.auction.client.BuyerClient'''
```

#### Version JavaFX (UI)

**Étape 1 - Démarrer le Serveur :**

```powershell
Start-Process powershell -ArgumentList '-NoExit','-Command','cd .\\server; mvn exec:java ''-Dexec.mainClass=com.auction.server.AuctionServer'''
# ou depuis un terminal déjà ouvert
Set-Location server
mvn exec:java '-Dexec.mainClass=com.auction.server.AuctionServer'
```

**Étape 2 - Démarrer le Client Admin (nouveau terminal) :**

```powershell
Start-Process powershell -ArgumentList '-NoExit','-Command','cd .\\client-admin; mvn exec:java ''-Dexec.mainClass=com.auction.admin.AdminClientApp'''
# ou depuis un terminal déjà ouvert
Set-Location ..\client-admin
mvn exec:java '-Dexec.mainClass=com.auction.admin.AdminClientApp'
```

**Étape 3 - Démarrer les Clients Acheteurs (plusieurs terminaux) :**

```powershell
Start-Process powershell -ArgumentList '-NoExit','-Command','cd .\\client-buyer; mvn exec:java ''-Dexec.mainClass=com.auction.client.BuyerClientApp'''
# ou depuis un terminal déjà ouvert
Set-Location ..\client-buyer
mvn exec:java '-Dexec.mainClass=com.auction.client.BuyerClientApp'
```

---

## 6. Guide d'Utilisation

### Client Acheteur

1. **Connexion** : Entrez votre nom d'utilisateur et cliquez sur "Se Connecter"
2. **Visualiser les enchères** : Les enchères actives s'affichent automatiquement
3. **Enchérir** : Sélectionnez une enchère, entrez un montant supérieur au prix actuel
4. **Suivre en temps réel** : Les mises à jour arrivent via Multicast

### Client Administrateur

1. **Connexion RMI** : Cliquez sur "Connecter" pour établir la connexion
2. **Créer une enchère** : Renseignez le nom du produit et le prix de départ
3. **Gérer les clients** : Visualisez et bannissez si nécessaire
4. **Clôturer** : Terminez l'enchère pour désigner le gagnant

---

## 7. Scénario d'Exécution

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
│     └─► Menu 1: Démarrer "PC Portable", 500 TND                 │
│                                                                 │
│  4. DIFFUSION                                                   │
│     └─► Multicast: "PC Portable - Prix: 500 TND"                │
│     └─► Tous les clients voient l'enchère                       │
│                                                                 │
│  5. ENCHÈRE                                                     │
│     └─► Client A clique "Enchérir" avec 550 TND                 │
│     └─► Message TCP envoyé au serveur                           │
│                                                                 │
│  6. VALIDATION (synchronized)                                   │
│     └─► Serveur vérifie: 550 TND > 500 TND ✓                    │
│     └─► Met à jour le prix courant                              │
│     └─► Diffuse via Multicast                                   │
│                                                                 │
│  7. MISE À JOUR                                                 │
│     └─► Client B reçoit via Multicast                           │
│     └─► Son interface affiche: "550 TND par ClientA"            │
│                                                                 │
│  8. CLÔTURE (Admin via RMI)                                     │
│     └─► Menu 2: Clôturer l'enchère                              │
│     └─► Multicast: "VENDU à ClientA pour 550 TND!"              │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

---

## 8. Captures d'Écran

### Interface Client Acheteur

- Panel de connexion avec saisie du nom d'utilisateur
- Liste des enchères actives avec prix en temps réel
- Historique des enchères placées
- Formulaire d'enchère intuitif

### Interface Administrateur

- Dashboard sombre moderne
- Contrôle complet des enchères (démarrer/arrêter)
- Liste des clients connectés avec options de bannissement
- Tableau d'historique détaillé

---

## 9. Technologies Utilisées

| Technologie | Version | Usage |
|-------------|---------|-------|
| Java | 11+ | Langage principal |
| JavaFX | 17.0.2 | Interface graphique moderne |
| Maven | 3.6+ | Gestion des dépendances |
| TCP Sockets | - | Communication client-serveur |
| UDP Multicast | - | Diffusion temps réel |
| Java RMI | - | Administration distante |

---

## 📝 Configuration Réseau

| Paramètre | Valeur | Description |
|-----------|--------|-------------|
| TCP Port | `5000` | Port du serveur d'enchères |
| Multicast Group | `225.1.1.1` | Adresse de diffusion |
| Multicast Port | `6000` | Port multicast |
| RMI Port | `1099` | Port du registre RMI |
| RMI Service | `AuctionAdmin` | Nom du service RMI |

---

## 🔒 Notes Techniques

> **Concurrence** : La variable `currentPrice` est protégée par un `ReentrantLock` pour éviter les conditions de course.

> **Multicast** : L'adresse `225.1.1.1` est une adresse de groupe. Tous les clients rejoignent ce groupe automatiquement.

> **RMI** : Le registre RMI est créé automatiquement par le serveur sur le port `1099`.

> **JavaFX Warning** : Le warning "Unsupported JavaFX configuration" peut apparaître mais n'affecte pas le fonctionnement.

---

## 👨‍💻 Auteur

**Youssef** - Projet conçu pour le module de **Développement d'Applications Réparties**

📧 GitHub: [@youssef7511](https://github.com/youssef7511)

---

## 📄 Licence

Ce projet est développé à des fins éducatives dans le cadre du cours de Développement d'Applications Réparties.

---

🎯 **Bonne utilisation !**
