package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class ParallelArmorCombinatorTest {

    @Test
    public void testParallel_MatchesSequential_SmallDataset() {
        // Test with small synthetic dataset
        Armor scaleArms = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Arms, 1);
        scaleArms.addResistance(PROTECTION.Slashing, 0.25);
        Armor scaleGloves = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Gauntlets, 1);
        scaleGloves.addResistance(PROTECTION.Slashing, 0.25);
        Armor scaleLegs = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Legs, 0.9);
        scaleLegs.addResistance(PROTECTION.Slashing, 0.25);
        Armor fullPlateArms = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Arms, 2);
        fullPlateArms.addResistance(PROTECTION.Slashing, 0.6);
        Armor fullPlateGloves = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Gauntlets, 2);
        fullPlateGloves.addResistance(PROTECTION.Slashing, 0.6);
        Armor infernalChest = new Armor(ARMOR_TYPE.Infernal, ARMOR_SLOT.Chest, 3);
        infernalChest.addResistance(PROTECTION.Slashing, 1.33);

        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, scaleLegs, fullPlateArms,
                fullPlateGloves, infernalChest }));

        // Sequential
        Collection<ArmorSet> sequential = new ParallelArmorCombinator(armors, 1).getOptimalArmorSets();

        // Parallel with 2 threads
        Collection<ArmorSet> parallel2 = new ParallelArmorCombinator(armors, 2).getOptimalArmorSets();

        // Parallel with 4 threads
        Collection<ArmorSet> parallel4 = new ParallelArmorCombinator(armors, 4).getOptimalArmorSets();

        // All should match
        assertResultsMatch(sequential, parallel2, "2 threads");
        assertResultsMatch(sequential, parallel4, "4 threads");
    }

    @Test
    public void testParallel_Determinism() {
        // Run same workload 10 times - should get identical results every time
        Armor scaleArms = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Arms, 1);
        scaleArms.addResistance(PROTECTION.Slashing, 0.25);
        Armor scaleGloves = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Gauntlets, 1);
        scaleGloves.addResistance(PROTECTION.Slashing, 0.25);
        Armor scaleLegs = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Legs, 0.9);
        scaleLegs.addResistance(PROTECTION.Slashing, 0.25);
        Armor fullPlateArms = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Arms, 2);
        fullPlateArms.addResistance(PROTECTION.Slashing, 0.6);
        Armor fullPlateGloves = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Gauntlets, 2);
        fullPlateGloves.addResistance(PROTECTION.Slashing, 0.6);
        Armor infernalChest = new Armor(ARMOR_TYPE.Infernal, ARMOR_SLOT.Chest, 3);
        infernalChest.addResistance(PROTECTION.Slashing, 1.33);

        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, scaleLegs, fullPlateArms,
                fullPlateGloves, infernalChest }));

        Collection<ArmorSet> baseline = new ParallelArmorCombinator(armors, 4).getOptimalArmorSets();

        for (int i = 0; i < 10; i++) {
            Collection<ArmorSet> result = new ParallelArmorCombinator(armors, 4).getOptimalArmorSets();
            assertResultsMatch(baseline, result, "Iteration " + i);
        }
    }

    @Test
    public void testParallel_SingleThread() {
        // Test that single thread mode works
        Armor scaleArms = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Arms, 1);
        scaleArms.addResistance(PROTECTION.Slashing, 0.25);
        Armor fullPlateArms = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Arms, 2);
        fullPlateArms.addResistance(PROTECTION.Slashing, 0.6);

        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, fullPlateArms }));

        Collection<ArmorSet> result = new ParallelArmorCombinator(armors, 1).getOptimalArmorSets();
        assertEquals(2, result.size());
    }

    @Test
    public void testParallel_MultipleThreadCounts() {
        // Verify different thread counts all produce same results
        Armor scaleArms = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Arms, 1);
        scaleArms.addResistance(PROTECTION.Slashing, 0.25);
        Armor scaleGloves = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Gauntlets, 1);
        scaleGloves.addResistance(PROTECTION.Slashing, 0.25);
        Armor scaleLegs = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Legs, 0.9);
        scaleLegs.addResistance(PROTECTION.Slashing, 0.25);
        Armor fullPlateArms = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Arms, 2);
        fullPlateArms.addResistance(PROTECTION.Slashing, 0.6);
        Armor fullPlateGloves = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Gauntlets, 2);
        fullPlateGloves.addResistance(PROTECTION.Slashing, 0.6);
        Armor infernalChest = new Armor(ARMOR_TYPE.Infernal, ARMOR_SLOT.Chest, 3);
        infernalChest.addResistance(PROTECTION.Slashing, 1.33);

        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, scaleLegs, fullPlateArms,
                fullPlateGloves, infernalChest }));

        Collection<ArmorSet> baseline = new ParallelArmorCombinator(armors, 1).getOptimalArmorSets();

        for (int threads : new int[]{2, 3, 4, 8}) {
            Collection<ArmorSet> result = new ParallelArmorCombinator(armors, threads).getOptimalArmorSets();
            assertResultsMatch(baseline, result, threads + " threads");
        }
    }

    /**
     * Helper method to verify two result sets match exactly
     */
    private void assertResultsMatch(Collection<ArmorSet> expected, Collection<ArmorSet> actual, String context) {
        assertEquals(context + ": Result count should match",
                expected.size(), actual.size());

        // Convert to encumbrance->resistance maps for comparison
        Map<Double, Double> expectedMap = toEncumbranceResistanceMap(expected);
        Map<Double, Double> actualMap = toEncumbranceResistanceMap(actual);

        // Verify each encumbrance/resistance pair matches
        for (Map.Entry<Double, Double> entry : expectedMap.entrySet()) {
            double encumbrance = entry.getKey();
            double expectedResistance = entry.getValue();

            assertTrue(context + ": Missing encumbrance " + encumbrance + " in new results",
                    actualMap.containsKey(encumbrance));

            double actualResistance = actualMap.get(encumbrance);
            assertEquals(context + ": Resistance mismatch at encumbrance " + encumbrance,
                    expectedResistance, actualResistance, 0.001);
        }

        // Verify order is the same (sorted by encumbrance)
        Iterator<ArmorSet> expectedIter = expected.iterator();
        Iterator<ArmorSet> actualIter = actual.iterator();
        int index = 0;
        while (expectedIter.hasNext() && actualIter.hasNext()) {
            ArmorSet expectedSet = expectedIter.next();
            ArmorSet actualSet = actualIter.next();

            assertEquals(context + ": Encumbrance mismatch at index " + index,
                    expectedSet.getEncumbrance(), actualSet.getEncumbrance(), 0.001);
            assertEquals(context + ": Resistance mismatch at index " + index,
                    expectedSet.getResistanceScore(), actualSet.getResistanceScore(), 0.001);
            index++;
        }
    }

    /**
     * Convert a collection of ArmorSets to a map of encumbrance -> resistance
     */
    private Map<Double, Double> toEncumbranceResistanceMap(Collection<ArmorSet> sets) {
        Map<Double, Double> map = new HashMap<>();
        for (ArmorSet set : sets) {
            map.put(set.getEncumbrance(), set.getResistanceScore());
        }
        return map;
    }
}
