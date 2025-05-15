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
import java.text.SimpleDateFormat;
import java.util.Date;

public class MultiThresholdWindow extends JFrame {
    private ChartPanel chartPanel;
    private XYSeries trafficSeries;
    private XYSeries normalThresholdSeries;
    private XYSeries warningThresholdSeries;
    private XYSeries criticalThresholdSeries;
    private XYSeriesCollection dataset;
    private JFreeChart chart;
    
    // Indicateurs
    private JLabel currentTrafficLabel;
    private JLabel normalThresholdLabel;
    private JLabel warningThresholdLabel;
    private JLabel criticalThresholdLabel;
    private JLabel statusLabel;
    
    // Compteurs
    private JPanel normalCounter;
    private JPanel warningCounter;
    private JPanel criticalCounter;
    
    // Seuils dynamiques
    private double baselineTraffic = 2000.0;  // Trafic de base initial
    private double normalThreshold = 3000.0;
    private double warningThreshold = 5000.0;
    private double criticalThreshold = 7000.0;
    private double alpha = 0.1;  // Facteur d'apprentissage
    private double[] trafficHistory = new double[100];  // Historique pour la moyenne mobile
    private int historyIndex = 0;
    
    private long startTime;

    public MultiThresholdWindow() {
        super("Analyse Multi-Seuils");
        startTime = System.currentTimeMillis();
        initializeComponents();
        setupWindow();
    }

    private void initializeComponents() {
        // Création des séries
        trafficSeries = new XYSeries("Trafic actuel");
        normalThresholdSeries = new XYSeries("Seuil normal");
        warningThresholdSeries = new XYSeries("Seuil d'alerte");
        criticalThresholdSeries = new XYSeries("Seuil critique");
        
        dataset = new XYSeriesCollection();
        dataset.addSeries(trafficSeries);
        dataset.addSeries(normalThresholdSeries);
        dataset.addSeries(warningThresholdSeries);
        dataset.addSeries(criticalThresholdSeries);

        // Création du graphique
        chart = ChartFactory.createXYLineChart(
            "Analyse Multi-Seuils du Trafic",
            "Temps (secondes)",
            "Paquets/s",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        customizeChart(chart);
        
        // Panel du graphique
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));

        // Panel des indicateurs
        JPanel indicatorsPanel = createIndicatorsPanel();
        
        // Panel des compteurs
        JPanel countersPanel = createCountersPanel();

        // Organisation du layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(indicatorsPanel, BorderLayout.NORTH);
        mainPanel.add(countersPanel, BorderLayout.EAST);

        add(mainPanel);
        
        // Initialiser les seuils sur le graphique
        initializeThresholds();
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
        
        // Seuil normal : ligne verte
        renderer.setSeriesPaint(1, new Color(0, 150, 0));
        renderer.setSeriesStroke(1, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                                                  1.0f, new float[]{10.0f, 5.0f}, 0.0f));
        
        // Seuil d'alerte : ligne orange
        renderer.setSeriesPaint(2, new Color(255, 165, 0));
        renderer.setSeriesStroke(2, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                                                  1.0f, new float[]{10.0f, 5.0f}, 0.0f));
        
        // Seuil critique : ligne rouge
        renderer.setSeriesPaint(3, new Color(220, 0, 0));
        renderer.setSeriesStroke(3, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                                                  1.0f, new float[]{10.0f, 5.0f}, 0.0f));

        plot.setRenderer(renderer);
        
        chart.getLegend().setFrame(BlockBorder.NONE);
        chart.getLegend().setBackgroundPaint(Color.WHITE);
    }

    private JPanel createIndicatorsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Indicateurs de trafic"));
        
        // Création des labels
        currentTrafficLabel = createIndicatorLabel("Trafic actuel", "0 pkt/s", Color.BLUE);
        normalThresholdLabel = createIndicatorLabel("Seuil normal", normalThreshold + " pkt/s", new Color(0, 150, 0));
        warningThresholdLabel = createIndicatorLabel("Seuil d'alerte", warningThreshold + " pkt/s", new Color(255, 165, 0));
        criticalThresholdLabel = createIndicatorLabel("Seuil critique", criticalThreshold + " pkt/s", Color.RED);
        statusLabel = createIndicatorLabel("État", "Normal", Color.BLACK);
        
        // Ajout des labels
        panel.add(currentTrafficLabel);
        panel.add(normalThresholdLabel);
        panel.add(warningThresholdLabel);
        panel.add(criticalThresholdLabel);
        panel.add(statusLabel);
        
        return panel;
    }

    private JLabel createIndicatorLabel(String title, String value, Color color) {
        JLabel label = new JLabel(String.format("<html><b>%s</b><br>%s</html>", title, value));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(color);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return label;
    }

    private JPanel createCountersPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Compteurs d'événements"));
        
        normalCounter = createCounter("Trafic normal", "0", new Color(0, 150, 0));
        warningCounter = createCounter("Alertes", "0", new Color(255, 165, 0));
        criticalCounter = createCounter("Critiques", "0", Color.RED);
        
        panel.add(normalCounter);
        panel.add(warningCounter);
        panel.add(criticalCounter);
        
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

    private void initializeThresholds() {
        // Récupérer la durée depuis le spinner de MainFrame
        double duration = 30.0; // Durée par défaut en secondes
        
        // Ajouter les lignes de seuil sur toute la durée
        for (double t = 0; t <= duration; t += 0.1) {  // Points plus rapprochés pour une ligne plus lisse
            normalThresholdSeries.add(t, normalThreshold);
            warningThresholdSeries.add(t, warningThreshold);
            criticalThresholdSeries.add(t, criticalThreshold);
        }
        
        // Configurer l'échelle du graphique
        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setRange(0, duration);  // Axe X de 0 à la durée
        plot.getRangeAxis().setRange(0, criticalThreshold * 1.2);  // Axe Y avec marge de 20%
    }

    private void setupWindow() {
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void updateDynamicThresholds(double currentValue) {
        // Mise à jour de l'historique
        trafficHistory[historyIndex] = currentValue;
        historyIndex = (historyIndex + 1) % trafficHistory.length;

        // Calcul de la moyenne mobile
        double sum = 0;
        int count = 0;
        for (double value : trafficHistory) {
            if (value > 0) {
                sum += value;
                count++;
            }
        }
        if (count > 0) {
            double movingAverage = sum / count;
            
            // Mise à jour du trafic de base avec lissage exponentiel
            baselineTraffic = (1 - alpha) * baselineTraffic + alpha * movingAverage;
            
            // Ajustement des seuils en fonction du trafic de base
            normalThreshold = baselineTraffic * 1.5;
            warningThreshold = baselineTraffic * 2.5;
            criticalThreshold = baselineTraffic * 3.5;
            
            // Log des ajustements
            System.out.println(String.format(
                "Seuils ajustés - Base: %.0f, Normal: %.0f, Alerte: %.0f, Critique: %.0f",
                baselineTraffic, normalThreshold, warningThreshold, criticalThreshold
            ));
        }
    }

    public void updateTraffic(double currentValue) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Mise à jour des seuils dynamiques
                updateDynamicThresholds(currentValue);
                
                // Mise à jour du trafic actuel
                currentTrafficLabel.setText(String.format("<html><b>Trafic actuel</b><br>%.0f pkt/s</html>", currentValue));
                
                // Mise à jour des labels de seuils
                normalThresholdLabel.setText(String.format("<html><b>Seuil normal</b><br>%.1f pkt/s</html>", normalThreshold));
                warningThresholdLabel.setText(String.format("<html><b>Seuil d'alerte</b><br>%.1f pkt/s</html>", warningThreshold));
                criticalThresholdLabel.setText(String.format("<html><b>Seuil critique</b><br>%.1f pkt/s</html>", criticalThreshold));
                
                // Calcul du temps écoulé
                double timeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
                
                // Ajout des points au graphique
                trafficSeries.add(timeInSeconds, currentValue);
                normalThresholdSeries.add(timeInSeconds, normalThreshold);
                warningThresholdSeries.add(timeInSeconds, warningThreshold);
                criticalThresholdSeries.add(timeInSeconds, criticalThreshold);
                
                // Nettoyage des anciens points
                while (trafficSeries.getItemCount() > 1000) {
                    trafficSeries.remove(0);
                    normalThresholdSeries.remove(0);
                    warningThresholdSeries.remove(0);
                    criticalThresholdSeries.remove(0);
                }
                
                // Mise à jour du statut
                String status;
                Color statusColor;
                
                if (currentValue > criticalThreshold) {
                    status = "CRITIQUE";
                    statusColor = Color.RED;
                    incrementCounter(criticalCounter);
                } else if (currentValue > warningThreshold) {
                    status = "ALERTE";
                    statusColor = new Color(255, 165, 0);
                    incrementCounter(warningCounter);
                } else if (currentValue > normalThreshold) {
                    status = "ATTENTION";
                    statusColor = Color.YELLOW;
                    incrementCounter(warningCounter);
                } else {
                    status = "Normal";
                    statusColor = new Color(0, 150, 0);
                    incrementCounter(normalCounter);
                }
                
                statusLabel.setText(String.format("<html><b>État</b><br>%s</html>", status));
                statusLabel.setForeground(statusColor);
                
                // Forcer le rafraîchissement du graphique
                chartPanel.repaint();
                
            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour du trafic: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void incrementCounter(JPanel counter) {
        JLabel valueLabel = (JLabel) counter.getComponent(1);
        int currentValue = Integer.parseInt(valueLabel.getText());
        valueLabel.setText(String.valueOf(currentValue + 1));
        animateCounter(counter);
    }

    private void animateCounter(JPanel counter) {
        Timer timer = new Timer(100, null);
        timer.addActionListener(e -> {
            Color originalColor = counter.getBackground();
            counter.setBackground(new Color(255, 255, 200));
            
            Timer resetTimer = new Timer(200, e2 -> {
                counter.setBackground(originalColor);
                ((Timer)e2.getSource()).stop();
            });
            resetTimer.setRepeats(false);
            resetTimer.start();
            
            timer.stop();
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void clearData() {
        trafficSeries.clear();
        currentTrafficLabel.setText("<html><b>Trafic actuel</b><br>0 pkt/s</html>");
        statusLabel.setText("<html><b>État</b><br>Normal</html>");
        statusLabel.setForeground(Color.BLACK);
        
        // Réinitialiser les compteurs
        ((JLabel)normalCounter.getComponent(1)).setText("0");
        ((JLabel)warningCounter.getComponent(1)).setText("0");
        ((JLabel)criticalCounter.getComponent(1)).setText("0");
        
        // Réinitialiser le temps de début
        startTime = System.currentTimeMillis();
        
        // Réinitialiser les seuils
        initializeThresholds();
        
        chartPanel.repaint();
    }
} 