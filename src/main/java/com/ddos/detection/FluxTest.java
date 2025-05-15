package com.ddos.detection;

public class FluxTest {
    private FluxSimulation fluxSimulation;
    private boolean isRunning;
    private boolean isDetectMode;

    public FluxTest() {
        this.fluxSimulation = new FluxSimulation();
        this.isRunning = false;
        this.isDetectMode = true;
    }

    public void setDetectMode(boolean detectMode) {
        this.isDetectMode = detectMode;
        this.fluxSimulation.setDetectMode(detectMode);
    }

    public void startTest(int packetsPerSecond, int duration, int sourceIPs, String attackType) {
        if (isRunning) return;
        isRunning = true;
        
        System.out.println("[FluxTest] Démarrage du test avec les paramètres:");
        System.out.println("- Mode: " + (isDetectMode ? "Détection" : "Non-détection"));
        System.out.println("- Paquets/s: " + packetsPerSecond);
        System.out.println("- Durée: " + duration + "s");
        System.out.println("- IPs sources: " + sourceIPs);
        System.out.println("- Type d'attaque: " + attackType);
        System.out.println("- Seuil: " + (isDetectMode ? 
            FluxSimulation.getFluxThreshold() : 
            FluxSimulation.getNormalThreshold()) + " paquets/s");

        fluxSimulation.startSimulation(packetsPerSecond, duration, sourceIPs, attackType);
    }

    public void stopTest() {
        if (!isRunning) return;
        isRunning = false;
        fluxSimulation.stopSimulation();
        System.out.println("[FluxTest] Test arrêté");
    }

    public void addFluxListener(FluxSimulation.FluxListener listener) {
        fluxSimulation.addListener(listener);
    }

    public static double getFluxThreshold() {
        return FluxSimulation.getFluxThreshold();
    }

    public static double getNormalThreshold() {
        return FluxSimulation.getNormalThreshold();
    }
} 