package com.ddos.detection;

import com.ddos.detection.simulation.StatisticalSimulator;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

public class StatisticalManager implements StatisticalSimulator.SimulationListener {
    private final StatisticalAnalyzer analyzer;
    private final StatisticalSimulator simulator;
    private final List<AnalysisListener> listeners;
    private boolean isRunning;
    private AnalysisListener listener;

    public interface AnalysisListener {
        void onAnalysisUpdate(AnalysisResult result);
        void onAnalysisComplete();
    }

    public static class AnalysisResult {
        public double currentValue;
        public double confidence;
        public boolean isAnomaly;
        public String anomalyType;

        // Constructeur par défaut
        public AnalysisResult() {
        }

        // Constructeur avec paramètres
        public AnalysisResult(double currentValue, boolean isAnomaly, String anomalyType, double confidence) {
            this.currentValue = currentValue;
            this.isAnomaly = isAnomaly;
            this.anomalyType = anomalyType;
            this.confidence = confidence;
        }
    }

    public StatisticalManager() {
        this.analyzer = new StatisticalAnalyzer();
        this.simulator = new StatisticalSimulator();
        this.listeners = new ArrayList<>();
        this.isRunning = false;

        // Configuration des écouteurs
        simulator.addListener(this);
        analyzer.addListener(new StatisticalAnalyzer.StatisticalListener() {
            @Override
            public void onAnomalyDetected(StatisticalAnalyzer.AnomalyReport report) {
                notifyUpdate(
                    report.zScore,
                    true,
                    report.anomalyType,
                    report.confidence
                );
            }

            @Override
            public void onAnalysisComplete() {
                notifyComplete();
            }
        });
    }

    public void addListener(AnalysisListener listener) {
        this.listener = listener;
    }

    public void removeListener() {
        this.listener = null;
    }

    public void startAnalysis(boolean attackMode, int uniqueIPs, double baseTraffic, int duration) {
        if (isRunning) return;
        isRunning = true;

        // Configuration du simulateur
        simulator.setAttackMode(attackMode);
        simulator.setUniqueIPs(uniqueIPs);
        simulator.setBaseTraffic(baseTraffic);

        // Démarrage de l'analyse
        analyzer.startAnalysis();
        simulator.startSimulation(duration);
    }

    public void stopAnalysis() {
        if (!isRunning) return;
        isRunning = false;

        simulator.stopSimulation();
        analyzer.stopAnalysis();

        notifyComplete();
    }

    @Override
    public void onTrafficGenerated(StatisticalSimulator.TrafficData data) {
        // Transmission des données du simulateur à l'analyseur
        analyzer.addSample(
            data.packetsPerSecond,
            data.bytesPerSecond,
            data.uniqueIPs,
            data.protocolDistribution
        );
    }

    @Override
    public void onSimulationComplete() {
        stopAnalysis();
    }

    protected void notifyUpdate(double currentValue, boolean isAnomaly, String anomalyType, double confidence) {
        if (listener != null) {
            // Créer un nouveau résultat avec les données actuelles
            AnalysisResult result = new AnalysisResult();
            result.currentValue = currentValue;
            result.isAnomaly = isAnomaly;
            result.anomalyType = anomalyType;
            result.confidence = confidence;
            
            // Notifier l'écouteur sur l'EDT de Swing
            SwingUtilities.invokeLater(() -> {
                try {
                    listener.onAnalysisUpdate(result);
                } catch (Exception e) {
                    System.err.println("Erreur lors de la mise à jour des statistiques: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    protected void notifyComplete() {
        if (listener != null) {
            listener.onAnalysisComplete();
        }
    }
} 