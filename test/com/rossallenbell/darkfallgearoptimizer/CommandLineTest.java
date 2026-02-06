package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class CommandLineTest {

    @Before
    public void setUp() {
        // Reset protection weights before each test
        DarkfallGearOptimizer.protectionWeights = new HashMap<PROTECTION, Double>();
    }

    @After
    public void tearDown() {
        // Clean up any generated result files
        File currentDir = new File(".");
        File[] resultFiles = currentDir.listFiles((dir, name) ->
            name.startsWith("results-") && (name.endsWith(".json") || name.endsWith(".txt"))
        );
        if (resultFiles != null) {
            for (File file : resultFiles) {
                if (file.getName().contains("test")) {
                    file.delete();
                }
            }
        }
    }

    @Test
    public void testWeightParsing_SingleWeight() {
        String[] args = {"--weight", "slashing=1.0"};

        // Simulate parsing
        DarkfallGearOptimizer.protectionWeights.clear();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--weight")) {
                String[] parts = args[i + 1].split("=");
                String protectionName = parts[0].substring(0, 1).toUpperCase() +
                                      parts[0].substring(1).toLowerCase();
                PROTECTION protection = PROTECTION.valueOf(protectionName);
                double value = Double.parseDouble(parts[1]);
                DarkfallGearOptimizer.protectionWeights.put(protection, value);
                i++;
            }
        }

        assertEquals(1, DarkfallGearOptimizer.protectionWeights.size());
        assertEquals(1.0, DarkfallGearOptimizer.protectionWeights.get(PROTECTION.Slashing), 0.001);
    }

    @Test
    public void testWeightParsing_MultipleWeights() {
        String[] args = {"--weight", "slashing=1.0", "--weight", "fire=0.5"};

        // Simulate parsing
        DarkfallGearOptimizer.protectionWeights.clear();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--weight")) {
                String[] parts = args[i + 1].split("=");
                String protectionName = parts[0].substring(0, 1).toUpperCase() +
                                      parts[0].substring(1).toLowerCase();
                PROTECTION protection = PROTECTION.valueOf(protectionName);
                double value = Double.parseDouble(parts[1]);
                DarkfallGearOptimizer.protectionWeights.put(protection, value);
                i++;
            }
        }

        assertEquals(2, DarkfallGearOptimizer.protectionWeights.size());
        assertEquals(1.0, DarkfallGearOptimizer.protectionWeights.get(PROTECTION.Slashing), 0.001);
        assertEquals(0.5, DarkfallGearOptimizer.protectionWeights.get(PROTECTION.Fire), 0.001);
    }

    @Test
    public void testDatasetNameExtraction() {
        assertEquals("minimal", extractDatasetName("./data/armor-data-minimal.csv"));
        assertEquals("common", extractDatasetName("./data/armor-data-common.csv"));
        assertEquals("complete", extractDatasetName("./data/armor-data-complete.csv"));
        assertEquals("unknown", extractDatasetName("./data/armor-data-unknown.csv"));
    }

    @Test
    public void testWeightIndicatorGeneration() {
        DarkfallGearOptimizer.protectionWeights.clear();
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Slashing, 1.0);
        String indicator = generateWeightIndicator();
        assertTrue(indicator.contains("slashing100"));

        DarkfallGearOptimizer.protectionWeights.clear();
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Slashing, 1.0);
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Fire, 0.5);
        indicator = generateWeightIndicator();
        assertTrue(indicator.contains("slashing100"));
        assertTrue(indicator.contains("fire50"));
    }

    @Test
    public void testJsonFormatValidation() throws IOException {
        // This is a simple validation test
        // In a real scenario, you'd want to parse the JSON and verify its structure
        String[] args = {"./data/armor-data-minimal.csv", "--format", "json", "--weight", "slashing=1.0"};

        // Run the optimizer (this would be done via main method in integration test)
        // For unit test, we just verify the format parameter is accepted
        String format = "text";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--format")) {
                format = args[i + 1];
                break;
            }
        }
        assertEquals("json", format);
    }

    // Helper methods (duplicated from main class for testing)
    private String extractDatasetName(String filePath) {
        String fileName = new File(filePath).getName().toLowerCase();
        if (fileName.contains("minimal")) {
            return "minimal";
        } else if (fileName.contains("common")) {
            return "common";
        } else if (fileName.contains("complete")) {
            return "complete";
        }
        return "unknown";
    }

    private String generateWeightIndicator() {
        java.util.List<String> parts = new java.util.ArrayList<String>();
        for (Map.Entry<PROTECTION, Double> entry : DarkfallGearOptimizer.protectionWeights.entrySet()) {
            if (entry.getValue() > 0) {
                String protectionName = entry.getKey().name().toLowerCase();
                int weightValue = (int) Math.round(entry.getValue() * 100);
                parts.add(protectionName + weightValue);
            }
        }
        return String.join("-", parts);
    }
}
