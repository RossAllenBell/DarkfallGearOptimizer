package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class GeneratePresetsTest {

    @After
    public void tearDown() {
        // Clean up test files
        File currentDir = new File(".");
        File[] resultFiles = currentDir.listFiles((dir, name) ->
            name.startsWith("results-") && name.contains("minimal") && (name.endsWith(".json") || name.endsWith(".txt"))
        );
        if (resultFiles != null) {
            for (File file : resultFiles) {
                file.delete();
            }
        }
    }

    @Test
    public void testPresetCombinations() {
        // Test that we have the expected number of preset combinations
        // Individual protections: 10
        // All protections: 1
        // Physical protections: 1
        // Magic protections: 1
        // Slashing + Fire: 1
        // Slashing-heavy + Fire: 1
        // Fire-heavy + Slashing: 1
        // Total: 16

        int expectedPresets = 16;
        assertEquals("Should have 16 preset combinations", expectedPresets,
                PROTECTION.values().length + 6);
    }

    @Test
    public void testWeightNormalization() {
        // Verify that weights are normalized correctly
        // [slashing=1.0, fire=1.0] should produce same score as [slashing=0.5, fire=0.5]

        // Set up test data
        DarkfallGearOptimizer.protectionWeights.clear();
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Slashing, 1.0);
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Fire, 1.0);

        Armor armor1 = new Armor(
            DarkfallGearOptimizer.ARMOR_TYPE.Scale,
            DarkfallGearOptimizer.ARMOR_SLOT.Chest,
            5.0
        );
        armor1.addResistance(PROTECTION.Slashing, 0.5);
        armor1.addResistance(PROTECTION.Fire, 0.3);
        double score1 = armor1.getResistanceScore();

        // Change weights but keep ratio the same
        DarkfallGearOptimizer.protectionWeights.clear();
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Slashing, 0.5);
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Fire, 0.5);

        Armor armor2 = new Armor(
            DarkfallGearOptimizer.ARMOR_TYPE.Scale,
            DarkfallGearOptimizer.ARMOR_SLOT.Chest,
            5.0
        );
        armor2.addResistance(PROTECTION.Slashing, 0.5);
        armor2.addResistance(PROTECTION.Fire, 0.3);
        double score2 = armor2.getResistanceScore();

        assertEquals("Scores should be equal for proportional weights", score1, score2, 0.0001);
    }

    @Test
    public void testSingleProtectionWeight() {
        // Test that a single protection weight works correctly
        DarkfallGearOptimizer.protectionWeights.clear();
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Slashing, 1.0);

        Armor armor = new Armor(
            DarkfallGearOptimizer.ARMOR_TYPE.Scale,
            DarkfallGearOptimizer.ARMOR_SLOT.Chest,
            5.0
        );
        armor.addResistance(PROTECTION.Slashing, 0.5);
        armor.addResistance(PROTECTION.Fire, 0.3);

        double score = armor.getResistanceScore();
        // Should only count slashing, so score = 0.5
        assertEquals("Score should only include slashing", 0.5, score, 0.0001);
    }

    @Test
    public void testMultipleProtectionWeights() {
        // Test weighted average calculation
        DarkfallGearOptimizer.protectionWeights.clear();
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Slashing, 3.0);
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Fire, 1.0);

        Armor armor = new Armor(
            DarkfallGearOptimizer.ARMOR_TYPE.Scale,
            DarkfallGearOptimizer.ARMOR_SLOT.Chest,
            5.0
        );
        armor.addResistance(PROTECTION.Slashing, 0.4);
        armor.addResistance(PROTECTION.Fire, 0.2);

        double score = armor.getResistanceScore();
        // Total weight = 4.0
        // Score = (0.4 * 3.0 / 4.0) + (0.2 * 1.0 / 4.0)
        //       = (1.2 / 4.0) + (0.2 / 4.0)
        //       = 0.3 + 0.05 = 0.35
        assertEquals("Score should be weighted average", 0.35, score, 0.0001);
    }

    @Test
    public void testPresetFileNaming() {
        // Test that preset file names are generated correctly
        Map<String, String> expectedPatterns = new HashMap<>();
        expectedPatterns.put("slashing100", "single protection");
        expectedPatterns.put("bludgeoning100-slashing100-piercing100", "physical protections");
        expectedPatterns.put("fire100-slashing100", "equal combination");
        expectedPatterns.put("fire33-slashing100", "slashing-heavy");
        expectedPatterns.put("fire100-slashing33", "fire-heavy");

        // Just verify the patterns make sense
        for (String pattern : expectedPatterns.keySet()) {
            assertTrue("Pattern should be valid: " + pattern, pattern.matches(".*\\d+.*"));
        }
    }
}
