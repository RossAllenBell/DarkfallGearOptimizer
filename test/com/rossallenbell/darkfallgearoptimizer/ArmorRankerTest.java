package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

public class ArmorRankerTest {
    
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
        fullPlateGloves = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Gauntlets,
                2);
        fullPlateGloves.addResistance(PROTECTION.Slashing, 0.6);
        infernalChest = new Armor(ARMOR_TYPE.Infernal, ARMOR_SLOT.Chest, 3);
        infernalChest.addResistance(PROTECTION.Slashing, 1.33);
    }
    
    @Test
    public void test() {
        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, scaleLegs, fullPlateArms,
                fullPlateGloves, infernalChest }));
        Set<ArmorSet> armorSets = new ArmorCombinator(armors).getArmorSets();
        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();
        assertEquals(3, winningSets.size());
        Iterator<ArmorSet> iterator = winningSets.iterator();
        assertEquals(5.90, iterator.next().encumbrance, 0);
        assertEquals(1.215, iterator.next().resistanceScore, 0);
        assertEquals(3, iterator.next().getSAArmorWithCounts().keySet().size());
    }

    @Test
    public void testGetWinningSets_VerifyParetoFrontier() {
        // Create sets where some dominate others
        // Verify dominated sets are excluded and all non-dominated sets are included
        Set<ArmorSet> armorSets = new HashSet<ArmorSet>();

        // Set 1: Light with low resistance (2, 0.5) - ON FRONTIER
        ArmorSet light = createArmorSet(2.0, 0.5);
        armorSets.add(light);

        // Set 2: Medium with medium resistance (5, 1.0) - ON FRONTIER
        ArmorSet medium = createArmorSet(5.0, 1.0);
        armorSets.add(medium);

        // Set 3: Heavy with high resistance (8, 1.5) - ON FRONTIER
        ArmorSet heavy = createArmorSet(8.0, 1.5);
        armorSets.add(heavy);

        // Set 4: Dominated by medium (6, 0.9) - heavier and weaker - DOMINATED
        ArmorSet dominated = createArmorSet(6.0, 0.9);
        armorSets.add(dominated);

        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();

        // Should have 3 winning sets (light, medium, heavy)
        assertEquals(3, winningSets.size());
        assertTrue(winningSets.contains(light));
        assertTrue(winningSets.contains(medium));
        assertTrue(winningSets.contains(heavy));
        assertTrue(!winningSets.contains(dominated));
    }

    @Test
    public void testGetWinningSets_SameEncumbrance() {
        // Two sets with same encumbrance, different resistance
        // Should keep only the higher resistance one
        Set<ArmorSet> armorSets = new HashSet<ArmorSet>();

        ArmorSet lower = createArmorSet(5.0, 1.0);
        ArmorSet higher = createArmorSet(5.0, 1.5);
        armorSets.add(lower);
        armorSets.add(higher);

        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();

        assertEquals(1, winningSets.size());
        assertTrue(winningSets.contains(higher));
        assertTrue(!winningSets.contains(lower));
    }

    @Test
    public void testGetWinningSets_SameResistance() {
        // Two sets with same resistance, different encumbrance
        // Should keep only the lighter one
        Set<ArmorSet> armorSets = new HashSet<ArmorSet>();

        ArmorSet lighter = createArmorSet(3.0, 1.0);
        ArmorSet heavier = createArmorSet(5.0, 1.0);
        armorSets.add(lighter);
        armorSets.add(heavier);

        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();

        assertEquals(1, winningSets.size());
        assertTrue(winningSets.contains(lighter));
        assertTrue(!winningSets.contains(heavier));
    }

    @Test
    public void testGetWinningSets_AllDominated() {
        // Create sets where one dominates all others
        // Should return only 1 winning set
        Set<ArmorSet> armorSets = new HashSet<ArmorSet>();

        ArmorSet best = createArmorSet(2.0, 2.0); // Lightest AND strongest
        ArmorSet dominated1 = createArmorSet(3.0, 1.5);
        ArmorSet dominated2 = createArmorSet(4.0, 1.0);
        ArmorSet dominated3 = createArmorSet(5.0, 0.5);

        armorSets.add(best);
        armorSets.add(dominated1);
        armorSets.add(dominated2);
        armorSets.add(dominated3);

        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();

        assertEquals(1, winningSets.size());
        assertTrue(winningSets.contains(best));
    }

    @Test
    public void testGetWinningSets_EmptyInput() {
        // Empty set should return empty winners
        Set<ArmorSet> armorSets = new HashSet<ArmorSet>();
        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();
        assertEquals(0, winningSets.size());
    }

    @Test
    public void testGetWinningSets_SingleSet() {
        // Single set should always be a winner
        Set<ArmorSet> armorSets = new HashSet<ArmorSet>();
        ArmorSet single = createArmorSet(5.0, 1.0);
        armorSets.add(single);

        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();

        assertEquals(1, winningSets.size());
        assertTrue(winningSets.contains(single));
    }

    @Test
    public void testGetWinningSets_ComplexDominance() {
        // Test more complex dominance scenario
        Set<ArmorSet> armorSets = new HashSet<ArmorSet>();

        // Create a proper Pareto frontier
        ArmorSet s1 = createArmorSet(1.0, 0.3); // Lightest, weakest - ON FRONTIER
        ArmorSet s2 = createArmorSet(2.0, 0.7); // - ON FRONTIER
        ArmorSet s3 = createArmorSet(3.0, 1.0); // - ON FRONTIER
        ArmorSet s4 = createArmorSet(4.0, 1.2); // - ON FRONTIER
        ArmorSet s5 = createArmorSet(5.0, 1.5); // Heaviest, strongest - ON FRONTIER

        // Add dominated sets (heavier but weaker than frontier points)
        ArmorSet d1 = createArmorSet(2.5, 0.6); // Dominated by s2 (2.0, 0.7)
        ArmorSet d2 = createArmorSet(4.5, 1.1); // Dominated by s4 (4.0, 1.2)

        armorSets.add(s1);
        armorSets.add(s2);
        armorSets.add(s3);
        armorSets.add(s4);
        armorSets.add(s5);
        armorSets.add(d1);
        armorSets.add(d2);

        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();

        assertEquals(5, winningSets.size());
        assertTrue(winningSets.contains(s1));
        assertTrue(winningSets.contains(s2));
        assertTrue(winningSets.contains(s3));
        assertTrue(winningSets.contains(s4));
        assertTrue(winningSets.contains(s5));
        assertTrue(!winningSets.contains(d1));
        assertTrue(!winningSets.contains(d2));
    }

    /**
     * Helper method to create an ArmorSet with specific encumbrance and resistance
     * Creates unique armor pieces so sets are distinguishable by HashSet
     */
    private static int uniqueId = 0;
    private ArmorSet createArmorSet(double encumbrance, double resistance) {
        ArmorSet set = new ArmorSet();

        // Create a unique armor piece for this set so they don't all hash to the same value
        // Use a unique ARMOR_TYPE for each call to ensure uniqueness
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
