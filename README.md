# Détection des Attaques DDoS par Amélioration Itérative et Apprentissage Adaptatif

## Description
Ce projet implémente un système avancé de détection d'attaques DDoS qui combine plusieurs approches complémentaires, chacune s'améliorant itérativement et contribuant à l'amélioration des autres méthodes.

## Architecture du Système

### 1. Analyse Statistique de Base
- *Composants Initiaux :*
- 
  - Calcul des métriques de base (moyenne, écart-type)
  - Détection des anomalies par score Z
  - Seuils statiques

- *Améliorations Itératives :*
  - Adaptation dynamique des seuils
  - Intégration des patterns temporels
  - Corrélation avec l'entropie IP
  - Apprentissage des comportements normaux

### 2. Analyse d'Entropie
- *Base Initiale :*
  - Calcul d'entropie des adresses IP
  - Détection des changements brusques

- *Améliorations :*
  - Fenêtre glissante adaptative
  - Combinaison avec l'analyse statistique
  - Enrichissement par apprentissage RL
  - Détection multi-niveaux

### 3. Analyse Multi-Seuils Dynamiques
- *Système Initial :*
  - Trois niveaux de seuils fixes
  - Adaptation basique au trafic

- *Évolution :*
  - Seuils avec taux d'apprentissage différenciés
  - Adaptation indépendante par niveau
  - Intégration des métriques d'entropie
  - Fusion avec les résultats RL

### 4. Apprentissage par Renforcement (RL)
- *Configuration de Base :*
  - Agent RL simple
  - États basés sur le trafic
  - Récompenses binaires

- *Optimisations :*
  - États enrichis (trafic, entropie, variation)
  - Système de récompenses nuancé
  - Apprentissage continu
  - Adaptation des hyperparamètres

### 5. Système de Fusion et Amélioration Continue
- *Mécanismes d'Amélioration :*
  - Feedback entre les méthodes
  - Validation croisée des détections
  - Ajustement automatique des paramètres
  - Apprentissage des patterns d'attaque

## Fonctionnalités et Interface

### 1. Visualisation Avancée
- Graphiques en temps réel pour chaque méthode
- Indicateurs de performance combinés
- Historique des détections
- Analyse comparative des méthodes

### 2. Configuration Adaptative
- Paramètres auto-ajustables
- Profils de détection personnalisables
- Modes d'apprentissage et de détection
- Calibration automatique

### 3. Analyse des Performances
- Métriques de précision par méthode
- Évaluation des faux positifs/négatifs
- Temps de détection
- Efficacité de l'apprentissage

## Installation et Prérequis

### Prérequis Système
- Java JDK 11 ou supérieur
- Maven 3.6+
- RAM : 4GB minimum
- Processeur : 2 cœurs minimum
- Espace disque : 500MB

### Installation

1. Cloner le repository :
   bash
   git clone https://github.com/votre-username/DDoS-Detection-Using-Entropy.git
   cd DDoS-Detection-Using-Entropy


2. Installer les dépendances :
   bash
   mvn clean install


3. Lancer l'application :
   bash
   mvn exec:java -Dexec.mainClass="com.ddos.detection.MainFrame"


## Utilisation

### 1. Configuration Initiale
- Sélection de la méthode de détection
- Configuration des paramètres de base
- Choix du mode (détection/apprentissage)

### 2. Phase d'Apprentissage
- Calibration des seuils
- Entraînement du modèle RL
- Ajustement des paramètres

### 3. Phase de Détection
- Surveillance en temps réel
- Analyse des alertes
- Ajustements dynamiques

### 4. Analyse et Optimisation
- Évaluation des performances
- Ajustement des paramètres
- Amélioration continue

## Structure du Projet

src/
├── main/
│   └── java/
│       └── com/
│           └── ddos/
│               └── detection/
│                   ├── MainFrame.java           # Interface principale
│                   ├── StatisticalAnalyzer.java # Analyse statistique
│                   ├── EntropyAnalyzer.java     # Analyse d'entropie
│                   ├── ui/
│                   │   ├── MultiThresholdWindow.java  # Interface seuils
│                   │   ├── RLSimulationWindow.java    # Simulation RL
│                   │   └── SimulationWindow.java      # Simulation générale
│                   ├── stats/
│                   │   ├── StatisticalDetector.java   # Détection statistique
│                   │   └── EntropyCalculator.java     # Calcul d'entropie
│                   └── rl/
│                       ├── RLAgent.java         # Agent d'apprentissage
│                       ├── RLEnvironment.java   # Environnement RL
│                       └── RLManager.java       # Gestionnaire RL


## Contribution
Les contributions sont les bienvenues ! Pour contribuer :
1. Fork le projet
2. Créer une branche pour votre fonctionnalité
3. Commiter vos changements
4. Pousser vers la branche
5. Ouvrir une Pull Request

## Licence
Ce projet est sous licence MIT. Voir le fichier LICENSE pour plus de détails.

## Auteurs
- [Votre Nom]
- [Autres Contributeurs]

## Contact
Pour toute question ou suggestion, n'hésitez pas à ouvrir une issue ou à nous contacter directement.