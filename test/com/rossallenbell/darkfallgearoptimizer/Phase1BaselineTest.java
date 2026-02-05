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

/**
 * CRITICAL: Baseline comparison tests for Phase 1.
 * These tests verify that the new getOptimalArmorSets() method produces
 * identical results to the old getArmorSets() + ArmorRanker approach.
 */
public class Phase1BaselineTest {

    @Test
    public void testPhase1_MatchesOldImplementation_SmallDataset() {
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

        ArmorCombinator combinator = new ArmorCombinator(armors);

        // Old approach: getArmorSets() + ArmorRanker
        Set<ArmorSet> oldSets = combinator.getArmorSets();
        Collection<ArmorSet> oldResults = new ArmorRanker(oldSets).getWinningSets();

        // New approach: getOptimalArmorSets()
        Collection<ArmorSet> newResults = combinator.getOptimalArmorSets();

        // Verify results match
        assertResultsMatch(oldResults, newResults, "SmallDataset");
    }

    @Test
    public void testPhase1_DeduplicationStillWorks() {
        // Verify slot-agnostic deduplication works in new implementation
        Armor scaleArms = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Arms, 1);
        scaleArms.addResistance(PROTECTION.Slashing, 0.25);
        Armor scaleGloves = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Gauntlets, 1);
        scaleGloves.addResistance(PROTECTION.Slashing, 0.25);
        Armor fullPlateArms = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Arms, 2);
        fullPlateArms.addResistance(PROTECTION.Slashing, 0.6);
        Armor fullPlateGloves = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Gauntlets, 2);
        fullPlateGloves.addResistance(PROTECTION.Slashing, 0.6);

        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, fullPlateArms, fullPlateGloves }));

        ArmorCombinator combinator = new ArmorCombinator(armors);
        Collection<ArmorSet> results = combinator.getOptimalArmorSets();

        // Should have 3 unique sets (all scale, all fullplate, mixed)
        // not 4 (which would happen if deduplication failed)
        assertEquals(3, results.size());
    }

    @Test
    public void testPhase1_ParetoFilteringStillWorks() {
        // Create scenario where some sets dominate others
        ParetoDeduplicatingFilter filter = new ParetoDeduplicatingFilter();

        ArmorSet light = createArmorSet(2.0, 0.5);
        ArmorSet medium = createArmorSet(3.0, 1.0);
        ArmorSet heavy = createArmorSet(4.0, 1.5);
        ArmorSet dominated = createArmorSet(3.5, 0.9); // Heavier than medium, weaker

        filter.tryAdd(light);
        filter.tryAdd(medium);
        filter.tryAdd(heavy);
        filter.tryAdd(dominated);

        Collection<ArmorSet> winners = filter.getWinningSets();
        assertEquals(3, winners.size());
        assertTrue(winners.contains(light));
        assertTrue(winners.contains(medium));
        assertTrue(winners.contains(heavy));
        assertTrue(!winners.contains(dominated));
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

    /**
     * Helper to create unique armor sets with specific encumbrance and resistance
     */
    private static int uniqueId = 0;
    private ArmorSet createArmorSet(double encumbrance, double resistance) {
        ArmorSet set = new ArmorSet();

        ARMOR_TYPE[] types = ARMOR_TYPE.values();
        ARMOR_TYPE type = types[uniqueId % types.length];
        ARMOR_SLOT slot = ARMOR_SLOT.values()[uniqueId % ARMOR_SLOT.values().length];

        Armor armor = new Armor(type, slot, encumbrance);
        armor.addResistance(PROTECTION.Slashing, resistance);
        set.addArmor(armor);
        uniqueId++;

        return set;
    }
}
