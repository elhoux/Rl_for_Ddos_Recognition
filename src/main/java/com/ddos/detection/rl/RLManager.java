package com.ddos.detection.rl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RLManager {
    private RLAgent agent;
    private RLEnvironment environment;
    private ScheduledExecutorService executor;
    private AtomicBoolean isRunning;
    private RLCallback callback;
    private int episodeCount;
    private static final int UPDATE_INTERVAL_MS = 100;

    public interface RLCallback {
        void onUpdate(double reward, double epsilon, int episode);
    }

    public RLManager(double baselineTraffic, RLCallback callback) {
        this.agent = new RLAgent(0.1, 0.9, 0.3);
        this.environment = new RLEnvironment(baselineTraffic);
        this.isRunning = new AtomicBoolean(false);
        this.callback = callback;
        this.episodeCount = 0;
    }

    public void startTraining(Runnable onTrainingComplete) {
        if (isRunning.get()) return;
        
        isRunning.set(true);
        executor = Executors.newSingleThreadScheduledExecutor();
        
        // Mode entraînement
        executor.scheduleAtFixedRate(() -> {
            if (!isRunning.get()) {
                executor.shutdown();
                onTrainingComplete.run();
                return;
            }

            // Obtenir l'état actuel
            String state = environment.getCurrentState();
            
            // Choisir une action
            int action = agent.selectAction(state);
            
            // Exécuter l'action et obtenir la récompense
            double reward = environment.step(action);
            
            // Obtenir le nouvel état
            String nextState = environment.getCurrentState();
            
            // Mettre à jour l'agent
            agent.update(state, action, reward, nextState);
            
            // Diminuer epsilon progressivement
            agent.decreaseEpsilon();
            
            // Mettre à jour l'interface
            episodeCount++;
            if (callback != null) {
                callback.onUpdate(reward, agent.getEpsilon(), episodeCount);
            }
            
            // Arrêter après 1000 épisodes
            if (episodeCount >= 1000) {
                isRunning.set(false);
            }
            
        }, 0, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void startDetection() {
        if (isRunning.get()) return;
        
        isRunning.set(true);
        executor = Executors.newSingleThreadScheduledExecutor();
        
        // Mode détection
        executor.scheduleAtFixedRate(() -> {
            if (!isRunning.get()) {
                executor.shutdown();
                return;
            }

            // Obtenir l'état actuel
            String state = environment.getCurrentState();
            
            // Choisir la meilleure action (pas d'exploration)
            int action = agent.getBestAction(state);
            
            // Exécuter l'action et obtenir la récompense
            double reward = environment.step(action);
            
            // Mettre à jour l'interface
            if (callback != null) {
                callback.onUpdate(reward, 0.0, episodeCount);
            }
            
        }, 0, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        isRunning.set(false);
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public RLEnvironment getEnvironment() {
        return environment;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void updateParameters(double learningRate, double discountFactor, double epsilon) {
        agent = new RLAgent(learningRate, discountFactor, epsilon);
    }

    public RLAgent getAgent() {
        return agent;
    }
} 