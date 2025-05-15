package com.ddos.detection;

import java.util.*;
import java.util.concurrent.*;

public class FluxSimulation {
    // Seuils de détection
    private static final double BASE_ATTACK_THRESHOLD = 5000.0; // Seuil de base pour le mode détection
    private static final double BASE_NORMAL_THRESHOLD = 2000.0; // Seuil de base pour le mode non-détection
    private static final int NORMAL_PERIOD = 3; // Période initiale de trafic normal (en secondes)
    
    // Paramètres de simulation
    private static final double NORMAL_TRAFFIC_BASE = 1000.0; // Trafic de base normal
    private static final double ATTACK_TRAFFIC_BASE = 8000.0; // Trafic de base en attaque
    private static final double NORMAL_VARIATION_PERCENT = 0.3; // 30% de variation pour trafic normal
    private static final double ATTACK_VARIATION_PERCENT = 0.5; // 20% de variation pour trafic d'attaque
    
    // Paramètres spécifiques au mode non-détection
    private static final double NON_DETECT_BASE_TRAFFIC = 60.0; // Trafic de base en non-détection
    private static final double NON_DETECT_MAX_TRAFFIC = 100.0; // Trafic maximum en non-détection
    private static final double NON_DETECT_MIN_TRAFFIC = 30.0; // Trafic minimum en non-détection
    
    private volatile boolean isRunning = false;
    private final List<FluxListener> listeners = new ArrayList<>();
    private ScheduledExecutorService executor;
    private Random random = new Random();
    private boolean isDetectMode = true;
    private long startTime;
    private double lastFluxValue = NON_DETECT_BASE_TRAFFIC;

    public interface FluxListener {
        void onFluxUpdate(double fluxValue, boolean isAttack);
        void onSimulationComplete();
    }

    public void addListener(FluxListener listener) {
        listeners.add(listener);
    }

    public void setDetectMode(boolean detectMode) {
        this.isDetectMode = detectMode;
        this.lastFluxValue = detectMode ? NORMAL_TRAFFIC_BASE : NON_DETECT_BASE_TRAFFIC;
    }

    public void startSimulation(int packetsPerSecond, int duration, int sourceIPs, String attackType) {
        if (isRunning) return;
        isRunning = true;
        startTime = System.currentTimeMillis();
        executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            if (!isRunning) {
                executor.shutdown();
                return;
            }

            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            double currentFlux;

            if (!isDetectMode) {
                // Mode non-détection : trafic normal avec variations douces
                currentFlux = generateNonDetectionTraffic(elapsedSeconds);
            } else {
                // Mode détection
                currentFlux = generateDetectionTraffic(elapsedSeconds);
            }

            // Ajuster selon le nombre d'IPs
            double ipFactor = calculateIPFactor(sourceIPs);
            double adjustedFlux = currentFlux * ipFactor;

            // En mode non-détection, s'assurer que le flux reste toujours sous le seuil
            if (!isDetectMode) {
                adjustedFlux = Math.min(adjustedFlux, NON_DETECT_MAX_TRAFFIC);
            }

            // Déterminer si c'est une attaque (uniquement en mode détection)
            boolean isAttack = isDetectMode && !isNormalPeriod(elapsedSeconds) && adjustedFlux > BASE_ATTACK_THRESHOLD;

            // Mettre à jour la dernière valeur
            lastFluxValue = adjustedFlux;

            // Notifier les listeners
            for (FluxListener listener : listeners) {
                listener.onFluxUpdate(adjustedFlux, isAttack);
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Programmer l'arrêt de la simulation
        executor.schedule(() -> {
            stopSimulation();
            for (FluxListener listener : listeners) {
                listener.onSimulationComplete();
            }
        }, duration, TimeUnit.SECONDS);
    }

    private double generateNonDetectionTraffic(long elapsedSeconds) {
        // Utiliser plusieurs fréquences non multiples pour éviter la synchronisation
        double f1 = 1.1; // Fréquence non entière pour éviter la périodicité
        double f2 = 2.7;
        double f3 = 4.3;
        double f4 = 7.5;
        
        // Variations avec des fréquences non synchronisées
        double v1 = Math.sin(elapsedSeconds * f1) * 10.0;
        double v2 = Math.cos(elapsedSeconds * f2) * 8.0;
        double v3 = Math.sin(elapsedSeconds * f3 + Math.PI/3) * 12.0;
        double v4 = Math.cos(elapsedSeconds * f4 + Math.PI/6) * 7.0;
        
        // Variation chaotique avec produit de sinus
        double chaos = Math.sin(elapsedSeconds * 0.3) * Math.cos(elapsedSeconds * 0.7) * 15.0;
        
        // Bruit aléatoire avec amplitude variable
        double randomAmplitude = 5.0 + Math.abs(Math.sin(elapsedSeconds * 0.1)) * 10.0;
        double noise = (random.nextDouble() * 2 - 1) * randomAmplitude;
        
        // Tendance lente avec période variable
        double trend = Math.sin(elapsedSeconds * 0.05 + Math.cos(elapsedSeconds * 0.02) * 0.5) * 20.0;
        
        // Variation supplémentaire basée sur le carré du temps
        double timeVar = Math.sin(Math.pow(elapsedSeconds % 10, 2) * 0.1) * 10.0;
        
        // Combiner toutes les variations de manière non linéaire
        double totalVariation = v1 + v2 * (1 + Math.abs(Math.sin(elapsedSeconds * 0.1))) +
                              v3 * (1 + Math.abs(Math.cos(elapsedSeconds * 0.15))) +
                              v4 + chaos + noise + trend + timeVar;
        
        // Ajouter une composante exponentielle pour éviter la stabilisation
        double expComponent = Math.exp(Math.sin(elapsedSeconds * 0.1)) * 5.0;
        totalVariation += expComponent;
        
        // Calculer le nouveau flux
        double newFlux = NON_DETECT_BASE_TRAFFIC + totalVariation;
        
        // Transition douce avec mémoire
        double alpha = 0.7; // Facteur de lissage
        newFlux = alpha * newFlux + (1 - alpha) * lastFluxValue;
        
        // Garantir les limites tout en permettant des variations
        return Math.max(NON_DETECT_MIN_TRAFFIC, Math.min(newFlux, NON_DETECT_MAX_TRAFFIC));
    }

    private double generateDetectionTraffic(long elapsedSeconds) {
        if (isNormalPeriod(elapsedSeconds)) {
            // Période initiale : trafic normal
            double variation = (random.nextDouble() * 2 - 1) * NORMAL_VARIATION_PERCENT;
            return NORMAL_TRAFFIC_BASE * (1.0 + variation);
        } else {
            // Après la période initiale
            if (random.nextDouble() < 0.7) { // 70% de chance d'attaque
                double variation = (random.nextDouble() * 2 - 1) * ATTACK_VARIATION_PERCENT;
                return ATTACK_TRAFFIC_BASE * (1.0 + variation);
            } else {
                double variation = (random.nextDouble() * 2 - 1) * NORMAL_VARIATION_PERCENT;
                return NORMAL_TRAFFIC_BASE * (1.0 + variation);
            }
        }
    }

    private boolean isNormalPeriod(long elapsedSeconds) {
        return elapsedSeconds < NORMAL_PERIOD;
    }

    private double calculateIPFactor(int sourceIPs) {
        if (isDetectMode) {
            // En mode détection, peu d'IPs = trafic plus concentré
            return Math.max(0.5, 1.0 - (Math.log10(sourceIPs) / 4.0));
        } else {
            // En mode non-détection, facteur minimal pour maintenir un trafic bas
            return 0.6;
        }
    }

    public void stopSimulation() {
        isRunning = false;
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static double getFluxThreshold() {
        return BASE_ATTACK_THRESHOLD;
    }

    public static double getNormalThreshold() {
        return BASE_NORMAL_THRESHOLD;
    }
} 