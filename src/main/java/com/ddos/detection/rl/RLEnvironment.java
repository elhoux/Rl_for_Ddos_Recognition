package com.ddos.detection.rl;

import java.util.ArrayList;
import java.util.List;

public class RLEnvironment {
    private double baselineTraffic;
    private double currentTraffic;
    private List<Double> trafficHistory;
    private static final int HISTORY_SIZE = 10;
    private static final double ATTACK_THRESHOLD = 1.5; // 150% du trafic de base

    public RLEnvironment(double baselineTraffic) {
        this.baselineTraffic = baselineTraffic;
        this.currentTraffic = baselineTraffic;
        this.trafficHistory = new ArrayList<>();
    }

    public void setCurrentTraffic(double traffic) {
        this.currentTraffic = traffic;
        trafficHistory.add(traffic);
        if (trafficHistory.size() > HISTORY_SIZE) {
            trafficHistory.remove(0);
        }
    }

    public String getCurrentState() {
        // Calculer les caractéristiques de l'état
        double avgTraffic = trafficHistory.stream().mapToDouble(Double::doubleValue).average().orElse(currentTraffic);
        double variance = calculateVariance(avgTraffic);
        double trend = calculateTrend();

        // Discrétiser les caractéristiques
        int trafficLevel = discretizeTraffic(currentTraffic);
        int varianceLevel = discretizeVariance(variance);
        int trendLevel = discretizeTrend(trend);

        // Combiner en un état unique
        return String.format("%d:%d:%d", trafficLevel, varianceLevel, trendLevel);
    }

    public double step(int action) {
        double reward = 0.0;
        boolean isAttack = currentTraffic > baselineTraffic * ATTACK_THRESHOLD;
        double trafficRatio = currentTraffic / baselineTraffic;

        // Récompenses selon l'action et l'état réel
        switch (action) {
            case 0: // Normal
                if (!isAttack && trafficRatio < 1.3) {
                    reward = 1.0; // Bonne détection du trafic normal
                } else {
                    reward = -2.0; // Pénalité pour avoir manqué une attaque
                }
                break;
            case 1: // Alerte
                if (trafficRatio >= 1.3 && trafficRatio < 1.8) {
                    reward = 0.5; // Bonne détection d'une anomalie légère
                } else {
                    reward = -0.5; // Pénalité pour fausse alerte
                }
                break;
            case 2: // Attaque
                if (isAttack && trafficRatio >= 1.8) {
                    reward = 2.0; // Excellente détection d'une attaque
                } else {
                    reward = -1.0; // Pénalité pour faux positif
                }
                break;
        }

        return reward;
    }

    private double calculateVariance(double mean) {
        return trafficHistory.stream()
            .mapToDouble(t -> Math.pow(t - mean, 2))
            .average()
            .orElse(0.0);
    }

    private double calculateTrend() {
        if (trafficHistory.size() < 2) return 0.0;
        return (trafficHistory.get(trafficHistory.size() - 1) - 
                trafficHistory.get(trafficHistory.size() - 2)) / baselineTraffic;
    }

    private int discretizeTraffic(double traffic) {
        double ratio = traffic / baselineTraffic;
        if (ratio < 1.3) return 0;      // Normal
        if (ratio < 1.8) return 1;      // Suspect
        return 2;                       // Attaque
    }

    private int discretizeVariance(double variance) {
        double normalizedVar = variance / (baselineTraffic * baselineTraffic);
        if (normalizedVar < 0.1) return 0;
        if (normalizedVar < 0.3) return 1;
        return 2;
    }

    private int discretizeTrend(double trend) {
        if (trend < -0.1) return 0;
        if (trend < 0.1) return 1;
        return 2;
    }

    public double getBaselineTraffic() {
        return baselineTraffic;
    }

    public double getCurrentTraffic() {
        return currentTraffic;
    }
} 