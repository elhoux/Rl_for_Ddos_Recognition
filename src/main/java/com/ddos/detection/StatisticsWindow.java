package com.ddos.detection;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.block.BlockBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StatisticsWindow extends JFrame {
    private ChartPanel chartPanel;
    private XYSeries trafficSeries;
    private XYSeries baselineSeries;
    private XYSeries anomalySeries;
    private XYSeriesCollection dataset;
    private JFreeChart chart;
    private JLabel currentTrafficLabel;
    private JLabel zScoreLabel;
    private JLabel confidenceLabel;
    private JLabel statusLabel;
    private JTextArea alertsArea;
    private JPanel alertCounter;
    private JPanel attackCounter;
    private JPanel fpCounter;
    private JTable statsTable;
    private long startTime;

    public StatisticsWindow(String title) {
        super(title);
        startTime = System.currentTimeMillis();
        initializeComponents();
        setupWindow();
    }

    private void initializeComponents() {
        // Création des séries
        trafficSeries = new XYSeries("Trafic actuel");
        baselineSeries = new XYSeries("Baseline");
        anomalySeries = new XYSeries("Anomalies");
        dataset = new XYSeriesCollection();
        dataset.addSeries(trafficSeries);
        dataset.addSeries(baselineSeries);
        dataset.addSeries(anomalySeries);

        // Création du graphique
        chart = ChartFactory.createXYLineChart(
            "Analyse Statistique du Trafic",
            "Temps (secondes)",
            "Paquets/s",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        // Configuration du graphique
        customizeChart(chart);
        
        // Panel du graphique
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));

        // Panel des métriques
        JPanel metricsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        metricsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Labels des métriques
        currentTrafficLabel = new JLabel("0 pkt/s", SwingConstants.CENTER);
        zScoreLabel = new JLabel("0.00", SwingConstants.CENTER);
        confidenceLabel = new JLabel("0%", SwingConstants.CENTER);
        statusLabel = new JLabel("Normal", SwingConstants.CENTER);

        // Ajout des métriques
        metricsPanel.add(createMetricPanel("Trafic Actuel", currentTrafficLabel));
        metricsPanel.add(createMetricPanel("Score Z", zScoreLabel));
        metricsPanel.add(createMetricPanel("Confiance", confidenceLabel));
        metricsPanel.add(createMetricPanel("État", statusLabel));

        // Panel des alertes
        JPanel alertsPanel = new JPanel(new BorderLayout(5, 5));
        alertsPanel.setBorder(BorderFactory.createTitledBorder("Alertes"));

        // Compteurs
        JPanel countersPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        alertCounter = createCounter("Alertes", "0", new Color(255, 165, 0));
        attackCounter = createCounter("Attaques", "0", Color.RED);
        fpCounter = createCounter("Faux +", "0", Color.GRAY);
        
        countersPanel.add(alertCounter);
        countersPanel.add(attackCounter);
        countersPanel.add(fpCounter);

        // Zone de texte des alertes
        alertsArea = new JTextArea();
        alertsArea.setEditable(false);
        alertsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane alertsScroll = new JScrollPane(alertsArea);
        alertsScroll.setPreferredSize(new Dimension(300, 200));

        alertsPanel.add(countersPanel, BorderLayout.NORTH);
        alertsPanel.add(alertsScroll, BorderLayout.CENTER);

        // Organisation du layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(metricsPanel, BorderLayout.NORTH);
        mainPanel.add(alertsPanel, BorderLayout.EAST);

        add(mainPanel);
    }

    private void customizeChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        // Trafic actuel : ligne bleue épaisse
        renderer.setSeriesPaint(0, new Color(0, 0, 220));
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, true);
        
        // Baseline : ligne verte pointillée
        renderer.setSeriesPaint(1, new Color(0, 150, 0));
        renderer.setSeriesStroke(1, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                                                  1.0f, new float[]{6.0f, 3.0f}, 0.0f));
        renderer.setSeriesShapesVisible(1, false);
        
        // Anomalies : points rouges
        renderer.setSeriesPaint(2, Color.RED);
        renderer.setSeriesStroke(2, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(2, true);
        renderer.setSeriesShape(2, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));

        plot.setRenderer(renderer);
        
        // Ajouter une légende
        chart.getLegend().setFrame(BlockBorder.NONE);
        chart.getLegend().setBackgroundPaint(Color.WHITE);
        
        // Configurer les axes
        plot.getDomainAxis().setLabel("Temps (secondes)");
        plot.getRangeAxis().setLabel("Paquets/s");
    }

    private JPanel createMetricPanel(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCounter(String title, String initialValue, Color color) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(color);
        
        JLabel valueLabel = new JLabel(initialValue, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 16));
        valueLabel.setForeground(color);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private void setupWindow() {
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void updateStatistics(StatisticalManager.AnalysisResult result) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Mise à jour des métriques principales
                currentTrafficLabel.setText(String.format("%.0f pkt/s", result.currentValue));
                
                // Calcul du score Z (simplifié)
                double zScore = result.currentValue / 1000.0; // Normalisation simple
                zScoreLabel.setText(String.format("%.2f", zScore));
                
                // Mise à jour de la confiance
                confidenceLabel.setText(String.format("%.1f%%", result.confidence * 100));

                // Mise à jour du statut avec code couleur
                if (result.isAnomaly) {
                    if (result.confidence > 0.8) {
                        statusLabel.setText("ATTAQUE DÉTECTÉE");
                        statusLabel.setForeground(new Color(180, 0, 0));
                    } else {
                        statusLabel.setText("ALERTE");
                        statusLabel.setForeground(new Color(200, 100, 0));
                    }
                    addAlert(result);
                } else {
                    statusLabel.setText("Normal");
                    statusLabel.setForeground(new Color(0, 150, 0));
                }

                // Mise à jour du graphique
                double timeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
                
                // Ajouter le point au graphique principal
                trafficSeries.add(timeInSeconds, result.currentValue);
                
                // Ajouter la baseline (moyenne mobile)
                baselineSeries.add(timeInSeconds, result.currentValue * 0.8); // 80% du trafic actuel comme baseline
                
                // Ajouter le point à la série d'anomalies si nécessaire
                if (result.isAnomaly) {
                    anomalySeries.add(timeInSeconds, result.currentValue);
                }
                
                // Limiter le nombre de points affichés pour éviter la surcharge
                while (trafficSeries.getItemCount() > 100) {
                    trafficSeries.remove(0);
                }
                while (baselineSeries.getItemCount() > 100) {
                    baselineSeries.remove(0);
                }
                while (anomalySeries.getItemCount() > 100) {
                    anomalySeries.remove(0);
                }

                // Forcer le rafraîchissement du graphique
                chartPanel.repaint();
                
                System.out.println("[DEBUG] Mise à jour des statistiques - Trafic: " + result.currentValue + 
                                 ", Confiance: " + result.confidence + 
                                 ", Anomalie: " + result.isAnomaly);
                
            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour des statistiques: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void addAlert(StatisticalManager.AnalysisResult result) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String severity;
        String colorCode;
        
        if (result.anomalyType.contains("Faux Positif")) {
            severity = "FAUX POSITIF";
            colorCode = "\u001B[33m"; // Orange
        } else if (result.confidence > 0.8) {
            severity = "CRITIQUE";
            colorCode = "\u001B[31m"; // Rouge
        } else if (result.confidence > 0.5) {
            severity = "ALERTE";
            colorCode = "\u001B[35m"; // Violet
        } else {
            severity = "INFO";
            colorCode = "\u001B[90m"; // Gris
        }
        
        String alertText = String.format("%s [%s] %s%n" +
                                       "   ├─ Confiance: %.1f%%%n" +
                                       "   ├─ Trafic: %.0f pkt/s%n" +
                                       "   └─ Type: %s%n",
            timestamp, severity, result.anomalyType,
            result.confidence * 100,
            result.currentValue,
            result.isAnomaly ? 
                (result.anomalyType.contains("Faux Positif") ? "Trafic légitime élevé" : "Possible attaque DDoS") : 
                "Trafic normal");
        
        alertsArea.append(alertText);
        alertsArea.setCaretPosition(alertsArea.getDocument().getLength());
        
        // Mise à jour des compteurs avec distinction des faux positifs
        updateCounters(result);
    }

    private void updateCounters(StatisticalManager.AnalysisResult result) {
        JLabel counterLabel;
        
        if (result.anomalyType.contains("Faux Positif")) {
            // Incrémenter le compteur de faux positifs
            counterLabel = (JLabel) fpCounter.getComponent(1);
            counterLabel.setText(String.valueOf(Integer.parseInt(counterLabel.getText()) + 1));
            // Animation spéciale pour les faux positifs
            animateCounter(fpCounter, new Color(255, 165, 0)); // Orange
        } else if (result.confidence > 0.8) {
            // Attaque réelle
            counterLabel = (JLabel) attackCounter.getComponent(1);
            counterLabel.setText(String.valueOf(Integer.parseInt(counterLabel.getText()) + 1));
            animateCounter(attackCounter, Color.RED);
        } else if (result.confidence > 0.5) {
            // Alerte normale
            counterLabel = (JLabel) alertCounter.getComponent(1);
            counterLabel.setText(String.valueOf(Integer.parseInt(counterLabel.getText()) + 1));
            animateCounter(alertCounter, new Color(255, 140, 0));
        }
    }

    private void animateCounter(JPanel counter, Color highlightColor) {
        Timer timer = new Timer(100, null);
        timer.addActionListener(new ActionListener() {
            private int count = 0;
            private final Color originalColor = counter.getBackground();
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (count < 6) { // 3 clignotements
                    counter.setBackground(count % 2 == 0 ? highlightColor : originalColor);
                    count++;
                } else {
                    counter.setBackground(originalColor);
                    ((Timer)e.getSource()).stop();
                }
            }
        });
        timer.start();
    }

    public void clearData() {
        trafficSeries.clear();
        baselineSeries.clear();
        anomalySeries.clear();
        alertsArea.setText("");
        currentTrafficLabel.setText("0 pkt/s");
        zScoreLabel.setText("0.00");
        confidenceLabel.setText("0%");
        statusLabel.setText("Normal");
        statusLabel.setForeground(Color.BLACK);
        
        // Réinitialiser les compteurs
        ((JLabel) alertCounter.getComponent(1)).setText("0");
        ((JLabel) attackCounter.getComponent(1)).setText("0");
        ((JLabel) fpCounter.getComponent(1)).setText("0");
        
        // Réinitialiser le temps de début
        startTime = System.currentTimeMillis();
        
        chartPanel.repaint();
    }
} 