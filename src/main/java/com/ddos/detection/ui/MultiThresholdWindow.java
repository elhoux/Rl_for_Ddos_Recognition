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
import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Random;
import javax.swing.border.TitledBorder;

public class MultiThresholdWindow extends JFrame {
    private XYSeries trafficSeries;
    private XYSeries normalThresholdSeries;
    private XYSeries warningThresholdSeries;
    private XYSeries criticalThresholdSeries;
    private ChartPanel chartPanel;
    private JLabel statusLabel;
    private JLabel trafficLabel;
    private Timer simulationTimer;
    private long startTime;
    private boolean isDetectionMode;
    private double baseTraffic = 2000.0;
    private double learningRate = 0.1;

    public MultiThresholdWindow(boolean isDetectionMode) {
        this.isDetectionMode = isDetectionMode;
        setTitle("Analyse Multi-Seuils en Temps Réel");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initializeComponents();
        setupLayout();
        startSimulation();
    }

    private void initializeComponents() {
        // Initialisation des séries
        trafficSeries = new XYSeries("Trafic");
        normalThresholdSeries = new XYSeries("Seuil Normal");
        warningThresholdSeries = new XYSeries("Seuil d'Alerte");
        criticalThresholdSeries = new XYSeries("Seuil Critique");

        // Configuration du graphique
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(trafficSeries);
        dataset.addSeries(normalThresholdSeries);
        dataset.addSeries(warningThresholdSeries);
        dataset.addSeries(criticalThresholdSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Analyse Multi-Seuils du Trafic",
            "Temps (secondes)",
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
        renderer.setSeriesPaint(1, Color.GREEN);     // Seuil normal
        renderer.setSeriesPaint(2, Color.ORANGE);    // Seuil d'alerte
        renderer.setSeriesPaint(3, Color.RED);       // Seuil critique
        
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesStroke(1, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10.0f, 10.0f}, 0));
        renderer.setSeriesStroke(2, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10.0f, 10.0f}, 0));
        renderer.setSeriesStroke(3, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10.0f, 10.0f}, 0));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(900, 400));

        // Labels d'information
        statusLabel = new JLabel("État: Normal", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        trafficLabel = new JLabel("Trafic: 0 pkt/s", SwingConstants.CENTER);
        trafficLabel.setFont(new Font("Arial", Font.PLAIN, 12));
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);

        // Panel d'information
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new TitledBorder("Informations"));
        infoPanel.add(statusLabel);
        infoPanel.add(trafficLabel);

        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void startSimulation() {
        startTime = System.currentTimeMillis();
        Random random = new Random();

        // Configuration des seuils initiaux avec des facteurs d'apprentissage différents
        final double[] thresholds = {
            baseTraffic * 1.5,  // normalThreshold
            baseTraffic * 2.5,  // warningThreshold
            baseTraffic * 3.5   // criticalThreshold
        };
        
        final double[] learningRates = {
            0.15,  // Taux d'apprentissage pour seuil normal (plus réactif)
            0.10,  // Taux d'apprentissage pour seuil d'alerte (modéré)
            0.05   // Taux d'apprentissage pour seuil critique (plus stable)
        };

        simulationTimer = new Timer(100, e -> {
            double timeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
            
            // Génération du trafic
            double noise = random.nextGaussian() * 500;
            double currentTraffic;
            
            if (isDetectionMode && timeInSeconds > 5) {
                // Mode détection: augmentation progressive avec variations
                double attackIntensity = Math.min(3.0, 1.0 + (timeInSeconds - 5) * 0.1);
                double variationFactor = 1.0 + 0.2 * Math.sin(timeInSeconds * 0.5);
                currentTraffic = baseTraffic * attackIntensity * variationFactor + noise;
            } else {
                // Mode normal: variations naturelles
                currentTraffic = baseTraffic + noise + Math.sin(timeInSeconds) * 300;
            }

            // Mise à jour des seuils dynamiques avec des comportements différents
            if (currentTraffic < thresholds[0]) {
                // Adaptation du trafic de base
                baseTraffic = (1 - learningRates[0]) * baseTraffic + learningRates[0] * currentTraffic;
                
                // Adaptation des seuils avec des facteurs différents
                thresholds[0] = baseTraffic * (1.5 + 0.1 * Math.sin(timeInSeconds * 0.3));  // Plus variable
                thresholds[1] = baseTraffic * (2.5 + 0.15 * Math.cos(timeInSeconds * 0.2)); // Variation moyenne
                thresholds[2] = baseTraffic * (3.5 + 0.05 * Math.sin(timeInSeconds * 0.1)); // Plus stable
            } else {
                // Adaptation plus lente en cas de dépassement
                for (int i = 0; i < thresholds.length; i++) {
                    if (currentTraffic > thresholds[i]) {
                        thresholds[i] += learningRates[i] * (currentTraffic - thresholds[i]);
                    }
                }
            }

            // Mise à jour des séries
            trafficSeries.add(timeInSeconds, currentTraffic);
            normalThresholdSeries.add(timeInSeconds, thresholds[0]);
            warningThresholdSeries.add(timeInSeconds, thresholds[1]);
            criticalThresholdSeries.add(timeInSeconds, thresholds[2]);

            // Nettoyage des anciennes données (garder 30 secondes d'historique)
            while (trafficSeries.getItemCount() > 0 && 
                   timeInSeconds - trafficSeries.getX(0).doubleValue() > 30) {
                trafficSeries.remove(0);
                normalThresholdSeries.remove(0);
                warningThresholdSeries.remove(0);
                criticalThresholdSeries.remove(0);
            }

            // Mise à jour des labels
            trafficLabel.setText(String.format("Trafic: %.0f pkt/s", currentTraffic));
            
            if (currentTraffic > thresholds[2]) {
                statusLabel.setText("État: CRITIQUE");
                statusLabel.setForeground(Color.RED);
            } else if (currentTraffic > thresholds[1]) {
                statusLabel.setText("État: ALERTE");
                statusLabel.setForeground(Color.ORANGE);
            } else if (currentTraffic > thresholds[0]) {
                statusLabel.setText("État: ATTENTION");
                statusLabel.setForeground(Color.YELLOW);
            } else {
                statusLabel.setText("État: Normal");
                statusLabel.setForeground(new Color(0, 150, 0));
            }
        });

        simulationTimer.start();
    }

    public void stopSimulation() {
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
        }
    }

    @Override
    public void dispose() {
        stopSimulation();
        super.dispose();
    }
} 