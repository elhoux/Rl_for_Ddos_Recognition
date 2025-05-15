package com.ddos.detection;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;

public class GraphWindow extends JFrame {
    private ChartPanel chartPanel;
    private XYSeries detectSeries;
    private XYSeries normalSeries;
    private XYSeriesCollection dataset;
    private JFreeChart chart;
    private JLabel statusLabel;
    private JLabel valueLabel;
    private long startTime;

    public GraphWindow(String title) {
        super(title);
        startTime = System.currentTimeMillis();
        initializeComponents();
        setupWindow();
    }

    private void initializeComponents() {
        // Création des séries de données
        detectSeries = new XYSeries("Mode Détection");
        normalSeries = new XYSeries("Mode Normal");
        dataset = new XYSeriesCollection();
        dataset.addSeries(detectSeries);
        dataset.addSeries(normalSeries);

        // Création du graphique
        chart = ChartFactory.createXYLineChart(
            "Analyse en Temps Réel",
            "Temps (secondes)",
            "Valeur",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        // Configuration du graphique
        customizeChart();
        
        // Panel du graphique
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));

        // Panel des métriques
        JPanel metricsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        metricsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Labels des métriques
        valueLabel = new JLabel("Valeur: 0.000", SwingConstants.CENTER);
        statusLabel = new JLabel("Statut: Normal", SwingConstants.CENTER);
        
        valueLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));

        metricsPanel.add(valueLabel);
        metricsPanel.add(statusLabel);

        // Organisation du layout
        setLayout(new BorderLayout(10, 10));
        add(chartPanel, BorderLayout.CENTER);
        add(metricsPanel, BorderLayout.SOUTH);
    }

    private void customizeChart() {
        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);     // Mode détection en rouge
        renderer.setSeriesPaint(1, Color.BLUE);    // Mode normal en bleu
        
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));

        plot.setRenderer(renderer);
    }

    private void setupWindow() {
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void updateData(double timeInSeconds, double value, boolean isDetectMode, boolean isAttack) {
        SwingUtilities.invokeLater(() -> {
            // Mise à jour du graphique
            if (isDetectMode) {
                detectSeries.add(timeInSeconds, value);
            } else {
                normalSeries.add(timeInSeconds, value);
            }

            // Mise à jour des labels
            valueLabel.setText(String.format("Valeur: %.3f", value));
            
            if (isAttack) {
                statusLabel.setText("ALERTE: ATTAQUE DÉTECTÉE!");
                statusLabel.setForeground(Color.RED);
            } else {
                statusLabel.setText("Statut: Normal");
                statusLabel.setForeground(new Color(0, 150, 0));
            }

            // Forcer le rafraîchissement
            chartPanel.repaint();
        });
    }

    public void clearData() {
        detectSeries.clear();
        normalSeries.clear();
        valueLabel.setText("Valeur: 0.000");
        statusLabel.setText("Statut: Normal");
        statusLabel.setForeground(Color.BLACK);
        startTime = System.currentTimeMillis();
        chartPanel.repaint();
    }
} 