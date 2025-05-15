package com.ddos.detection.simulation;

import com.ddos.detection.config.StatisticalConfig;
import java.util.*;
import java.util.concurrent.*;

public class StatisticalSimulator {
    private final Random random;
    private final ScheduledExecutorService executor;
    private final List<SimulationListener> listeners;
    private volatile boolean isRunning;
    private boolean isAttackMode;
    private int uniqueIPs;
    private double baseTraffic;

    public interface SimulationListener {
        void onTrafficGenerated(TrafficData data);
        void onSimulationComplete();
    }

    public static class TrafficData {
        public final double packetsPerSecond;
        public final double bytesPerSecond;
        public final int uniqueIPs;
        public final Map<String, Integer> protocolDistribution;
        public final long timestamp;

        public TrafficData(double pps, double bps, int ips, Map<String, Integer> protocols) {
            this.packetsPerSecond = pps;
            this.bytesPerSecond = bps;
            this.uniqueIPs = ips;
            this.protocolDistribution = new HashMap<>(protocols);
            this.timestamp = System.currentTimeMillis();
        }
    }

    public StatisticalSimulator() {
        this.random = new Random();
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.listeners = new ArrayList<>();
        this.isRunning = false;
        this.isAttackMode = false;
        this.uniqueIPs = 100;
        this.baseTraffic = StatisticalConfig.NORMAL_TRAFFIC_MEAN;
    }

    public void addListener(SimulationListener listener) {
        listeners.add(listener);
    }

    public void setAttackMode(boolean attackMode) {
        this.isAttackMode = attackMode;
    }

    public void setUniqueIPs(int ips) {
        this.uniqueIPs = ips;
    }

    public void setBaseTraffic(double traffic) {
        this.baseTraffic = traffic;
    }

    public void startSimulation(int durationSeconds) {
        if (isRunning) return;
        isRunning = true;

        // Planifier la génération de trafic
        executor.scheduleAtFixedRate(
            this::generateTraffic,
            0,
            StatisticalConfig.UPDATE_INTERVAL,
            TimeUnit.MILLISECONDS
        );

        // Planifier l'arrêt de la simulation
        executor.schedule(() -> {
            stopSimulation();
        }, durationSeconds, TimeUnit.SECONDS);
    }

    private void generateTraffic() {
        if (!isRunning) return;

        // Générer le trafic de base avec variation gaussienne
        double baseNoise = random.nextGaussian() * StatisticalConfig.NORMAL_TRAFFIC_STD;
        double currentTraffic = baseTraffic + baseNoise;

        // Ajuster le trafic en mode attaque
        if (isAttackMode) {
            currentTraffic *= StatisticalConfig.ATTACK_TRAFFIC_MULTIPLIER;
        }

        // Générer la distribution des protocoles
        Map<String, Integer> protocols = generateProtocolDistribution(currentTraffic);

        // Calculer les octets par seconde (variable selon le protocole)
        double bytesPerSecond = calculateBytesPerSecond(currentTraffic, protocols);

        // Créer les données de trafic
        TrafficData data = new TrafficData(
            currentTraffic,
            bytesPerSecond,
            uniqueIPs,
            protocols
        );

        // Notifier les écouteurs
        for (SimulationListener listener : listeners) {
            listener.onTrafficGenerated(data);
        }
    }

    private Map<String, Integer> generateProtocolDistribution(double totalTraffic) {
        Map<String, Integer> distribution = new HashMap<>();
        
        if (isAttackMode) {
            // En mode attaque, favoriser un protocole spécifique
            distribution.put("TCP", (int)(totalTraffic * 0.9));
            distribution.put("UDP", (int)(totalTraffic * 0.08));
            distribution.put("ICMP", (int)(totalTraffic * 0.02));
        } else {
            // Distribution normale
            distribution.put("TCP", (int)(totalTraffic * StatisticalConfig.TCP_RATIO));
            distribution.put("UDP", (int)(totalTraffic * StatisticalConfig.UDP_RATIO));
            distribution.put("ICMP", (int)(totalTraffic * StatisticalConfig.ICMP_RATIO));
        }
        
        return distribution;
    }

    private double calculateBytesPerSecond(double pps, Map<String, Integer> protocols) {
        double totalBytes = 0;
        
        // Tailles moyennes des paquets par protocole (en octets)
        Map<String, Integer> packetSizes = new HashMap<>();
        packetSizes.put("TCP", 576);  // Taille moyenne d'un paquet TCP
        packetSizes.put("UDP", 512);  // Taille moyenne d'un paquet UDP
        packetSizes.put("ICMP", 64);  // Taille moyenne d'un paquet ICMP

        for (Map.Entry<String, Integer> entry : protocols.entrySet()) {
            String protocol = entry.getKey();
            int packetCount = entry.getValue();
            int avgSize = packetSizes.getOrDefault(protocol, 512);
            totalBytes += packetCount * avgSize;
        }

        return totalBytes;
    }

    public void stopSimulation() {
        isRunning = false;
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (SimulationListener listener : listeners) {
            listener.onSimulationComplete();
        }
    }
} 