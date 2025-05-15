package com.ddos.detection.config;

public class StatisticalConfig {
    // Paramètres de la fenêtre d'analyse
    public static final int WINDOW_SIZE = 60;  // Taille de la fenêtre en secondes
    public static final int UPDATE_INTERVAL = 1000;  // Intervalle de mise à jour en ms
    
    // Seuils de détection
    public static final double CORRELATION_THRESHOLD = 0.8;
    public static final double ZSCORE_THRESHOLD = 2.0;
    public static final double PROTOCOL_ANOMALY_THRESHOLD = 0.5;
    
    // Paramètres de simulation
    public static final double NORMAL_TRAFFIC_MEAN = 1000.0;  // paquets/s
    public static final double NORMAL_TRAFFIC_STD = 200.0;    // écart-type
    public static final double ATTACK_TRAFFIC_MULTIPLIER = 3.0;
    
    // Distribution des protocoles (normal)
    public static final double TCP_RATIO = 0.7;
    public static final double UDP_RATIO = 0.2;
    public static final double ICMP_RATIO = 0.1;
    
    // Paramètres d'apprentissage
    public static final double LEARNING_RATE = 0.1;  // Taux d'apprentissage pour la mise à jour de la ligne de base
    public static final int MIN_SAMPLES = 30;        // Nombre minimum d'échantillons pour l'analyse
} 