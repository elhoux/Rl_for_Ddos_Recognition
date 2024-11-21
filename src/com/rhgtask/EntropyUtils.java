package com.rhgtask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntropyUtils {
	// Calculate entropy of incoming data packets
	public static double calculateEntropy(List<String> packets) {
		Map<Character, Integer> frequencyMap = new HashMap<>();
		for (String packet : packets) {
			for (char ch : packet.toCharArray()) {
				frequencyMap.put(ch, frequencyMap.getOrDefault(ch, 0) + 1);
			}
		}

		double entropy = 0.0;
		int totalPackets = packets.size();
		for (int count : frequencyMap.values()) {
			double probability = (double) count / totalPackets;
			entropy -= probability * (Math.log(probability) / Math.log(2));
		}

		return entropy;
	}
}
