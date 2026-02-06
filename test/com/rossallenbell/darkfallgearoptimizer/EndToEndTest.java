package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;
import com.rossallenbell.darkfallgearoptimizer.data.CsvArmorProvider;

public class EndToEndTest {

    Armor scaleArms;
    Armor scaleGloves;
    Armor scaleLegs;
    Armor fullPlateArms;
    Armor fullPlateGloves;
    Armor infernalChest;

    @Before
    public void setUp() {
        // Configure protection weights for tests (match original defaults)
        DarkfallGearOptimizer.protectionWeights.clear();
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Slashing, 1.0);
        DarkfallGearOptimizer.protectionWeights.put(PROTECTION.Fire, 1.0);

        scaleArms = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Arms, 1);
        scaleArms.addResistance(PROTECTION.Slashing, 0.25);
        scaleGloves = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Gauntlets, 1);
        scaleGloves.addResistance(PROTECTION.Slashing, 0.25);
        scaleLegs = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Legs, 0.9);
        scaleLegs.addResistance(PROTECTION.Slashing, 0.25);
        fullPlateArms = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Arms, 2);
        fullPlateArms.addResistance(PROTECTION.Slashing, 0.6);
        fullPlateGloves = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Gauntlets, 2);
        fullPlateGloves.addResistance(PROTECTION.Slashing, 0.6);
        infernalChest = new Armor(ARMOR_TYPE.Infernal, ARMOR_SLOT.Chest, 3);
        infernalChest.addResistance(PROTECTION.Slashing, 1.33);
    }

    @Test
    public void testCompleteFlow_SmallDataset() {
        // Use small synthetic dataset - fast enough for automated tests
        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, scaleLegs, fullPlateArms,
                fullPlateGloves, infernalChest }));

        // Run complete flow: combine → rank
        ArmorCombinator combinator = new ArmorCombinator(armors);
        Set<ArmorSet> armorSets = combinator.getArmorSets();
        assertEquals(3, armorSets.size());

        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();
        assertEquals(3, winningSets.size());

        // Verify winning sets form a proper Pareto frontier
        // (each subsequent set should be heavier with higher resistance)
        ArmorSet previous = null;
        for (ArmorSet current : winningSets) {
            if (previous != null) {
                assertTrue("Encumbrance should increase along frontier",
                        current.getEncumbrance() > previous.getEncumbrance());
                assertTrue("Resistance should increase along frontier",
                        current.getResistanceScore() > previous.getResistanceScore());
            }
            previous = current;
        }
    }

    @Test
    public void testCompleteFlow_VerifyNoDominatedSets() {
        // Verify that no winning set dominates another
        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, scaleLegs, fullPlateArms,
                fullPlateGloves, infernalChest }));
        ArmorCombinator combinator = new ArmorCombinator(armors);
        Set<ArmorSet> armorSets = combinator.getArmorSets();
        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();

        // Check every pair of winning sets
        for (ArmorSet set1 : winningSets) {
            for (ArmorSet set2 : winningSets) {
                if (set1 != set2) {
                    // Neither should dominate the other
                    boolean set1Dominates = set1.getEncumbrance() <= set2.getEncumbrance()
                            && set1.getResistanceScore() >= set2.getResistanceScore()
                            && (set1.getEncumbrance() < set2.getEncumbrance()
                                    || set1.getResistanceScore() > set2.getResistanceScore());

                    boolean set2Dominates = set2.getEncumbrance() <= set1.getEncumbrance()
                            && set2.getResistanceScore() >= set1.getResistanceScore()
                            && (set2.getEncumbrance() < set1.getEncumbrance()
                                    || set2.getResistanceScore() > set1.getResistanceScore());

                    assertTrue("No winning set should dominate another", !set1Dominates && !set2Dominates);
                }
            }
        }
    }

    @Test
    public void testCompleteFlow_ConsistentResults() {
        // Verify that running the same flow twice produces identical results
        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, scaleLegs, fullPlateArms,
                fullPlateGloves, infernalChest }));

        // First run
        ArmorCombinator combinator1 = new ArmorCombinator(armors);
        Set<ArmorSet> armorSets1 = combinator1.getArmorSets();
        ArmorRanker ranker1 = new ArmorRanker(armorSets1);
        Collection<ArmorSet> winningSets1 = ranker1.getWinningSets();

        // Second run
        ArmorCombinator combinator2 = new ArmorCombinator(armors);
        Set<ArmorSet> armorSets2 = combinator2.getArmorSets();
        ArmorRanker ranker2 = new ArmorRanker(armorSets2);
        Collection<ArmorSet> winningSets2 = ranker2.getWinningSets();

        // Results should be identical
        assertEquals("Should generate same number of sets", armorSets1.size(), armorSets2.size());
        assertEquals("Should have same number of winners", winningSets1.size(), winningSets2.size());

        // Verify encumbrance and resistance values match
        Iterator<ArmorSet> iter1 = winningSets1.iterator();
        Iterator<ArmorSet> iter2 = winningSets2.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            ArmorSet set1 = iter1.next();
            ArmorSet set2 = iter2.next();
            assertEquals("Encumbrance should match", set1.getEncumbrance(), set2.getEncumbrance(), 0.001);
            assertEquals("Resistance should match", set1.getResistanceScore(), set2.getResistanceScore(), 0.001);
        }
    }

    @Test
    public void testMinimalDataset() throws FileNotFoundException, IOException {
        // Test with the actual minimal dataset file
        CsvArmorProvider provider = new CsvArmorProvider();
        Set<Armor> armors = provider.readFilePath("./data/armor-data-minimal.csv");

        assertFalse("Minimal dataset should not be empty", armors.isEmpty());

        // Run the optimized algorithm
        ArmorCombinator combinator = new ArmorCombinator(armors);
        Collection<ArmorSet> winningSets = combinator.getOptimalArmorSets();

        assertFalse("Should produce some optimal sets", winningSets.isEmpty());

        // Verify Pareto optimality
        for (ArmorSet set1 : winningSets) {
            for (ArmorSet set2 : winningSets) {
                if (set1 != set2) {
                    // Neither should dominate the other
                    boolean set1Dominates = set1.getEncumbrance() <= set2.getEncumbrance()
                            && set1.getResistanceScore() >= set2.getResistanceScore()
                            && (set1.getEncumbrance() < set2.getEncumbrance()
                                    || set1.getResistanceScore() > set2.getResistanceScore());

                    assertFalse("No winning set should dominate another in minimal dataset", set1Dominates);
                }
            }
        }
    }
}
