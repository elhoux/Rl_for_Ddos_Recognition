package com.ddos.detection;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EntropySimulator {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;
    private final Map<String, Integer> ipCounts = new ConcurrentHashMap<>();
    private final List<EntropyListener> listeners = new ArrayList<>();
    public static final double ENTROPY_THRESHOLD = 1.5; // Seuil d'alerte pour l'entropie
    private Random random;
    private double baselineEntropy;
    private double attackThreshold;

    public interface EntropyListener {
        void onEntropyUpdate(double entropy, boolean isAttack);
        void onSimulationComplete();
    }

    public EntropySimulator() {
        this.random = new Random();
        this.baselineEntropy = 0.7; // Valeur normale d'entropie
        this.attackThreshold = 0.3; // Seuil de détection d'attaque
    }

    public void addListener(EntropyListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(EntropyListener listener) {
        listeners.remove(listener);
    }

    public void startSimulation(int packetsPerSecond, int duration, int sourceIPs, String attackType) {
        if (running.get()) {
            return;
        }

        running.set(true);
        executor = Executors.newScheduledThreadPool(2);
        
        // Tâche de génération de trafic
        executor.scheduleAtFixedRate(() -> {
            if (!running.get()) {
                return;
            }

            // Générer des paquets avec des adresses IP
            generateTraffic(packetsPerSecond, sourceIPs, attackType);
            
            // Calculer et notifier l'entropie
            double entropy = calculateEntropy();
            boolean isAttack = entropy < ENTROPY_THRESHOLD;
            
            for (EntropyListener listener : listeners) {
                System.out.println("[DEBUG] Notification du listener : entropie=" + entropy + ", isAttack=" + isAttack);
                listener.onEntropyUpdate(entropy, isAttack);
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Arrêt automatique après la durée spécifiée
        executor.schedule(() -> {
            stopSimulation();
        }, duration, TimeUnit.SECONDS);
    }

    private void generateTraffic(int packetsPerSecond, int sourceIPs, String attackType) {
        int nbIPs = Math.max(2, sourceIPs);
        for (int i = 0; i < packetsPerSecond; i++) {
            String ip;
            // Si beaucoup d'IPs (non-détection), on répartit uniformément
            if (nbIPs > 50) {
                ip = "192.168.1." + (random.nextInt(nbIPs) + 1);
            } else if (attackType.equals("SYN Flood")) {
                // 90% du trafic sur la première IP, 10% sur les autres
                if (random.nextDouble() < 0.9) {
                    ip = "192.168.1.1";
                } else {
                    ip = "192.168.1." + (random.nextInt(nbIPs - 1) + 2);
                }
            } else {
                ip = "192.168.1." + (random.nextInt(nbIPs) + 1);
            }
            ipCounts.merge(ip, 1, Integer::sum);
        }
        System.out.println("[DEBUG] Trafic généré : " + ipCounts);
    }

    private double calculateEntropy() {
        if (ipCounts.isEmpty()) {
            return 0.0;
        }
        int totalPackets = ipCounts.values().stream().mapToInt(Integer::intValue).sum();
        double entropy = 0.0;
        for (int count : ipCounts.values()) {
            double probability = (double) count / totalPackets;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        System.out.println("[DEBUG] Entropie calculée : " + entropy);
        return entropy;
    }

    public void stopSimulation() {
        if (!running.get()) {
            return;
        }

        running.set(false);
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

        ipCounts.clear();
        
        for (EntropyListener listener : listeners) {
            listener.onSimulationComplete();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    // Ajout d'une méthode utilitaire pour générer des paquets simulés selon le scénario
    public static Map<String, Integer> simulateTraffic(int packetsPerSecond, int sourceIPs, String scenario) {
        // scenario: "normal", "detect", "fail"
        Map<String, Integer> ipCounts = new java.util.HashMap<>();
        java.util.Random random = new java.util.Random();
        int nbIPs = sourceIPs;
        if (scenario.equals("detect")) {
            nbIPs = Math.max(1, sourceIPs / 20); // très peu d'IP pour entropie basse
        } else if (scenario.equals("fail")) {
            nbIPs = Math.max(sourceIPs, 100); // beaucoup d'IP pour entropie haute
        }
        for (int i = 0; i < packetsPerSecond; i++) {
            String ip = "192.168.1." + (random.nextInt(nbIPs) + 1);
            ipCounts.merge(ip, 1, Integer::sum);
        }
        return ipCounts;
    }

    public void start() {
        running.set(true);
    }

    public void stop() {
        running.set(false);
        notifySimulationComplete();
    }

    public void reset() {
        stop();
    }

    public void updateEntropy(double timeInSeconds) {
        if (!running.get()) return;

        // Simuler une valeur d'entropie
        double entropy = calculateEntropy(timeInSeconds);
        boolean isAttack = entropy < attackThreshold;

        // Notifier les écouteurs
        notifyEntropyUpdate(entropy, isAttack);
    }

    private double calculateEntropy(double timeInSeconds) {
        // Simulation simple d'entropie
        double noise = random.nextGaussian() * 0.1;
        
        // Ajouter une perturbation périodique pour simuler des attaques
        double periodicComponent = Math.sin(timeInSeconds / 10.0) * 0.2;
        
        return baselineEntropy + noise + periodicComponent;
    }

    private void notifyEntropyUpdate(double entropy, boolean isAttack) {
        for (EntropyListener listener : listeners) {
            listener.onEntropyUpdate(entropy, isAttack);
        }
    }

    private void notifySimulationComplete() {
        for (EntropyListener listener : listeners) {
            listener.onSimulationComplete();
        }
    }

    public void setBaselineEntropy(double baselineEntropy) {
        this.baselineEntropy = baselineEntropy;
    }

    public void setAttackThreshold(double attackThreshold) {
        this.attackThreshold = attackThreshold;
    }
} 