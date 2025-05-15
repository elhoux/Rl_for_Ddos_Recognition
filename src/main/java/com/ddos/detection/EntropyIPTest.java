package com.ddos.detection;

import java.util.Map;

public class EntropyIPTest {
    public static void main(String[] args) throws InterruptedException {
        int packetsPerSecond = 2000;
        int duration = 20; // secondes

        // Cas détection (entropie basse, attaque détectée)
        int sourceIPsDetect = 10;
        System.out.println("--- Cas détection (entropie basse, attaque détectée) ---");
        for (int t = 0; t < duration; t++) {
            Map<String, Integer> traffic = EntropySimulator.simulateTraffic(packetsPerSecond, sourceIPsDetect, "detect");
            double entropy = calculateEntropy(traffic);
            System.out.printf("Seconde %2d : Entropie = %.3f | IPs uniques = %d\n", t+1, entropy, traffic.size());
            System.out.println("Distribution des paquets :");
            for (Map.Entry<String, Integer> entry : traffic.entrySet()) {
                System.out.printf("  %s : %d\n", entry.getKey(), entry.getValue());
            }
            System.out.println("-----------------------------");
            Thread.sleep(1000);
        }

        // Cas non-détection (entropie haute, attaque non détectée)
        int sourceIPsFail = 200;
        System.out.println("\n--- Cas non-détection (entropie haute, attaque non détectée) ---");
        for (int t = 0; t < duration; t++) {
            Map<String, Integer> traffic = EntropySimulator.simulateTraffic(packetsPerSecond, sourceIPsFail, "fail");
            double entropy = calculateEntropy(traffic);
            System.out.printf("Seconde %2d : Entropie = %.3f | IPs uniques = %d\n", t+1, entropy, traffic.size());
            System.out.println("Distribution des paquets :");
            for (Map.Entry<String, Integer> entry : traffic.entrySet()) {
                System.out.printf("  %s : %d\n", entry.getKey(), entry.getValue());
            }
            System.out.println("-----------------------------");
            Thread.sleep(1000);
        }
    }

    // Méthode utilitaire pour calculer l'entropie
    public static double calculateEntropy(Map<String, Integer> ipCounts) {
        int totalPackets = ipCounts.values().stream().mapToInt(Integer::intValue).sum();
        double entropy = 0.0;
        for (int count : ipCounts.values()) {
            double probability = (double) count / totalPackets;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        return entropy;
    }
} 