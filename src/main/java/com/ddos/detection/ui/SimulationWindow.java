package com.ddos.detection.ui;

import javax.swing.*;
import java.awt.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.plot.PlotOrientation;
import com.ddos.detection.rl.RLEnvironment;
import com.ddos.detection.rl.RLAgent;

public class SimulationWindow extends JFrame {
    private RLEnvironment environment;
    private RLAgent agent;
    private ChartPanel trafficChart;
    private XYSeries trafficSeries;
    private XYSeries thresholdSeries;
    private JLabel statusLabel;
    private JButton startButton;
    private JButton stopButton;
    private Timer simulationTimer;
    private boolean isDetectionMode;
    private int timeStep;

    public SimulationWindow(RLAgent agent, boolean isDetectionMode) {
        this.agent = agent;
        this.isDetectionMode = isDetectionMode;
        this.environment = new RLEnvironment(2000.0);
        this.timeStep = 0;

        setTitle(isDetectionMode ? "Simulation avec Détection" : "Simulation sans Détection");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        createChartPanel();
        createControlPanel();
        createStatusPanel();

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void createChartPanel() {
        trafficSeries = new XYSeries("Trafic Réseau");
        thresholdSeries = new XYSeries("Seuil");
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(trafficSeries);
        dataset.addSeries(thresholdSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Trafic Réseau en Temps Réel",
            "Temps (s)",
            "Paquets/s",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        trafficChart = new ChartPanel(chart);
        trafficChart.setPreferredSize(new Dimension(700, 400));
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
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("En attente de démarrage...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        statusPanel.setBorder(BorderFactory.createTitledBorder("État"));
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.NORTH);
    }

    private void startSimulation() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        trafficSeries.clear();
        thresholdSeries.clear();
        timeStep = 0;

        simulationTimer = new Timer(100, e -> {
            // Mise à jour de la simulation
            timeStep++;
            
            if (isDetectionMode) {
                // Mode avec détection
                String state = environment.getCurrentState();
                int action = agent.getBestAction(state);
                double reward = environment.step(action);
                
                // Mise à jour de l'interface
                updateDetectionUI(action);
            } else {
                // Mode sans détection
                environment.step(0);
            }

            // Mise à jour du graphique
            double traffic = environment.getCurrentTraffic();
            trafficSeries.add(timeStep, traffic);
            thresholdSeries.add(timeStep, environment.getBaselineTraffic() * 2.0);

            // Limiter les points affichés
            if (timeStep > 100) {
                trafficSeries.remove(0);
                thresholdSeries.remove(0);
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
        statusLabel.setText("Simulation arrêtée");
    }

    private void updateDetectionUI(int action) {
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
                status = "État: Critique";
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