package com.ddos.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ParamsManager {
    private static final String[] METHODS = {
            "Entropie IP", "Analyse de flux", "Heuristique", "Machine Learning", "Reinforcement Learning"
    };
    private static final String[] FILES = {
            "entropy_params.json", "flow_params.json", "heuristic_params.json", "ml_params.json", "rl_params.json"
    };
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Valeurs par défaut pour chaque méthode
    private static final Map<String, Map<String, Integer>> DEFAULTS = new HashMap<>() {{
        put("Entropie IP_detect", Map.of("packets", 2000, "duration", 15, "ips", 3));
        put("Entropie IP_fail", Map.of("packets", 2000, "duration", 15, "ips", 200));
        put("Analyse de flux_detect", Map.of("packets", 6000, "duration", 90, "ips", 15));
        put("Analyse de flux_fail", Map.of("packets", 15, "duration", 90, "ips", 15));
        put("Heuristique_detect", Map.of("packets", 75, "duration", 400, "ips", 8));
        put("Heuristique_fail", Map.of("packets", 75, "duration", 400, "ips", 8));
        put("Machine Learning_detect", Map.of("packets", 3000, "duration", 90, "ips", 60));
        put("Machine Learning_fail", Map.of("packets", 3000, "duration", 90, "ips", 60));
        put("Reinforcement Learning_detect", Map.of("packets", 3000, "duration", 90, "ips", 5));
        put("Reinforcement Learning_fail", Map.of("packets", 3000, "duration", 90, "ips", 5));
    }};

    public static void ensureAllParamsFilesExist() {
        for (int i = 0; i < METHODS.length; i++) {
            File file = new File(FILES[i]);
            if (!file.exists()) {
                Map<String, Map<String, Integer>> params = new HashMap<>();
                params.put("detect", DEFAULTS.get(METHODS[i] + "_detect"));
                params.put("fail", DEFAULTS.get(METHODS[i] + "_fail"));
                try {
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, params);
                } catch (IOException ignored) {}
            }
        }
    }

    public static Map<String, Integer> getParams(String method, String type) {
        int idx = getMethodIndex(method);
        if (idx == -1) return null;
        File file = new File(FILES[idx]);
        try {
            Map<String, Map<String, Integer>> params = objectMapper.readValue(file, HashMap.class);
            return params.get(type);
        } catch (IOException e) {
            return DEFAULTS.get(method + "_" + type);
        }
    }

    public static void saveParams(String method, Map<String, Integer> detectParams, Map<String, Integer> failParams) {
        int idx = getMethodIndex(method);
        if (idx == -1) return;
        File file = new File(FILES[idx]);
        Map<String, Map<String, Integer>> params = new HashMap<>();
        params.put("detect", detectParams);
        params.put("fail", failParams);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, params);
        } catch (IOException ignored) {}
    }

    private static int getMethodIndex(String method) {
        for (int i = 0; i < METHODS.length; i++) {
            if (METHODS[i].equals(method)) return i;
        }
        return -1;
    }

    public static String[] getMethods() {
        return METHODS;
    }
} 