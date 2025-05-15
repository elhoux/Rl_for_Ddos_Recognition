package com.ddos.detection.ui;

import javax.swing.*;
import java.awt.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import com.ddos.detection.rl.RLAgent;
import com.ddos.detection.rl.RLEnvironment;

public class RLSimulationWindow extends JFrame {
    private RLAgent agent;
    private RLEnvironment environment;
    private ChartPanel trafficChart;
    private XYSeries trafficSeries;
    private XYSeries thresholdSeries;
    private XYSeries rewardSeries;
    private JLabel statusLabel;
    private JLabel trafficLabel;
    private JLabel rewardLabel;
    private JLabel epsilonLabel;
    private JButton startButton;
    private JButton stopButton;
    private Timer simulationTimer;
    private boolean isDetectionMode;
    private int timeStep;
    private double currentEpsilon;

    public RLSimulationWindow(RLAgent agent, boolean isDetectionMode) {
        this.agent = agent;
        this.isDetectionMode = isDetectionMode;
        this.environment = new RLEnvironment(2000.0); // Trafic de base
        this.timeStep = 0;
        this.currentEpsilon = agent.getEpsilon();

        setTitle(isDetectionMode ? "Simulation RL - Mode Détection" : "Simulation RL - Mode Normal");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        createChartPanel();
        createControlPanel();
        createStatusPanel();

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void createChartPanel() {
        // Séries pour le graphique
        trafficSeries = new XYSeries("Trafic Réseau");
        thresholdSeries = new XYSeries("Seuil");
        rewardSeries = new XYSeries("Récompense");
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(trafficSeries);
        dataset.addSeries(thresholdSeries);
        dataset.addSeries(rewardSeries);

        // Création du graphique
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Simulation de Détection DDoS par RL",
            "Temps (s)",
            "Paquets/s",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        // Personnalisation du graphique
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        // Configuration des séries
        renderer.setSeriesPaint(0, Color.BLUE);      // Trafic
        renderer.setSeriesPaint(1, Color.RED);       // Seuil
        renderer.setSeriesPaint(2, Color.GREEN);     // Récompense
        
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesStroke(1, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1.0f, new float[]{10.0f, 6.0f}, 0.0f));
        renderer.setSeriesStroke(2, new BasicStroke(1.5f));
        
        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        trafficChart = new ChartPanel(chart);
        trafficChart.setPreferredSize(new Dimension(900, 500));
        add(trafficChart, BorderLayout.CENTER);
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout());
        startButton = new JButton("Démarrer");
        stopButton = new JButton("Arrêter");
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startSimulation());
        stopButton.addActionListener(e -> stopSimulation());

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private void createStatusPanel() {
        JPanel statusPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Labels d'information
        statusLabel = new JLabel("État: En attente", SwingConstants.LEFT);
        trafficLabel = new JLabel("Trafic: 0 pkt/s", SwingConstants.LEFT);
        rewardLabel = new JLabel("Récompense: 0.0", SwingConstants.LEFT);
        epsilonLabel = new JLabel("Epsilon: " + currentEpsilon, SwingConstants.LEFT);

        // Style des labels
        Font labelFont = new Font("Arial", Font.BOLD, 12);
        statusLabel.setFont(labelFont);
        trafficLabel.setFont(labelFont);
        rewardLabel.setFont(labelFont);
        epsilonLabel.setFont(labelFont);

        // Ajout des labels au panel
        statusPanel.add(statusLabel);
        statusPanel.add(trafficLabel);
        statusPanel.add(rewardLabel);
        statusPanel.add(epsilonLabel);

        add(statusPanel, BorderLayout.NORTH);
    }

    private void startSimulation() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        timeStep = 0;
        
        // Réinitialiser les séries
        trafficSeries.clear();
        thresholdSeries.clear();
        rewardSeries.clear();

        simulationTimer = new Timer(100, e -> {
            timeStep++;
            
            // Générer le trafic selon le mode
            double traffic;
            if (isDetectionMode && timeStep > 50) { // Attaque après 5 secondes
                // Simulation d'une attaque DDoS progressive
                double attackIntensity = Math.min(3.0, 1.0 + (timeStep - 50) * 0.02);
                traffic = environment.getBaselineTraffic() * attackIntensity * (1.0 + 0.1 * Math.sin(timeStep * 0.1));
            } else {
                // Trafic normal avec variations
                traffic = environment.getBaselineTraffic() * (1.0 + 0.2 * Math.sin(timeStep * 0.05));
            }
            
            // Mettre à jour l'environnement
            environment.setCurrentTraffic(traffic);
            String state = environment.getCurrentState();
            
            // Obtenir l'action de l'agent avec exploration epsilon-greedy
            int action;
            if (Math.random() < currentEpsilon) {
                action = (int)(Math.random() * 3); // Exploration
                currentEpsilon *= 0.999; // Décroissance d'epsilon
            } else {
                action = agent.getBestAction(state); // Exploitation
            }
            
            // Obtenir la récompense
            double reward = environment.step(action);
            
            // Mise à jour des séries
            trafficSeries.add(timeStep * 0.1, traffic);
            thresholdSeries.add(timeStep * 0.1, environment.getBaselineTraffic() * 1.5);
            rewardSeries.add(timeStep * 0.1, reward * 500); // Échelle pour la visualisation
            
            // Mise à jour des labels
            updateStatus(action, traffic, reward);
            
            // Limiter les points affichés
            if (timeStep > 200) {
                trafficSeries.remove(0);
                thresholdSeries.remove(0);
                rewardSeries.remove(0);
            }
        });

        simulationTimer.start();
    }

    private void stopSimulation() {
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("État: Simulation arrêtée");
    }

    private void updateStatus(int action, double traffic, double reward) {
        // Mise à jour des labels
        trafficLabel.setText(String.format("Trafic: %.0f pkt/s", traffic));
        rewardLabel.setText(String.format("Récompense: %.2f", reward));
        epsilonLabel.setText(String.format("Epsilon: %.3f", currentEpsilon));

        // Mise à jour du statut selon l'action
        String status;
        Color statusColor;
        switch (action) {
            case 0:
                status = "État: Normal";
                statusColor = new Color(0, 150, 0);
                break;
            case 1:
                status = "État: Alerte";
                statusColor = new Color(200, 150, 0);
                break;
            case 2:
                status = "État: Attaque Détectée";
                statusColor = new Color(200, 0, 0);
                break;
            default:
                status = "État: Inconnu";
                statusColor = Color.GRAY;
        }
        
        statusLabel.setText(status);
        statusLabel.setForeground(statusColor);
    }
} 