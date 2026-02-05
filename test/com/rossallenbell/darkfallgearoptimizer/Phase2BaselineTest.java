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
 * CRITICAL: Baseline comparison tests for Phase 2.
 * These tests verify that StreamingParetoFilter produces identical results
 * to ParetoDeduplicatingFilter (Phase 1).
 */
public class Phase2BaselineTest {

    @Test
    public void testPhase2_MatchesPhase1_SmallDataset() {
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

        // Phase 1: ParetoDeduplicatingFilter
        ParetoDeduplicatingFilter filter1 = new ParetoDeduplicatingFilter();
        // Generate all combinations and filter with Phase 1
        ArmorCombinator combinator = new ArmorCombinator(armors);
        Map<ARMOR_SLOT, java.util.List<Armor>> slotBuckets = combinator.getSlotBuckets();
        java.util.List<ARMOR_SLOT> slots = new java.util.ArrayList<ARMOR_SLOT>(slotBuckets.keySet());
        Map<ARMOR_SLOT, Integer> slotPointers = new HashMap<ARMOR_SLOT, Integer>();
        for(ARMOR_SLOT slot : slots){
            slotPointers.put(slot, 0);
        }
        ARMOR_SLOT mostSignificantSlot = slots.get(slots.size()-1);
        while(slotPointers.get(mostSignificantSlot) < slotBuckets.get(mostSignificantSlot).size()){
            ArmorSet armorSet = new ArmorSet();
            for(ARMOR_SLOT slot : slots){
                armorSet.addArmor(slotBuckets.get(slot).get(slotPointers.get(slot)));
            }
            filter1.tryAdd(armorSet);
            slotPointers.put(slots.get(0), slotPointers.get(slots.get(0)) + 1);
            for(int i=0; i<slots.size() - 1; i++){
                ARMOR_SLOT slot = slots.get(i);
                if(slotPointers.get(slot) >= slotBuckets.get(slot).size()){
                    slotPointers.put(slot, 0);
                    ARMOR_SLOT nextSlot = slots.get(i + 1);
                    slotPointers.put(nextSlot, slotPointers.get(nextSlot) + 1);
                } else {
                    break;
                }
            }
        }
        Collection<ArmorSet> phase1Results = filter1.getWinningSets();

        // Phase 2: StreamingParetoFilter (via getOptimalArmorSets)
        Collection<ArmorSet> phase2Results = new ArmorCombinator(armors).getOptimalArmorSets();

        // Verify results match
        assertResultsMatch(phase1Results, phase2Results, "SmallDataset");
    }

    @Test
    public void testPhase2_HashCollisionRate() {
        // Monitor collision rate during test run
        StreamingParetoFilter filter = new StreamingParetoFilter();

        // Create 1000 unique sets
        for (int i = 0; i < 1000; i++) {
            ArmorSet set = createArmorSet(i + 1.0, i * 0.5);
            filter.tryAdd(set);
        }

        double collisionRate = filter.getCollisionRate();
        assertTrue("Collision rate too high: " + collisionRate + "%", collisionRate < 1.0);
    }

    @Test
    public void testPhase2_DeduplicationWithHashes() {
        // Verify hash-based deduplication works correctly
        StreamingParetoFilter filter = new StreamingParetoFilter();

        Armor scaleArms = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Arms, 1);
        scaleArms.addResistance(PROTECTION.Slashing, 0.25);
        Armor scaleGloves = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Gauntlets, 1);
        scaleGloves.addResistance(PROTECTION.Slashing, 0.25);
        Armor fullPlateArms = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Arms, 2);
        fullPlateArms.addResistance(PROTECTION.Slashing, 0.6);
        Armor fullPlateGloves = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Gauntlets, 2);
        fullPlateGloves.addResistance(PROTECTION.Slashing, 0.6);

        // Create two slot-agnostic duplicates
        ArmorSet set1 = new ArmorSet();
        set1.addArmor(scaleArms);
        set1.addArmor(fullPlateGloves);

        ArmorSet set2 = new ArmorSet();
        set2.addArmor(fullPlateArms);
        set2.addArmor(scaleGloves);

        assertTrue("First set should be added", filter.tryAdd(set1));
        assertTrue("Duplicate should be detected via hash", !filter.tryAdd(set2));
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
