package com.ddos.detection.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.ddos.detection.rl.RLAgent;
import com.ddos.detection.rl.RLManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class RLWindow extends JFrame {
    private RLAgent agent;
    private JTextArea descriptionArea;
    private JPanel controlPanel;
    private ChartPanel learningChart;
    private XYSeries rewardSeries;
    private XYSeries epsilonSeries;
    private JLabel statusLabel;
    private JButton trainButton;
    private JButton detectButton;
    private JLabel modelStatusLabel;
    private boolean isModelTrained = false;
    private static final String MODEL_PATH = "rl_model.json";
    private JSpinner learningRateSpinner;
    private JSpinner epsilonSpinner;
    private JSpinner discountFactorSpinner;
    private RLManager rlManager;

    public RLWindow() {
        setTitle("Apprentissage par Renforcement - Détection DDoS");
        setSize(1000, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Initialisation de l'agent et du manager
        rlManager = new RLManager(2000.0, (reward, epsilon, episode) -> {
            SwingUtilities.invokeLater(() -> updateChart(reward, epsilon, episode));
        });

        // Vérifier si un modèle existe déjà
        if (new File(MODEL_PATH).exists()) {
            loadModel();
            isModelTrained = true;
        }

        // Création des composants
        createDescriptionPanel();
        createControlPanel();
        createChartPanel();
        createStatusPanel();

        // Configuration finale
        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void createDescriptionPanel() {
        JPanel descPanel = new JPanel(new BorderLayout(5, 5));
        descPanel.setBorder(BorderFactory.createTitledBorder("Description de la Méthode"));

        descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setFont(new Font("Arial", Font.PLAIN, 14));
        descriptionArea.setText(
            "Apprentissage par Renforcement pour la Détection DDoS\n\n" +
            "Cette approche utilise le Q-Learning pour apprendre à détecter les attaques DDoS en temps réel. " +
            "L'agent apprend progressivement à distinguer le trafic normal du trafic malveillant en se basant sur " +
            "différentes caractéristiques du réseau :\n\n" +
            "1. États : Combinaisons de métriques réseau (trafic, entropie, etc.)\n" +
            "2. Actions : Normal, Alerte, Critique\n" +
            "3. Récompenses : Basées sur la précision de la détection\n\n" +
            "Avantages de cette approche :\n" +
            "• Adaptation dynamique aux nouveaux patterns d'attaque\n" +
            "• Amélioration continue des performances\n" +
            "• Réduction des faux positifs\n" +
            "• Personnalisation selon l'environnement réseau\n\n" +
            "Paramètres ajustables :\n" +
            "• Taux d'apprentissage : Vitesse d'apprentissage de l'agent\n" +
            "• Facteur d'actualisation : Importance des récompenses futures\n" +
            "• Epsilon : Balance entre exploration et exploitation"
        );

        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        descPanel.add(scrollPane, BorderLayout.CENTER);
        add(descPanel, BorderLayout.NORTH);
    }

    private void createControlPanel() {
        controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Contrôles"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Spinners pour les paramètres
        learningRateSpinner = new JSpinner(new SpinnerNumberModel(0.05, 0.001, 1.0, 0.001));
        epsilonSpinner = new JSpinner(new SpinnerNumberModel(0.5, 0.01, 1.0, 0.01));
        discountFactorSpinner = new JSpinner(new SpinnerNumberModel(0.95, 0.1, 1.0, 0.01));

        // Ajout des composants avec leurs labels
        addLabelAndComponent("Taux d'apprentissage:", learningRateSpinner, gbc, 0);
        addLabelAndComponent("Epsilon:", epsilonSpinner, gbc, 1);
        addLabelAndComponent("Facteur d'actualisation:", discountFactorSpinner, gbc, 2);

        // Boutons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        trainButton = new JButton("Démarrer l'entraînement");
        detectButton = new JButton("Mode Détection");
        detectButton.setEnabled(isModelTrained);

        // Status du modèle
        modelStatusLabel = new JLabel(isModelTrained ? "Modèle entraîné" : "Modèle non entraîné");
        modelStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        modelStatusLabel.setForeground(isModelTrained ? new Color(0, 150, 0) : Color.RED);

        buttonPanel.add(trainButton);
        buttonPanel.add(detectButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        controlPanel.add(buttonPanel, gbc);

        gbc.gridy = 4;
        controlPanel.add(modelStatusLabel, gbc);

        add(controlPanel, BorderLayout.WEST);

        // Ajout des listeners
        setupEventListeners();
    }

    private void addLabelAndComponent(String labelText, JComponent component, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        controlPanel.add(component, gbc);
    }

    private void createChartPanel() {
        // Création des séries de données
        rewardSeries = new XYSeries("Récompenses");
        epsilonSeries = new XYSeries("Epsilon");

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(rewardSeries);
        dataset.addSeries(epsilonSeries);

        // Création du graphique
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Performance de l'Apprentissage",
            "Épisodes",
            "Valeur",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        learningChart = new ChartPanel(chart);
        learningChart.setPreferredSize(new Dimension(600, 400));
        add(learningChart, BorderLayout.CENTER);
    }

    private void createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("État"));

        statusLabel = new JLabel("En attente de démarrage...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        add(statusPanel, BorderLayout.SOUTH);
    }

    private void setupEventListeners() {
        trainButton.addActionListener(e -> {
            startTraining();
            trainButton.setEnabled(false);
            detectButton.setEnabled(false);
            learningRateSpinner.setEnabled(false);
            epsilonSpinner.setEnabled(false);
            discountFactorSpinner.setEnabled(false);
        });

        detectButton.addActionListener(e -> {
            startDetection();
            trainButton.setEnabled(false);
            detectButton.setEnabled(false);
        });
    }

    private void startTraining() {
        // Réinitialisation des séries
        rewardSeries.clear();
        epsilonSeries.clear();

        // Mise à jour des paramètres
        double lr = (double) learningRateSpinner.getValue();
        double eps = (double) epsilonSpinner.getValue();
        double df = (double) discountFactorSpinner.getValue();
        rlManager.updateParameters(lr, df, eps);

        statusLabel.setText("Entraînement en cours...");
        rlManager.startTraining(() -> {
            // Callback appelé quand l'entraînement est terminé
            SwingUtilities.invokeLater(() -> {
                saveModel();
                isModelTrained = true;
                modelStatusLabel.setText("Modèle entraîné");
                modelStatusLabel.setForeground(new Color(0, 150, 0));
                trainButton.setEnabled(true);
                detectButton.setEnabled(true);
                statusLabel.setText("Entraînement terminé");
            });
        });
    }

    private void startDetection() {
        if (!isModelTrained) {
            JOptionPane.showMessageDialog(this,
                "Le modèle doit d'abord être entraîné.",
                "Modèle non entraîné",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        statusLabel.setText("Mode détection actif");
        rlManager.startDetection();
    }

    private void saveModel() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File(MODEL_PATH), rlManager.getAgent().getQTable());
            System.out.println("Modèle sauvegardé avec succès");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la sauvegarde du modèle");
        }
    }

    private void loadModel() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, double[]> qTable = mapper.readValue(new File(MODEL_PATH),
                new TypeReference<Map<String, double[]>>() {});
            rlManager.getAgent().setQTable(qTable);
            System.out.println("Modèle chargé avec succès");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement du modèle");
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    public void updateChart(double reward, double epsilon, int episode) {
        rewardSeries.add(episode, reward);
        epsilonSeries.add(episode, epsilon);
        statusLabel.setText(String.format("Épisode: %d, Récompense: %.2f, Epsilon: %.2f", 
                          episode, reward, epsilon));
    }
} 