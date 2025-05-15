package com.ddos.detection.rl;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RLAgent {
    private Map<String, double[]> qTable;
    private double learningRate;
    private double discountFactor;
    private double epsilon;
    private Random random;
    private static final int NUM_ACTIONS = 3; // Normal, Alert, Critical

    public RLAgent(double learningRate, double discountFactor, double epsilon) {
        this.qTable = new HashMap<>();
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.epsilon = epsilon;
        this.random = new Random();
    }

    public int selectAction(String state) {
        // Exploration vs Exploitation
        if (random.nextDouble() < epsilon) {
            return random.nextInt(NUM_ACTIONS);
        }
        
        double[] qValues = getQValues(state);
        return getMaxActionIndex(qValues);
    }

    public void update(String state, int action, double reward, String nextState) {
        double[] qValues = getQValues(state);
        double[] nextQValues = getQValues(nextState);
        
        // Q-Learning update formula
        double maxNextQ = getMaxQValue(nextQValues);
        double currentQ = qValues[action];
        double newQ = currentQ + learningRate * (reward + discountFactor * maxNextQ - currentQ);
        
        qValues[action] = newQ;
        qTable.put(state, qValues);
    }

    private double[] getQValues(String state) {
        return qTable.computeIfAbsent(state, k -> new double[NUM_ACTIONS]);
    }

    private int getMaxActionIndex(double[] qValues) {
        int maxIndex = 0;
        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > qValues[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private double getMaxQValue(double[] qValues) {
        double maxValue = qValues[0];
        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > maxValue) {
                maxValue = qValues[i];
            }
        }
        return maxValue;
    }

    public void decreaseEpsilon() {
        // Décroissance plus lente au début, plus rapide vers la fin
        epsilon = Math.max(0.05, epsilon * 0.998);
    }

    public double getEpsilon() {
        return epsilon;
    }

    public int getBestAction(String state) {
        double[] qValues = qTable.computeIfAbsent(state, k -> new double[NUM_ACTIONS]);
        int bestAction = 0;
        double bestValue = qValues[0];
        
        // Trouver l'action avec la plus grande valeur Q
        for (int i = 1; i < NUM_ACTIONS; i++) {
            if (qValues[i] > bestValue) {
                bestValue = qValues[i];
                bestAction = i;
            }
        }
        
        return bestAction;
    }

    public Map<String, double[]> getQTable() {
        return qTable;
    }

    public void setQTable(Map<String, double[]> qTable) {
        this.qTable = qTable;
    }
} 