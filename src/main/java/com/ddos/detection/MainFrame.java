package com.ddos.detection;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import java.awt.BasicStroke;
import java.awt.Color;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.block.BlockBorder;
import com.ddos.detection.ui.RLWindow;
import com.ddos.detection.ui.SimulationWindow;
import com.ddos.detection.rl.RLAgent;
import com.ddos.detection.ui.RLSimulationWindow;
import com.ddos.detection.ui.MultiThresholdWindow;

public class MainFrame extends JFrame implements EntropySimulator.EntropyListener, FluxSimulation.FluxListener {
    private static final long serialVersionUID = 1L;
    // --- Composants principaux ---
    private JComboBox<String> detectionMethodCombo;
    private JComboBox<String> attackTypeCombo;
    private JSpinner packetsPerSecondSpinner;
    private JSpinner durationSpinner;
    private JSpinner sourceIPsSpinner;
    private JSpinner sourcePortSpinner;
    private JSpinner destPortSpinner;
    private JButton startButton, stopButton, resetButton;
    private JTextArea logArea;
    private ChartPanel chartPanel;
    private XYSeries detectSeries;
    private XYSeries nonDetectSeries;
    private XYSeriesCollection dataset;
    private JLabel valueLabel, detectionStatusLabel;
    private EntropySimulator simulator;
    private long startTime;
    private JButton detectButton, nonDetectButton;
    private JTextArea consoleArea;
    private FluxTest fluxTest;
    private ValueMarker thresholdMarker;
    private boolean isDetectMode = true;
    private JButton zoomDetectButton, zoomNonDetectButton, resetZoomButton;
    private TimeSeries detectionSeries;
    private TimeSeries nonDetectionSeries;
    private JFreeChart chart;
    private FluxSimulation fluxSimulation;
    private boolean isRunning;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JComboBox<String> modeSelector;
    private Timer updateTimer;
    private static final int UPDATE_INTERVAL = 100; // 100ms
    private static final int DETECTION_THRESHOLD = 5000; // paquets/s
    private StatisticalAnalyzer statisticalAnalyzer;
    private StatisticalManager statisticalManager;
    private JPanel statisticsPanel;
    private XYSeries anomalyScoreSeries;
    private XYSeries trafficSeries;
    private XYSeries baselineSeries;
    private XYSeries anomalySeries;
    private XYSeries thresholdSeries;
    private JTable statsTable;
    private JTextArea alertsArea;
    private JLabel currentTrafficLabel;
    private JLabel zScoreLabel;
    private JLabel confidenceLabel;
    private JLabel statusLabel;
    private JPanel alertCounter;
    private JPanel attackCounter;
    private JPanel fpCounter;
    private GraphWindow graphWindow;
    private StatisticsWindow statisticsWindow;
    private JButton rlTrainingButton;
    private JPanel mainPanel;
    private Timer simulationTimer;

    public MainFrame() {
        System.out.println("[DEBUG] MainFrame lancé !");
        
        // Configuration de base de la fenêtre
        setTitle("DDoS Detection Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Initialisation des composants AVANT leur utilisation
        initializeComponents();     // Crée les composants de base
        
        // Initialisation des simulateurs
        simulator = new EntropySimulator();
        simulator.addListener(this);
        System.out.println("[DEBUG] Simulateur d'entropie initialisé");
        
        fluxSimulation = new FluxSimulation();  // Ajout de cette ligne
        System.out.println("[DEBUG] Simulateur de flux initialisé");
        
        fluxTest = new FluxTest();
        fluxTest.addFluxListener(this);
        System.out.println("[DEBUG] Test de flux initialisé");
        
        statisticalAnalyzer = new StatisticalAnalyzer();
        statisticalAnalyzer.addListener(new StatisticalAnalyzer.StatisticalListener() {
            @Override
            public void onAnomalyDetected(StatisticalAnalyzer.AnomalyReport report) {
                SwingUtilities.invokeLater(() -> {
                    updateStatisticalAnalysis(report);
                });
            }

            @Override
            public void onAnalysisComplete() {
                SwingUtilities.invokeLater(() -> {
                    logArea.append("Analyse statistique terminée\n");
                });
            }
        });
        System.out.println("[DEBUG] Analyseur statistique initialisé");
        
        // Initialisation du gestionnaire statistique
        statisticalManager = new StatisticalManager();
        statisticalManager.addListener(new StatisticalManager.AnalysisListener() {
            @Override
            public void onAnalysisUpdate(StatisticalManager.AnalysisResult result) {
                updateStatisticalDisplay(result);
            }

            @Override
            public void onAnalysisComplete() {
                stopButton.setEnabled(true);
                startButton.setEnabled(true);
            }
        });
        
        // Configuration des autres composants
        setupChart();              // Configure le graphique
        setupZoomButtons();        // Configure les boutons de zoom
        setupLayout();            // Configure la mise en page
        setupEventListeners();    // Configure les écouteurs d'événements
        
        // Initialisation de l'interface
        String initialMethod = (String) detectionMethodCombo.getSelectedItem();
        updateInterfaceForMethod(initialMethod);
        
        System.out.println("[DEBUG] Interface initialisée avec la méthode : " + initialMethod);
    }

    public void initializeComponents() {
        // Définition des couleurs personnalisées
        Color usedFieldColor = new Color(230, 255, 230);    // Vert clair
        Color unusedFieldColor = new Color(255, 230, 230);  // Rouge clair
        Color buttonColor = new Color(240, 240, 255);       // Bleu très clair
        Font labelFont = new Font("Arial", Font.BOLD, 12);
        
        // Création des zones de texte
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        
        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        
        // Création des labels
        valueLabel = new JLabel("Valeur actuelle: 0.0");
        valueLabel.setFont(labelFont);
        
        detectionStatusLabel = new JLabel("Statut: Normal");
        detectionStatusLabel.setFont(labelFont);
        
        // Création des combos et spinners
        detectionMethodCombo = new JComboBox<>(new String[]{
            "Entropie IP", 
            "Analyse de Flux",
            "Analyse Statistique",
            "Analyse Multi-Seuils",
            "Apprentissage par Renforcement"  // Nouvelle méthode ajoutée
        });
        detectionMethodCombo.setFont(labelFont);
        detectionMethodCombo.setBackground(Color.WHITE);
        
        attackTypeCombo = new JComboBox<>(new String[]{"SYN Flood", "UDP Flood", "HTTP Flood", "Slowloris"});
        attackTypeCombo.setFont(labelFont);
        attackTypeCombo.setBackground(Color.WHITE);
        
        packetsPerSecondSpinner = createStyledSpinner(2000, 1, 10000, 100, usedFieldColor);
        durationSpinner = createStyledSpinner(15, 1, 3600, 1, usedFieldColor);
        sourceIPsSpinner = createStyledSpinner(200, 1, 1000, 10, usedFieldColor);
        sourcePortSpinner = createStyledSpinner(12345, 1, 65535, 1, unusedFieldColor);
        destPortSpinner = createStyledSpinner(80, 1, 65535, 1, unusedFieldColor);
        
        // Création des boutons principaux
        startButton = new JButton("Démarrer");
        stopButton = new JButton("Arrêter");
        resetButton = new JButton("Réinitialiser");
        detectButton = new JButton("Détection");
        nonDetectButton = new JButton("Non-détection");
        
        // Style des boutons principaux
        Color detectButtonColor = new Color(255, 200, 200);    // Rouge clair
        Color nonDetectButtonColor = new Color(200, 255, 200); // Vert clair
        
        for (JButton btn : new JButton[]{startButton, stopButton, resetButton}) {
            btn.setFont(labelFont);
            btn.setBackground(buttonColor);
            btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(buttonColor.darker()),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        }
        
        detectButton.setBackground(detectButtonColor);
        detectButton.setFont(labelFont);
        detectButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(detectButtonColor.darker()),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        
        nonDetectButton.setBackground(nonDetectButtonColor);
        nonDetectButton.setFont(labelFont);
        nonDetectButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(nonDetectButtonColor.darker()),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        
        // État initial des boutons
        stopButton.setEnabled(false);
    }

    private JSpinner createStyledSpinner(int value, int min, int max, int step, Color bgColor) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        spinner.setFont(new Font("Arial", Font.PLAIN, 12));
        JComponent editor = spinner.getEditor();
        JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
        textField.setBackground(bgColor);
        textField.setHorizontalAlignment(JTextField.CENTER);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        return spinner;
    }

    private void setupChart() {
        // Création des séries de données
        detectSeries = new XYSeries("Mode Détection (DDoS)");
        nonDetectSeries = new XYSeries("Mode Normal");
        dataset = new XYSeriesCollection();
        dataset.addSeries(detectSeries);
        dataset.addSeries(nonDetectSeries);

        // Création du graphique avec des paramètres simples
        chart = ChartFactory.createXYLineChart(
            "Analyse du Trafic Réseau",  // titre
            "Temps (secondes)",          // axe X
            "Paquets/s",                 // axe Y
            dataset,                     // données
            PlotOrientation.VERTICAL,    // orientation
            true,                        // légende
            true,                        // tooltips
            false                        // URLs
        );

        // Configuration basique du graphique
        chart.setBackgroundPaint(Color.WHITE);
        
        // Configuration du plot
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Configuration du rendu des lignes
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);     // Série de détection en rouge
        renderer.setSeriesPaint(1, Color.GREEN);   // Série normale en vert
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        plot.setRenderer(renderer);
        
        // Ajout du marqueur de seuil
        thresholdMarker = new ValueMarker(DETECTION_THRESHOLD);
        thresholdMarker.setPaint(Color.BLUE);
        thresholdMarker.setStroke(new BasicStroke(1.0f));
        thresholdMarker.setLabel("Seuil de détection");
        plot.addRangeMarker(thresholdMarker);

        // Création et configuration du panel
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);
    }

    private void setupLayout() {
        // Création du panneau principal avec marge
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);

        // Panel de configuration en haut
        JPanel configPanel = createConfigPanel();
        mainPanel.add(configPanel, BorderLayout.NORTH);

        // Panel central avec split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.33); // 1/3 pour le panneau de gauche

        // Panel de gauche (logs)
        JPanel leftPanel = createLeftPanel();
        splitPane.setLeftComponent(leftPanel);

        // Panel de droite (graphique unique)
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.add(chartPanel, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createConfigPanel() {
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBackground(Color.WHITE);
        configPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Première ligne
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Type de détection:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        configPanel.add(detectionMethodCombo, gbc);
        
        gbc.gridx = 3; gbc.gridwidth = 1;
        configPanel.add(detectButton, gbc);
        
        gbc.gridx = 4;
        configPanel.add(nonDetectButton, gbc);

        gbc.gridx = 5;
        configPanel.add(new JLabel("Type d'attaque:"), gbc);
        
        gbc.gridx = 6;
        configPanel.add(attackTypeCombo, gbc);
        
        gbc.gridx = 7;
        configPanel.add(new JLabel("Paquets/s:"), gbc);
        
        gbc.gridx = 8;
        configPanel.add(packetsPerSecondSpinner, gbc);
        
        gbc.gridx = 9;
        configPanel.add(new JLabel("IPs sources:"), gbc);
        
        gbc.gridx = 10;
        configPanel.add(sourceIPsSpinner, gbc);

        // Deuxième ligne
        gbc.gridy = 1;
        gbc.gridx = 0;
        configPanel.add(new JLabel("Port source:"), gbc);
        
        gbc.gridx = 1;
        configPanel.add(sourcePortSpinner, gbc);
        
        gbc.gridx = 2;
        configPanel.add(new JLabel("Port destination:"), gbc);
        
        gbc.gridx = 3;
        configPanel.add(destPortSpinner, gbc);
        
        gbc.gridx = 4;
        configPanel.add(new JLabel("Durée (s):"), gbc);
        
        gbc.gridx = 5;
        configPanel.add(durationSpinner, gbc);

        gbc.gridx = 6; gbc.gridwidth = 2;
        configPanel.add(startButton, gbc);
        
        gbc.gridx = 8;
        configPanel.add(stopButton, gbc);
        
        gbc.gridx = 10;
        configPanel.add(resetButton, gbc);

        // Panel de status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        statusPanel.setBackground(Color.WHITE);
        statusPanel.add(valueLabel);
        statusPanel.add(detectionStatusLabel);

        JPanel fullConfigPanel = new JPanel(new BorderLayout());
        fullConfigPanel.setBackground(Color.WHITE);
        fullConfigPanel.add(configPanel, BorderLayout.CENTER);
        fullConfigPanel.add(statusPanel, BorderLayout.SOUTH);

        return fullConfigPanel;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Zone de logs avec titre
        JPanel logPanel = new JPanel(new BorderLayout(0, 5));
        logPanel.setBackground(Color.WHITE);
        logPanel.add(new JLabel("Logs détaillés"), BorderLayout.NORTH);
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Zone de console avec titre
        JPanel consolePanel = new JPanel(new BorderLayout(0, 5));
        consolePanel.setBackground(Color.WHITE);
        consolePanel.add(new JLabel("Console"), BorderLayout.NORTH);
        consolePanel.add(new JScrollPane(consoleArea), BorderLayout.CENTER);

        // Split pane vertical pour logs et console
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplitPane.setResizeWeight(0.67);
        leftSplitPane.setTopComponent(logPanel);
        leftSplitPane.setBottomComponent(consolePanel);
        
        leftPanel.add(leftSplitPane, BorderLayout.CENTER);

        return leftPanel;
    }

    private void setupEventListeners() {
        startButton.addActionListener(e -> {
            System.out.println("[DEBUG] Bouton Démarrer cliqué !");
            startSimulation();
        });
        
        stopButton.addActionListener(e -> {
            System.out.println("[DEBUG] Bouton Arrêter cliqué !");
            stopSimulation();
        });
        
        resetButton.addActionListener(e -> {
            System.out.println("[DEBUG] Bouton Réinitialiser cliqué !");
            resetSimulation();
        });
        
        detectButton.addActionListener(e -> detectButtonClicked());
        nonDetectButton.addActionListener(e -> nonDetectButtonClicked());

        detectionMethodCombo.addActionListener(e -> {
            String method = (String) detectionMethodCombo.getSelectedItem();
            System.out.println("[DEBUG] Changement de méthode : " + method);
            
            // Réinitialiser l'interface
            resetSimulation();
            
            // Mettre à jour les champs et l'interface selon la méthode
            updateFieldColors(method);
            updateInterfaceForMethod(method);
            
            // Appliquer les paramètres par défaut
            String type = (String) attackTypeCombo.getSelectedItem();
            applyParamsFromFile(type);
        });

        attackTypeCombo.addActionListener(e -> {
            String type = (String) attackTypeCombo.getSelectedItem();
            applyParamsFromFile(type);
        });
    }

    private void updateInterfaceForMethod(String method) {
        // Réinitialisation de l'interface
        resetSimulation();
        
        // Configuration selon la méthode
        switch (method) {
            case "Entropie IP":
                valueLabel.setText("Entropie: 0.000");
                chart.setTitle("Analyse d'Entropie des Adresses IP");
                chart.getXYPlot().getRangeAxis().setLabel("Entropie");
                break;
                
            case "Analyse de Flux":
                valueLabel.setText("Flux: 0 pkt/s");
                chart.setTitle("Analyse du Flux Réseau");
                chart.getXYPlot().getRangeAxis().setLabel("Paquets/s");
                break;
                
            case "Analyse Statistique":
                valueLabel.setText("Score Z: 0.000");
                chart.setTitle("Analyse Statistique du Trafic");
                chart.getXYPlot().getRangeAxis().setLabel("Score Z");
                break;
                
            case "Analyse Multi-Seuils":
                valueLabel.setText("Trafic: 0 pkt/s");
                chart.setTitle("Analyse Multi-Seuils");
                chart.getXYPlot().getRangeAxis().setLabel("Paquets/s");
                // Configuration des valeurs par défaut
                packetsPerSecondSpinner.setValue(2000);
                sourceIPsSpinner.setValue(50);
                durationSpinner.setValue(20);
                break;

            case "Apprentissage par Renforcement":
                valueLabel.setText("RL Score: 0.000");
                chart.setTitle("Apprentissage par Renforcement - Détection DDoS");
                chart.getXYPlot().getRangeAxis().setLabel("Score RL");
                break;
        }
        
        // Configuration commune
        chart.getXYPlot().getDomainAxis().setLabel("Temps (secondes)");
    }

    private void setupEntropyMode() {
        System.out.println("[DEBUG] Configuration du mode Entropie");
        
        // Configuration des paramètres pour le mode Entropie
        packetsPerSecondSpinner.setValue(2000);
        sourceIPsSpinner.setValue(isDetectMode ? 3 : 200);
        
        // Désactiver les ports en mode Entropie
        sourcePortSpinner.setEnabled(false);
        destPortSpinner.setEnabled(false);
        sourcePortSpinner.getEditor().getComponent(0).setBackground(new Color(240, 240, 240));
        destPortSpinner.getEditor().getComponent(0).setBackground(new Color(240, 240, 240));
        
        // Mise à jour du graphique
        JFreeChart chart = chartPanel.getChart();
        chart.setTitle("Entropie des Adresses IP Sources");
        XYPlot plot = chart.getXYPlot();
        plot.getRangeAxis().setLabel("Entropie");
        
        // Mise à jour du seuil d'entropie
        double entropyThreshold = isDetectMode ? 0.7 : 2.5;
        thresholdMarker.setValue(entropyThreshold);
        thresholdMarker.setLabel("Seuil d'entropie: " + String.format("%.2f", entropyThreshold));
        System.out.println("[DEBUG] Seuil d'entropie configuré à : " + entropyThreshold);
        
        // Mise à jour des séries
        detectSeries.clear();
        nonDetectSeries.clear();
        
        // Configuration des couleurs des séries
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(255, 0, 0));  // Rouge pour détection
        renderer.setSeriesPaint(1, new Color(0, 180, 0));  // Vert pour non-détection
        renderer.setSeriesVisible(0, true);
        renderer.setSeriesVisible(1, true);
        
        // Forcer le rafraîchissement du graphique
        chartPanel.repaint();
        
        System.out.println("[DEBUG] Mode Entropie configuré - Mode: " + (isDetectMode ? "Détection" : "Non-détection"));
    }

    private void setupFluxMode() {
        // Configuration des paramètres pour le mode Flux
        if (isDetectMode) {
            packetsPerSecondSpinner.setValue(8000);
            sourceIPsSpinner.setValue(10);
            sourcePortSpinner.setValue(12345);
            destPortSpinner.setValue(80);
        } else {
            packetsPerSecondSpinner.setValue(1000);
            sourceIPsSpinner.setValue(100);
            sourcePortSpinner.setValue(1024);
            destPortSpinner.setValue(0); // Port aléatoire
        }
        
        // Activer les ports en mode Flux
        sourcePortSpinner.setEnabled(true);
        destPortSpinner.setEnabled(true);
        sourcePortSpinner.getEditor().getComponent(0).setBackground(Color.WHITE);
        destPortSpinner.getEditor().getComponent(0).setBackground(Color.WHITE);
        
        // Mise à jour du graphique
        JFreeChart chart = chartPanel.getChart();
        chart.setTitle("Analyse du Flux Réseau");
        XYPlot plot = chart.getXYPlot();
        plot.getRangeAxis().setLabel("Paquets/s");
        
        // Mise à jour du seuil de flux
        double fluxThreshold = isDetectMode ? 5000 : 2000;
        thresholdMarker.setValue(fluxThreshold);
        thresholdMarker.setLabel("Seuil de flux: " + String.format("%.2f", fluxThreshold));
        
        // Mise à jour des séries
        detectSeries.clear();
        nonDetectSeries.clear();
    }

    private void setupStatisticalMode() {
        // Configuration des paramètres pour le mode Analyse Statistique
        Random random = new Random();
        
        // Paramètres de base
        int basePacketRate = 1000 + random.nextInt(500);  // Trafic de base: 1000-1500 pkt/s
        double baselineStdDev = basePacketRate * 0.2;     // Écart-type: 20% du trafic de base
        double zScoreThreshold = 2.0;                     // Seuil Z-score standard
        
        // Configuration des paramètres d'entrée
        packetsPerSecondSpinner.setValue(basePacketRate);
        sourceIPsSpinner.setValue(50 + random.nextInt(51));  // 50-100 IPs sources
        durationSpinner.setValue(20);                        // 20 secondes d'analyse
        
        // Configuration des seuils statistiques
        try {
            statisticalAnalyzer.setBaselineParameters(basePacketRate, baselineStdDev, zScoreThreshold);
            
            // Log des paramètres configurés
            logArea.append("\n=== Configuration de l'Analyse Statistique ===\n");
            logArea.append(String.format(
                "Paramètres d'entrée:%n" +
                "├─ Trafic de base: %d paquets/s%n" +
                "├─ Écart-type acceptable: %.2f%n" +
                "├─ Seuil Z-score: %.1f%n" +
                "├─ IPs sources: %d%n" +
                "└─ Durée d'analyse: %d secondes%n%n",
                basePacketRate, baselineStdDev, zScoreThreshold,
                (int)sourceIPsSpinner.getValue(), 
                (int)durationSpinner.getValue()
            ));
            
            // Afficher les avantages
            explainStatisticalAdvantages();
            
        } catch (Exception e) {
            logArea.append("Erreur de configuration: " + e.getMessage() + "\n");
        }
        
        // Auto-scroll des logs
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void explainStatisticalAdvantages() {
        logArea.append("\n=== Avantages de l'Analyse Statistique ===\n");
        logArea.append("1. Détection plus précise:\n");
        logArea.append("   ├─ Utilisation du score Z pour mesurer les anomalies\n");
        logArea.append("   ├─ Prise en compte de la variance normale du trafic\n");
        logArea.append("   └─ Moins de faux positifs\n\n");
        
        logArea.append("2. Métriques multiples:\n");
        logArea.append("   ├─ Trafic total (paquets/s)\n");
        logArea.append("   ├─ Distribution des protocoles\n");
        logArea.append("   ├─ Patterns temporels\n");
        logArea.append("   └─ Corrélations entre métriques\n\n");
        
        logArea.append("3. Adaptation dynamique:\n");
        logArea.append("   ├─ Ajustement automatique des seuils\n");
        logArea.append("   ├─ Apprentissage du comportement normal\n");
        logArea.append("   └─ Détection d'anomalies progressives\n\n");
        
        logArea.append("4. Classification des alertes:\n");
        logArea.append("   ├─ Alertes (confiance > 50%)\n");
        logArea.append("   ├─ Attaques (confiance > 80%)\n");
        logArea.append("   └─ Faux positifs potentiels\n");
    }

    private void resetInterface(JFreeChart chart) {
        // Réinitialisation des séries
        detectSeries.clear();
        nonDetectSeries.clear();
        
        // Mise à jour des labels
        String method = (String) detectionMethodCombo.getSelectedItem();
        valueLabel.setText(method.equals("Entropie IP") ? 
            "Entropie actuelle: 0.0" : 
            "Taux de flux actuel: 0.0");
        detectionStatusLabel.setText("Statut: Normal");
        detectionStatusLabel.setForeground(Color.BLACK);
        
        // Vider les zones de log
        logArea.setText("");
        consoleArea.setText("");
        
        // Réinitialiser le zoom
        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setAutoRange(true);
        plot.getRangeAxis().setAutoRange(true);
        
        // Forcer le rafraîchissement
        chartPanel.repaint();
    }

    private void applyParamsFromFile(String type) {
        String method = (String) detectionMethodCombo.getSelectedItem();
        Map<String, Integer> params = ParamsManager.getParams(method, type);
        if (params != null) {
            packetsPerSecondSpinner.setValue(params.getOrDefault("packets", 2000));
            durationSpinner.setValue(params.getOrDefault("duration", 15));
            sourceIPsSpinner.setValue(params.getOrDefault("ips", method.equals("Entropie IP") && type.equals("detect") ? 3 : (method.equals("Entropie IP") && type.equals("fail") ? 200 : 10)));
            sourcePortSpinner.setValue(params.getOrDefault("sourcePort", 12345));
            destPortSpinner.setValue(params.getOrDefault("destPort", 80));
            
            // Mettre à jour les couleurs des boutons
            if (type.equals("detect")) {
                detectButton.setBackground(new Color(255, 200, 200)); // Rouge clair
                nonDetectButton.setBackground(new Color(240, 240, 240)); // Gris clair
            } else {
                nonDetectButton.setBackground(new Color(200, 255, 200)); // Vert clair
                detectButton.setBackground(new Color(240, 240, 240)); // Gris clair
            }
            
            System.out.println("[DEBUG] Paramètres appliqués : " + params);
        }
    }

    private void startSimulation() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        startTime = System.currentTimeMillis();
        
        String method = (String) detectionMethodCombo.getSelectedItem();
        String attackType = (String) attackTypeCombo.getSelectedItem();
        int packetsPerSecond = (int) packetsPerSecondSpinner.getValue();
        int duration = (int) durationSpinner.getValue();
        int sourceIPs = (int) sourceIPsSpinner.getValue();
        
        // Réinitialiser les séries de données
        detectSeries.clear();
        nonDetectSeries.clear();
        
        // Log du démarrage
        logArea.append("\n=== Démarrage de la simulation ===\n");
        logArea.append("Méthode: " + method + "\n");
        logArea.append("Type d'attaque: " + attackType + "\n");
        logArea.append("Paquets/s: " + packetsPerSecond + "\n");
        logArea.append("Durée: " + duration + "s\n");
        logArea.append("IPs sources: " + sourceIPs + "\n\n");
        
        if (method.equals("Entropie IP")) {
            // Mise à jour du graphique principal pour l'entropie
            chart.setTitle("Entropie des Adresses IP Sources");
            XYPlot plot = chart.getXYPlot();
            plot.getRangeAxis().setLabel("Entropie");
            plot.getDomainAxis().setLabel("Temps (secondes)");
            simulator.startSimulation(packetsPerSecond, duration, sourceIPs, attackType);
        } 
        else if (method.equals("Analyse de Flux")) {
            System.out.println("[DEBUG] Démarrage de l'analyse de flux");
            // Mise à jour du graphique principal pour le flux
            chart.setTitle("Analyse du Flux Réseau");
            XYPlot plot = chart.getXYPlot();
            plot.getRangeAxis().setLabel("Paquets/s");
            plot.getDomainAxis().setLabel("Temps (secondes)");
            if (fluxSimulation == null) {
                fluxSimulation = new FluxSimulation();
            }
            fluxSimulation.setDetectMode(isDetectMode);
            fluxSimulation.addListener(this);
            fluxSimulation.startSimulation(packetsPerSecond, duration, sourceIPs, attackType);
        } 
        else if (method.equals("Analyse Statistique")) {
            System.out.println("[DEBUG] Démarrage de l'analyse statistique");
            if (statisticsWindow == null || !statisticsWindow.isVisible()) {
                statisticsWindow = new StatisticsWindow("Analyse Statistique en Temps Réel");
                statisticsWindow.setVisible(true);
            }
            statisticalAnalyzer.startAnalysis();
            startStatisticalSimulation(packetsPerSecond, duration, sourceIPs);
        } 
        else if (method.equals("Analyse Multi-Seuils")) {
            System.out.println("[DEBUG] Démarrage de l'analyse multi-seuils");
            startMultiThresholdSimulation();
        }
        else if (method.equals("Apprentissage par Renforcement")) {
            System.out.println("[DEBUG] Démarrage de la simulation RL");
            // Paramètres d'apprentissage par renforcement
            double learningRate = 0.05;  // Taux d'apprentissage
            double discountFactor = 0.95; // Facteur d'actualisation
            double epsilon = 0.5;         // Epsilon pour l'exploration
            
            RLAgent agent = new RLAgent(learningRate, discountFactor, epsilon);
            RLSimulationWindow rlSimWindow = new RLSimulationWindow(agent, isDetectMode);
            rlSimWindow.setVisible(true);
            
            // Log des paramètres
            logArea.append("\n=== Configuration RL ===\n");
            logArea.append(String.format("├─ Taux d'apprentissage: %.3f\n", learningRate));
            logArea.append(String.format("├─ Facteur d'actualisation: %.3f\n", discountFactor));
            logArea.append(String.format("└─ Epsilon: %.3f\n\n", epsilon));
        }
    }

    private void startStatisticalSimulation(int packetsPerSecond, int duration, int sourceIPs) {
        // Réinitialiser les séries pour le nouveau test
        trafficSeries.clear();
        baselineSeries.clear();
        anomalySeries.clear();
        
        // Initialiser les valeurs de base
        final double baselineValue = packetsPerSecond;
        final double stdDev = packetsPerSecond * 0.2; // 20% de variation normale
        
        // Rendre le simulateur final
        final ScheduledExecutorService simulator = Executors.newSingleThreadScheduledExecutor();
        final Random random = new Random();
        final long simulationStartTime = System.currentTimeMillis();
        
        // Planifier les mises à jour périodiques
        simulator.scheduleAtFixedRate(() -> {
            try {
                // Calculer le temps écoulé
                double timeInSeconds = (System.currentTimeMillis() - simulationStartTime) / 1000.0;
                
                // Générer des données simulées
                double normalVariation = random.nextGaussian() * stdDev;
                double currentValue = baselineValue + normalVariation;
                
                // Ajouter une anomalie si en mode détection
                if (isDetectMode && timeInSeconds > duration * 0.3) {
                    currentValue += packetsPerSecond * 1.5; // Augmentation significative
                }
                
                // Calculer la confiance et le statut d'anomalie
                double confidence = Math.min(1.0, Math.abs(normalVariation) / (stdDev * 2));
                boolean isAnomaly = confidence > 0.5;
                
                // Créer le rapport d'analyse avec le constructeur approprié
                final StatisticalManager.AnalysisResult result = new StatisticalManager.AnalysisResult(
                    currentValue,
                    isAnomaly,
                    "Anomalie de trafic (paquets/s)",
                    confidence
                );
                
                // Mettre à jour l'interface
                SwingUtilities.invokeLater(() -> updateStatisticalDisplay(result));
                
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> logArea.append("Erreur lors de la simulation: " + e.getMessage() + "\n"));
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        
        // Arrêt automatique après la durée spécifiée
        simulator.schedule(() -> {
            simulator.shutdown();
            SwingUtilities.invokeLater(() -> {
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
                logArea.append("\n=== Simulation terminée ===\n");
            });
        }, duration, TimeUnit.SECONDS);
    }

    private void stopSimulation() {
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        
        String method = (String) detectionMethodCombo.getSelectedItem();
        if (method.equals("Entropie IP")) {
            simulator.stopSimulation();
        } else if (method.equals("Analyse de Flux")) {
            fluxTest.stopTest();
        } else if (method.equals("Analyse Statistique")) {
            statisticalAnalyzer.stopAnalysis();
            if (statisticsWindow != null) {
                statisticsWindow.clearData();
            }
        }
        
        // Message de fin dans le log
        logArea.append("\n╔═══════════════════════════════════════════════════════════════════════════\n");
        logArea.append("║                     SIMULATION ARRÊTÉE                                    \n");
        logArea.append("╚═══════════════════════════════════════════════════════════════════════════\n");
    }

    private void resetSimulation() {
        // Réinitialiser les fenêtres
        if (statisticsWindow != null) {
            statisticsWindow.clearData();
            statisticsWindow.dispose();
            statisticsWindow = null;
        }
        
        // Réinitialiser les logs
        logArea.setText("═══════════════════ SIMULATION RÉINITIALISÉE ═══════════════════\n");
        consoleArea.setText("");
        
        // Réinitialiser les labels
        valueLabel.setText("Flux: 0 paquets/s");
        detectionStatusLabel.setText("Statut: Normal");
        detectionStatusLabel.setForeground(Color.BLACK);
        
        // Réinitialiser les boutons
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private String getValueLabelText(double value) {
        String method = (String) detectionMethodCombo.getSelectedItem();
        return method.equals("Entropie IP") ?
            String.format("Entropie actuelle: %.3f", value) :
            String.format("Taux de flux actuel: %.3f", value);
    }

    private void updateFieldColors(String method) {
        Color usedFieldColor = new Color(230, 255, 230);    // Vert clair
        Color unusedFieldColor = new Color(255, 230, 230);  // Rouge clair
        
        if (method.equals("Entropie IP")) {
            packetsPerSecondSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
            sourceIPsSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
            sourcePortSpinner.getEditor().getComponent(0).setBackground(unusedFieldColor);
            destPortSpinner.getEditor().getComponent(0).setBackground(unusedFieldColor);
            durationSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
        } else if (method.equals("Analyse de Flux")) {
            packetsPerSecondSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
            sourceIPsSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
            sourcePortSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
            destPortSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
            durationSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
        } else if (method.equals("Analyse Statistique")) {
            packetsPerSecondSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
            sourceIPsSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
            sourcePortSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
            destPortSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
            durationSpinner.getEditor().getComponent(0).setBackground(usedFieldColor);
        }
    }

    private void updateChartVisibility(String method) {
        // Le graphique est toujours visible
        chartPanel.setVisible(true);

        // Mettre à jour le titre et les axes selon la méthode
        if (method.equals("Analyse de Flux")) {
            chart.setTitle("Analyse du Flux Réseau - Comparaison des Modes");
            XYPlot plot = chart.getXYPlot();
            plot.getRangeAxis().setLabel("Paquets par seconde");
            plot.getDomainAxis().setLabel("Temps (secondes)");
            
            // Ajuster le seuil de détection
            thresholdMarker.setValue(DETECTION_THRESHOLD);
            thresholdMarker.setLabel("Seuil de détection DDoS: " + DETECTION_THRESHOLD + " paquets/s");
        }

        // Forcer le rafraîchissement du graphique
        chartPanel.repaint();

        // Configuration des séries dans updateChartVisibility
        XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

        renderer.setSeriesPaint(0, new Color(255, 0, 0));  // Rouge pour détection
        renderer.setSeriesPaint(1, new Color(0, 180, 0));  // Vert pour non-détection
        renderer.setSeriesVisible(0, true);
        renderer.setSeriesVisible(1, true);
    }

    private void detectButtonClicked() {
        System.out.println("[DEBUG] Bouton Détection cliqué");
        detectButton.setBackground(new Color(255, 200, 200));
        nonDetectButton.setBackground(new Color(240, 240, 240));
        isDetectMode = true;
        
        // Configuration automatique des valeurs selon le mode de détection
        String method = (String) detectionMethodCombo.getSelectedItem();
        if (method.equals("Entropie IP")) {
            packetsPerSecondSpinner.setValue(2000);
            sourceIPsSpinner.setValue(10);  // Petit nombre d'IPs pour simuler une attaque
            durationSpinner.setValue(15);
            // Log de la configuration
            logArea.append("\n=== Configuration Mode Détection (Entropie) ===\n");
            logArea.append("├─ Paquets/s: 2000\n");
            logArea.append("├─ IPs sources: 3\n");
            logArea.append("└─ Durée: 15s\n\n");
        } else if (method.equals("Analyse de Flux")) {
            packetsPerSecondSpinner.setValue(8000);  // Trafic élevé
            sourceIPsSpinner.setValue(10);  // Petit nombre d'IPs
            sourcePortSpinner.setValue(12345);
            destPortSpinner.setValue(80);
            durationSpinner.setValue(15);
            // Log de la configuration
            logArea.append("\n=== Configuration Mode Détection (Flux) ===\n");
            logArea.append("├─ Paquets/s: 8000\n");
            logArea.append("├─ IPs sources: 10\n");
            logArea.append("├─ Port source: 12345\n");
            logArea.append("├─ Port destination: 80\n");
            logArea.append("└─ Durée: 15s\n\n");
        } else if (method.equals("Analyse Statistique")) {
            packetsPerSecondSpinner.setValue(5000);  // Trafic anormal
            sourceIPsSpinner.setValue(5);   // Très peu d'IPs
            sourcePortSpinner.setValue(12345);
            destPortSpinner.setValue(80);
            durationSpinner.setValue(20);
            // Log de la configuration
            logArea.append("\n=== Configuration Mode Détection (Statistique) ===\n");
            logArea.append("├─ Paquets/s: 5000\n");
            logArea.append("├─ IPs sources: 5\n");
            logArea.append("├─ Port source: 12345\n");
            logArea.append("├─ Port destination: 80\n");
            logArea.append("└─ Durée: 20s\n\n");
        }
        
        // Mise à jour de la console
        consoleArea.append("Mode Détection activé - Configuration appliquée pour: " + method + "\n");
        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        
        // Auto-scroll des logs
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void nonDetectButtonClicked() {
        System.out.println("[DEBUG] Bouton Non-détection cliqué");
        nonDetectButton.setBackground(new Color(200, 255, 200));
        detectButton.setBackground(new Color(240, 240, 240));
        isDetectMode = false;
        
        // Configuration automatique des valeurs selon le mode normal
        String method = (String) detectionMethodCombo.getSelectedItem();
        if (method.equals("Entropie IP")) {
            packetsPerSecondSpinner.setValue(2000);  // Trafic normal
            sourceIPsSpinner.setValue(100);   // Nombre normal d'IPs
            durationSpinner.setValue(15);
            // Log de la configuration
            logArea.append("\n=== Configuration Mode Normal (Entropie) ===\n");
            logArea.append("├─ Paquets/s: 1000\n");
            logArea.append("├─ IPs sources: 10\n");
            logArea.append("└─ Durée: 15s\n\n");
        } else if (method.equals("Analyse de Flux")) {
            packetsPerSecondSpinner.setValue(2000);  // Trafic normal
            sourceIPsSpinner.setValue(50);   // Nombre normal d'IPs
            sourcePortSpinner.setValue(12345);
            destPortSpinner.setValue(80);
            durationSpinner.setValue(15);
            // Log de la configuration
            logArea.append("\n=== Configuration Mode Normal (Flux) ===\n");
            logArea.append("├─ Paquets/s: 2000\n");
            logArea.append("├─ IPs sources: 50\n");
            logArea.append("├─ Port source: 12345\n");
            logArea.append("├─ Port destination: 80\n");
            logArea.append("└─ Durée: 15s\n\n");
        } else if (method.equals("Analyse Statistique")) {
            packetsPerSecondSpinner.setValue(1500);  // Trafic normal
            sourceIPsSpinner.setValue(30);   // Nombre normal d'IPs
            sourcePortSpinner.setValue(12345);
            destPortSpinner.setValue(80);
            durationSpinner.setValue(20);
            // Log de la configuration
            logArea.append("\n=== Configuration Mode Normal (Statistique) ===\n");
            logArea.append("├─ Paquets/s: 1500\n");
            logArea.append("├─ IPs sources: 30\n");
            logArea.append("├─ Port source: 12345\n");
            logArea.append("├─ Port destination: 80\n");
            logArea.append("└─ Durée: 20s\n\n");
        }
        
        // Mise à jour de la console
        consoleArea.append("Mode Normal activé - Configuration appliquée pour: " + method + "\n");
        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        
        // Auto-scroll des logs
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    @Override
    public void onEntropyUpdate(double entropy, boolean isAttack) {
        SwingUtilities.invokeLater(() -> {
            // Mise à jour des valeurs affichées
            valueLabel.setText(String.format("Entropie: %.3f", entropy));
            
            // Mise à jour du statut
            if (isAttack) {
                detectionStatusLabel.setText("Statut: ATTAQUE DÉTECTÉE");
                detectionStatusLabel.setForeground(Color.RED);
            } else {
                detectionStatusLabel.setText("Statut: Normal");
                detectionStatusLabel.setForeground(new Color(0, 150, 0));
            }
            
            // Mise à jour du graphique principal
            double timeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
            if (isDetectMode) {
                detectSeries.add(timeInSeconds, entropy);
            } else {
                nonDetectSeries.add(timeInSeconds, entropy);
            }
            
            // Mise à jour des logs
            String logMessage = String.format("[%.2fs] Entropie: %.3f - %s%n",
                timeInSeconds, entropy, 
                isAttack ? "ATTAQUE DÉTECTÉE" : "Trafic normal");
            logArea.append(logMessage);
            
            // Auto-scroll des logs
            logArea.setCaretPosition(logArea.getDocument().getLength());
            
            // Forcer le rafraîchissement du graphique
            chartPanel.repaint();
        });
    }

    @Override
    public void onFluxUpdate(double fluxValue, boolean isAttack) {
        SwingUtilities.invokeLater(() -> {
            // Mise à jour des valeurs affichées
            valueLabel.setText(String.format("Flux: %.0f paquets/s", fluxValue));
            
            if (isAttack) {
                detectionStatusLabel.setText("ALERTE: ATTAQUE DDoS DÉTECTÉE!");
                detectionStatusLabel.setForeground(new Color(220, 0, 0));
            } else {
                detectionStatusLabel.setText("Statut: Normal");
                detectionStatusLabel.setForeground(new Color(0, 160, 0));
            }
            
            // Mise à jour du graphique principal
            double timeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
            if (isDetectMode) {
                detectSeries.add(timeInSeconds, fluxValue);
            } else {
                nonDetectSeries.add(timeInSeconds, fluxValue);
            }

            // Mise à jour des logs
            String logMessage = String.format("[%.2fs] %s - Flux: %.0f paquets/s - %s%n",
                timeInSeconds,
                isDetectMode ? "Mode Détection" : "Mode Normal",
                fluxValue,
                isAttack ? "⚠️ ATTAQUE DÉTECTÉE ⚠️" : "✓ Trafic normal");
            logArea.append(logMessage);
            
            // Auto-scroll des logs
            logArea.setCaretPosition(logArea.getDocument().getLength());
            
            // Forcer le rafraîchissement du graphique
            chartPanel.repaint();
        });
    }

    @Override
    public void onSimulationComplete() {
        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            
            String method = (String) detectionMethodCombo.getSelectedItem();
            StringBuilder completionMessage = new StringBuilder();
            completionMessage.append("\n═══════════════════ SIMULATION TERMINÉE ═══════════════════\n");
            completionMessage.append("Méthode utilisée: " + method + "\n");
            completionMessage.append("Durée totale: " + ((System.currentTimeMillis() - startTime) / 1000.0) + " secondes\n");
            completionMessage.append("Résultats finaux:\n");
            if (method.equals("Entropie IP")) {
                completionMessage.append("- Dernière valeur d'entropie: " + valueLabel.getText() + "\n");
            }
            completionMessage.append("- Statut final: " + detectionStatusLabel.getText() + "\n");
            completionMessage.append("═════════════════════════════════════════════════════════\n");
            
            logArea.append(completionMessage.toString());
        });
    }

    private void setupZoomButtons() {
        System.out.println("[DEBUG] Configuration des boutons de zoom");
        
        // Création des boutons de zoom
        zoomDetectButton = new JButton("🔍 Détection");
        zoomNonDetectButton = new JButton("🔍 Non-détection");
        resetZoomButton = new JButton("🔍 Vue complète");

        // Style des boutons
        Color zoomButtonColor = new Color(240, 240, 255);
        Font zoomFont = new Font("Arial", Font.PLAIN, 11);

        // Configuration initiale des boutons
        for (JButton btn : new JButton[]{zoomDetectButton, zoomNonDetectButton, resetZoomButton}) {
            btn.setBackground(zoomButtonColor);
            btn.setFont(zoomFont);
            btn.setFocusPainted(false);
            btn.setEnabled(true);  // Activer les boutons par défaut
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));  // Curseur de main pour indiquer qu'ils sont cliquables
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 220)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
        }

        // Ajout des listeners avec gestion des erreurs
        zoomDetectButton.addActionListener(e -> {
            System.out.println("[DEBUG] Bouton zoom détection cliqué");
            try {
                if (!detectSeries.isEmpty()) {
                    zoomOnSeries(detectSeries);
                } else {
                    System.out.println("[DEBUG] Série de détection vide");
                    // Garder le bouton actif même si la série est vide
                    zoomDetectButton.setEnabled(true);
                }
            } catch (Exception ex) {
                System.err.println("[ERREUR] Erreur lors du zoom sur la série de détection: " + ex.getMessage());
                // Réactiver le bouton en cas d'erreur
                zoomDetectButton.setEnabled(true);
            }
        });
        
        zoomNonDetectButton.addActionListener(e -> {
            System.out.println("[DEBUG] Bouton zoom non-détection cliqué");
            try {
                if (!nonDetectSeries.isEmpty()) {
                    zoomOnSeries(nonDetectSeries);
                } else {
                    System.out.println("[DEBUG] Série de non-détection vide");
                    // Garder le bouton actif même si la série est vide
                    zoomNonDetectButton.setEnabled(true);
                }
            } catch (Exception ex) {
                System.err.println("[ERREUR] Erreur lors du zoom sur la série de non-détection: " + ex.getMessage());
                // Réactiver le bouton en cas d'erreur
                zoomNonDetectButton.setEnabled(true);
            }
        });
        
        resetZoomButton.addActionListener(e -> {
            System.out.println("[DEBUG] Bouton reset zoom cliqué");
            try {
                resetZoom();
            } catch (Exception ex) {
                System.err.println("[ERREUR] Erreur lors de la réinitialisation du zoom: " + ex.getMessage());
            }
            // Toujours garder le bouton de réinitialisation actif
            resetZoomButton.setEnabled(true);
        });
        
        // Forcer l'activation initiale des boutons
        zoomDetectButton.setEnabled(true);
        zoomNonDetectButton.setEnabled(true);
        resetZoomButton.setEnabled(true);
        
        System.out.println("[DEBUG] Boutons de zoom configurés et activés");
    }

    private void zoomOnSeries(XYSeries series) {
        if (series.isEmpty()) {
            System.out.println("[DEBUG] Tentative de zoom sur une série vide");
            return;
        }

        try {
            XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
            
            // Calcul des limites avec marge
            double minX = series.getMinX();
            double maxX = series.getMaxX();
            double minY = series.getMinY();
            double maxY = series.getMaxY();
            
            // Ajout d'une marge de 10%
            double xMargin = (maxX - minX) * 0.1;
            double yMargin = (maxY - minY) * 0.1;
            
            // Application du zoom avec des marges minimales
            plot.getDomainAxis().setRange(Math.max(0, minX - xMargin), maxX + xMargin);
            plot.getRangeAxis().setRange(Math.max(0, minY - yMargin), maxY + yMargin);
            
            System.out.println("[DEBUG] Zoom appliqué sur la série");
        } catch (Exception e) {
            System.err.println("[ERREUR] Erreur lors de l'application du zoom: " + e.getMessage());
        }
    }

    private void resetZoom() {
        try {
            XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
            plot.getDomainAxis().setAutoRange(true);
            plot.getRangeAxis().setAutoRange(true);
            System.out.println("[DEBUG] Zoom réinitialisé");
        } catch (Exception e) {
            System.err.println("[ERREUR] Erreur lors de la réinitialisation du zoom: " + e.getMessage());
        }
    }

    private void updateZoomButtonsState() {
        try {
            // Les boutons sont toujours activés par défaut
            zoomDetectButton.setEnabled(true);
            zoomNonDetectButton.setEnabled(true);
            resetZoomButton.setEnabled(true);
            
            // Mise à jour de l'apparence en fonction des données
            boolean hasDetectData = !detectSeries.isEmpty();
            boolean hasNonDetectData = !nonDetectSeries.isEmpty();
            
            // Mise à jour des couleurs pour indiquer la disponibilité des données
            if (hasDetectData) {
                zoomDetectButton.setBackground(new Color(240, 240, 255));
            } else {
                zoomDetectButton.setBackground(new Color(245, 245, 245));
            }
            
            if (hasNonDetectData) {
                zoomNonDetectButton.setBackground(new Color(240, 240, 255));
            } else {
                zoomNonDetectButton.setBackground(new Color(245, 245, 245));
            }
            
            System.out.println("[DEBUG] État des boutons de zoom mis à jour - Détection: " + 
                             hasDetectData + ", Non-détection: " + hasNonDetectData);
        } catch (Exception e) {
            System.err.println("[ERREUR] Erreur lors de la mise à jour de l'état des boutons: " + e.getMessage());
            // En cas d'erreur, activer tous les boutons par défaut
            zoomDetectButton.setEnabled(true);
            zoomNonDetectButton.setEnabled(true);
            resetZoomButton.setEnabled(true);
        }
    }

    private void updateConsoleWithDetails(double timeInSeconds, double fluxValue, boolean isAttack) {
        String consoleMessage = String.format(
            "[%.2fs] %s%n" +
            "├─ Flux: %.0f paquets/s%n" +
            "├─ Mode: %s%n" +
            "├─ IPs sources: %d%n" +
            "├─ Port source: %d%n" +
            "├─ Port destination: %d%n" +
            "└─ Statut: %s%n%n",
            timeInSeconds,
            isDetectMode ? "SIMULATION DE DÉTECTION" : "SIMULATION NORMALE",
            fluxValue,
            isDetectMode ? "Détection DDoS" : "Trafic normal",
            (int) sourceIPsSpinner.getValue(),
            (int) sourcePortSpinner.getValue(),
            (int) destPortSpinner.getValue(),
            isAttack ? "⚠️ ATTAQUE DDoS DÉTECTÉE ⚠️" : "✓ Trafic normal");
        consoleArea.append(consoleMessage);
        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
    }

    private void updateStatisticalAnalysis(StatisticalAnalyzer.AnomalyReport report) {
        // Mise à jour de l'interface avec les résultats de l'analyse statistique
        String statusMessage = String.format(
            "Anomalie détectée (%s) - Confiance: %.2f%%",
            report.anomalyType,
            report.confidence * 100
        );
        
        detectionStatusLabel.setText(statusMessage);
        detectionStatusLabel.setForeground(
            report.confidence > 0.8 ? Color.RED : 
            report.confidence > 0.5 ? Color.ORANGE : 
            Color.GREEN
        );

        // Ajout du point au graphique
        double timeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        if (isDetectMode) {
            detectSeries.add(timeInSeconds, report.zScore);
        } else {
            nonDetectSeries.add(timeInSeconds, report.zScore);
        }

        // Mise à jour des logs
        String logMessage = String.format(
            "[%.2fs] Analyse Statistique:%n" +
            "├─ Type: %s%n" +
            "├─ Score Z: %.2f%n" +
            "└─ Confiance: %.2f%%%n",
            timeInSeconds,
            report.anomalyType,
            report.zScore,
            report.confidence * 100
        );
        logArea.append(logMessage);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void updateStatisticalDisplay(StatisticalManager.AnalysisResult result) {
        if (statisticsWindow != null && statisticsWindow.isVisible()) {
            statisticsWindow.updateStatistics(result);
        }
    }
    
    private void updateMetricLabel(JLabel label, String value) {
        updateMetricLabel(label, value, null);
    }
    
    private void updateMetricLabel(JLabel label, String value, Color color) {
        label.setText(value);
        if (color != null) {
            label.setForeground(color);
        }
    }
    
    private void updateStatsTable(StatisticalManager.AnalysisResult result) {
        // Mise à jour des valeurs dans le tableau avec les données disponibles
        statsTable.setValueAt(String.format("%.0f pkt/s", result.currentValue), 0, 1);
        statsTable.setValueAt(result.isAnomaly ? "Détecté" : "Normal", 1, 1);
        statsTable.setValueAt(String.format("%.1f%%", result.confidence * 100), 2, 1);
        
        // Mise à jour de l'indicateur de tendance basé sur l'anomalie
        String indicator = result.isAnomaly ? "↑" : "→";
        Color color = result.isAnomaly ? 
            (result.confidence > 0.8 ? Color.RED : Color.ORANGE) : 
            Color.BLACK;
        statsTable.setValueAt(indicator, 0, 2);
        ((DefaultTableCellRenderer)statsTable.getCellRenderer(0, 2)).setForeground(color);
    }
    
    private void updateTrendIndicator(JTable table, int row, double trend) {
        String indicator = trend > 0.1 ? "↑" : trend < -0.1 ? "↓" : "→";
        Color color = trend > 0.1 ? Color.RED : trend < -0.1 ? Color.BLUE : Color.BLACK;
        table.setValueAt(indicator, row, 2);
        ((DefaultTableCellRenderer)table.getCellRenderer(row, 2)).setForeground(color);
    }
    
    private void updateAlertCounters(StatisticalManager.AnalysisResult result) {
        if (result.isAnomaly) {
            // Mise à jour des compteurs selon le niveau de confiance
            if (result.confidence > 0.8) {
                incrementCounter(attackCounter);
                animateCounter(attackCounter);
            } else if (result.confidence > 0.5) {
                incrementCounter(alertCounter);
                animateCounter(alertCounter);
            } else {
                incrementCounter(fpCounter);
            }
        }
    }
    
    private void incrementCounter(JPanel counterPanel) {
        JLabel valueLabel = (JLabel) counterPanel.getComponent(1);
        int currentValue = Integer.parseInt(valueLabel.getText());
        valueLabel.setText(String.valueOf(currentValue + 1));
    }
    
    private void animateCounter(JPanel counterPanel) {
        // Animation simple du compteur (clignotement)
        Timer timer = new Timer(100, null);
        timer.addActionListener(new ActionListener() {
            private int count = 0;
            private final Color originalColor = counterPanel.getBackground();
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (count < 6) { // 3 clignotements
                    counterPanel.setBackground(count % 2 == 0 ? 
                        new Color(255, 255, 200) : originalColor);
                    count++;
                } else {
                    counterPanel.setBackground(originalColor);
                    ((Timer)e.getSource()).stop();
                }
            }
        });
        timer.start();
    }
    
    private void addAlertEntry(StatisticalManager.AnalysisResult result) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String confidenceStr = String.format("%.1f%%", result.confidence * 100);
        String severity = result.confidence > 0.8 ? "CRITIQUE" : 
                         result.confidence > 0.5 ? "ALERTE" : "INFO";
        String colorCode = result.confidence > 0.8 ? "\u001B[31m" : // Rouge
                          result.confidence > 0.5 ? "\u001B[33m" : // Orange
                          "\u001B[90m"; // Gris
        
        String entry = String.format("%s%s [%s] %s (Confiance: %s)%n" +
                                   "   └─ Trafic: %.0f pkt/s%n",
            colorCode, timestamp, severity, result.anomalyType,
            confidenceStr, result.currentValue);
        
        alertsArea.append(entry);
        alertsArea.setCaretPosition(alertsArea.getDocument().getLength());
    }

    private void customizeChart(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(0, 0, 180));  // Bleu pour la série principale
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesPaint(1, new Color(0, 150, 0));  // Vert pour la baseline
        renderer.setSeriesStroke(1, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 
                                                  1.0f, new float[]{6.0f, 6.0f}, 0.0f));
        
        plot.setRenderer(renderer);
        
        // Personnalisation des axes
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 12));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 12));
        
        // Ajout d'une légende
        chart.getLegend().setFrame(BlockBorder.NONE);
        chart.getLegend().setBackgroundPaint(Color.WHITE);
    }

    private JPanel createMetricPanel(String name, String initialValue) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        JLabel valueLabel = new JLabel(initialValue);
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        panel.add(nameLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        
        // Stocker la référence du JLabel pour les mises à jour
        if (name.equals("Trafic Actuel")) currentTrafficLabel = valueLabel;
        else if (name.equals("Score Z")) zScoreLabel = valueLabel;
        else if (name.equals("Niveau de Confiance")) confidenceLabel = valueLabel;
        else if (name.equals("État")) statusLabel = valueLabel;
        
        return panel;
    }

    private void setupStatisticsPanel() {
        statisticsPanel = new JPanel(new BorderLayout(10, 10));
        statisticsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel principal divisé en deux colonnes
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6); // 60% gauche, 40% droite
        
        // --- PANNEAU GAUCHE : Graphiques et Métriques ---
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        
        // Initialisation des séries si elles sont null
        if (trafficSeries == null) trafficSeries = new XYSeries("Trafic actuel");
        if (baselineSeries == null) baselineSeries = new XYSeries("Baseline");
        if (anomalySeries == null) anomalySeries = new XYSeries("Anomalies");
        
        // Créer le dataset
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(trafficSeries);
        dataset.addSeries(baselineSeries);
        dataset.addSeries(anomalySeries);
        
        // Créer le graphique
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Analyse Statistique du Trafic",
            "Temps (secondes)",
            "Paquets/s",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        
        // Personnaliser le graphique
        customizeChart(chart);
        
        // Créer le panel du graphique
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        
        // Panel des métriques en temps réel
        JPanel metricsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        metricsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Initialisation des labels de métriques
        currentTrafficLabel = new JLabel("0 pkt/s");
        zScoreLabel = new JLabel("0.00");
        confidenceLabel = new JLabel("0%");
        statusLabel = new JLabel("Normal");
        
        // Création des panneaux de métriques
        metricsPanel.add(createMetricPanel("Trafic Actuel", currentTrafficLabel));
        metricsPanel.add(createMetricPanel("Score Z", zScoreLabel));
        metricsPanel.add(createMetricPanel("Niveau de Confiance", confidenceLabel));
        metricsPanel.add(createMetricPanel("État", statusLabel));
        
        // Ajout des composants au panneau gauche
        leftPanel.add(chartPanel, BorderLayout.CENTER);
        leftPanel.add(metricsPanel, BorderLayout.SOUTH);
        
        // --- PANNEAU DROIT : Alertes et Statistiques ---
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        
        // Tableau des statistiques
        String[] columnNames = {"Métrique", "Valeur", "Tendance"};
        Object[][] data = {
            {"Trafic", "0 pkt/s", "→"},
            {"État", "Normal", ""},
            {"Confiance", "0%", ""}
        };
        statsTable = new JTable(data, columnNames);
        customizeStatsTable(statsTable);
        
        // Zone des alertes
        alertsArea = new JTextArea();
        alertsArea.setEditable(false);
        alertsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // Compteurs d'alertes
        JPanel countersPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        alertCounter = createAnimatedCounter("Alertes", "0", new Color(255, 165, 0), "⚠");
        attackCounter = createAnimatedCounter("Attaques", "0", Color.RED, "🔥");
        fpCounter = createAnimatedCounter("Faux +", "0", Color.GRAY, "ℹ");
        
        countersPanel.add(alertCounter);
        countersPanel.add(attackCounter);
        countersPanel.add(fpCounter);
        
        // Organisation du panneau droit
        JPanel statsPanel = new JPanel(new BorderLayout(5, 5));
        statsPanel.add(new JScrollPane(statsTable), BorderLayout.NORTH);
        statsPanel.add(countersPanel, BorderLayout.CENTER);
        
        rightPanel.add(statsPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(alertsArea), BorderLayout.CENTER);
        
        // Configuration finale du split pane
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);
        
        // Ajout au panel principal
        statisticsPanel.add(mainSplitPane, BorderLayout.CENTER);
        
        // Mise à jour de l'interface
        add(statisticsPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel createMetricPanel(String name, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        panel.add(nameLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        
        return panel;
    }

    private void updateAlertFilters() {
        // Cette méthode sera appelée quand les filtres changent
        // Implémentation à venir selon les besoins
    }

    private JPanel createAnimatedCounter(String label, String value, Color color, String icon) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 24));
        iconLabel.setForeground(color);
        
        JLabel titleLabel = new JLabel(label, SwingConstants.CENTER);
        titleLabel.setForeground(color);
        
        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 18));
        valueLabel.setForeground(color);
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setOpaque(false);
        topPanel.add(iconLabel);
        topPanel.add(titleLabel);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        
        return panel;
    }

    private void customizeStatsTable(JTable table) {
        table.setEnabled(false);
        table.setRowHeight(25);
        table.setIntercellSpacing(new Dimension(5, 5));
        table.setShowGrid(true);
        table.setGridColor(Color.LIGHT_GRAY);
        
        // Personnalisation des en-têtes
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(240, 240, 240));
        
        // Personnalisation des cellules
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Alignement des colonnes
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
    }

    private void setupMultiThresholdMode() {
        // Réinitialisation des séries de données
        trafficSeries = new XYSeries("Trafic Actuel");
        baselineSeries = new XYSeries("Baseline");
        anomalySeries = new XYSeries("Anomalies");
        thresholdSeries = new XYSeries("Seuils");
        
        // Configuration du dataset
        dataset = new XYSeriesCollection();
        dataset.addSeries(trafficSeries);
        dataset.addSeries(baselineSeries);
        dataset.addSeries(thresholdSeries);
        dataset.addSeries(anomalySeries);
        
        // Configuration du graphique
        chart = ChartFactory.createXYLineChart(
            "Analyse Multi-Seuils",
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
        renderer.setSeriesPaint(0, Color.BLUE);      // Trafic actuel
        renderer.setSeriesPaint(1, Color.GREEN);     // Baseline
        renderer.setSeriesPaint(2, Color.RED);       // Seuils
        renderer.setSeriesPaint(3, Color.ORANGE);    // Anomalies
        
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesStroke(1, new BasicStroke(1.5f));
        renderer.setSeriesStroke(2, new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
        renderer.setSeriesStroke(3, new BasicStroke(2.0f));
        
        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Mise à jour du panel
        if (chartPanel != null) {
            chartPanel.setChart(chart);
        } else {
            chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(800, 400));
        }
    }

    private void startMultiThresholdSimulation() {
        // Créer et afficher la nouvelle fenêtre de simulation multi-seuils
        MultiThresholdWindow multiThresholdWindow = new MultiThresholdWindow(isDetectMode);
        multiThresholdWindow.setVisible(true);
        
        // Log des paramètres
        logArea.append("\n=== Démarrage de l'Analyse Multi-Seuils ===\n");
        logArea.append(String.format(
            "Configuration:%n" +
            "├─ Mode: %s%n" +
            "├─ Paquets/s initial: %d%n" +
            "├─ IPs sources: %d%n" +
            "└─ Durée: %d secondes%n%n",
            isDetectMode ? "Détection" : "Normal",
            (int)packetsPerSecondSpinner.getValue(),
            (int)sourceIPsSpinner.getValue(),
            (int)durationSpinner.getValue()
        ));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
} 