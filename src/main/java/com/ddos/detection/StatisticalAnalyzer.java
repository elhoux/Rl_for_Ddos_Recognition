package com.ddos.detection;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import java.util.*;

public class StatisticalAnalyzer {
    private final DescriptiveStatistics packetStats;
    private final DescriptiveStatistics byteStats;
    private final DescriptiveStatistics ipStats;
    private final List<StatisticalListener> listeners;
    private boolean isRunning;
    private final Map<String, Double> baselineProtocolRatios;
    private double baselineTraffic;
    private double baselineStdDev;
    private double zScoreThreshold;

    public interface StatisticalListener {
        void onAnomalyDetected(AnomalyReport report);
        void onAnalysisComplete();
    }

    public static class AnomalyReport {
        public final double zScore;
        public final String anomalyType;
        public final double confidence;

        public AnomalyReport(double zScore, String type, double confidence) {
            this.zScore = zScore;
            this.anomalyType = type;
            this.confidence = confidence;
        }
    }

    public StatisticalAnalyzer() {
        this.packetStats = new DescriptiveStatistics(60); // 1 minute de données
        this.byteStats = new DescriptiveStatistics(60);
        this.ipStats = new DescriptiveStatistics(60);
        this.listeners = new ArrayList<>();
        this.isRunning = false;
        this.baselineProtocolRatios = new HashMap<>();
        this.baselineTraffic = 1000.0; // Valeur par défaut
        this.baselineStdDev = 200.0;   // Valeur par défaut
        this.zScoreThreshold = 2.0;    // Valeur par défaut
        initializeBaseline();
    }

    private void initializeBaseline() {
        baselineProtocolRatios.put("TCP", 0.7);
        baselineProtocolRatios.put("UDP", 0.2);
        baselineProtocolRatios.put("ICMP", 0.1);
    }

    public void addListener(StatisticalListener listener) {
        listeners.add(listener);
    }

    public void startAnalysis() {
        isRunning = true;
    }

    public void stopAnalysis() {
        isRunning = false;
        notifyAnalysisComplete();
    }

    public void addSample(double packetsPerSecond, double bytesPerSecond, int uniqueIPs, Map<String, Integer> protocols) {
        if (!isRunning) return;

        // Ajout des échantillons
        packetStats.addValue(packetsPerSecond);
        byteStats.addValue(bytesPerSecond);
        ipStats.addValue(uniqueIPs);

        // Analyse des anomalies
        analyzeTrafficPatterns(packetsPerSecond, bytesPerSecond, uniqueIPs);
        analyzeProtocolDistribution(protocols);
    }

    private void analyzeTrafficPatterns(double pps, double bps, int ips) {
        // Calcul des Z-scores
        double ppsZScore = calculateZScore(pps, packetStats);
        double bpsZScore = calculateZScore(bps, byteStats);
        double ipsZScore = calculateZScore(ips, ipStats);

        // Détection d'anomalies basée sur les Z-scores
        if (Math.abs(ppsZScore) > 2.0) {
            notifyAnomaly(new AnomalyReport(
                ppsZScore,
                "Anomalie de trafic (paquets/s)",
                calculateConfidence(ppsZScore)
            ));
        }

        if (Math.abs(bpsZScore) > 2.0) {
            notifyAnomaly(new AnomalyReport(
                bpsZScore,
                "Anomalie de volume (octets/s)",
                calculateConfidence(bpsZScore)
            ));
        }

        if (Math.abs(ipsZScore) > 2.0) {
            notifyAnomaly(new AnomalyReport(
                ipsZScore,
                "Anomalie d'adresses IP",
                calculateConfidence(ipsZScore)
            ));
        }
    }

    private void analyzeProtocolDistribution(Map<String, Integer> protocols) {
        // Calcul des ratios actuels
        int total = protocols.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return;

        for (Map.Entry<String, Integer> entry : protocols.entrySet()) {
            String protocol = entry.getKey();
            double currentRatio = entry.getValue() / (double) total;
            double baselineRatio = baselineProtocolRatios.getOrDefault(protocol, 0.0);
            
            // Détection d'anomalies dans la distribution des protocoles
            double difference = Math.abs(currentRatio - baselineRatio);
            if (difference > 0.2) { // Seuil de 20% de différence
                notifyAnomaly(new AnomalyReport(
                    difference,
                    "Anomalie de distribution " + protocol,
                    calculateConfidence(difference * 5) // Normalisation pour le calcul de confiance
                ));
            }
        }
    }

    private double calculateZScore(double value, DescriptiveStatistics stats) {
        if (stats.getN() < 2) return 0.0;
        return (value - stats.getMean()) / stats.getStandardDeviation();
    }

    private double calculateConfidence(double zScore) {
        // Conversion du Z-score en niveau de confiance (0-1)
        return Math.min(1.0, Math.abs(zScore) / 4.0);
    }

    private void notifyAnomaly(AnomalyReport report) {
        for (StatisticalListener listener : listeners) {
            listener.onAnomalyDetected(report);
        }
    }

    private void notifyAnalysisComplete() {
        for (StatisticalListener listener : listeners) {
            listener.onAnalysisComplete();
        }
    }

    public void setBaselineParameters(int baseTraffic, double stdDev, double threshold) {
        this.baselineTraffic = baseTraffic;
        this.baselineStdDev = stdDev;
        this.zScoreThreshold = threshold;
        
        // Réinitialisation des statistiques avec les nouvelles valeurs de base
        packetStats.clear();
        packetStats.addValue(baseTraffic);
        
        // Mise à jour des seuils de détection
        updateDetectionThresholds();
    }

    private void updateDetectionThresholds() {
        // Mise à jour des seuils basés sur les nouveaux paramètres
        double upperThreshold = baselineTraffic + (zScoreThreshold * baselineStdDev);
        double lowerThreshold = baselineTraffic - (zScoreThreshold * baselineStdDev);
        
        // Utilisation des nouveaux seuils dans l'analyse
        if (isRunning) {
            analyzeTrafficPatterns(packetStats.getMean(), byteStats.getMean(), (int)ipStats.getMean());
        }
    }
} 