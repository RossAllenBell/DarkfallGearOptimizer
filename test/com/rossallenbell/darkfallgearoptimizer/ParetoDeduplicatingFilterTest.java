package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class ParetoDeduplicatingFilterTest {

    Armor scaleArms;
    Armor scaleGloves;
    Armor fullPlateArms;
    Armor fullPlateGloves;

    @Before
    public void setUp() {
        scaleArms = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Arms, 1);
        scaleArms.addResistance(PROTECTION.Slashing, 0.25);
        scaleGloves = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Gauntlets, 1);
        scaleGloves.addResistance(PROTECTION.Slashing, 0.25);
        fullPlateArms = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Arms, 2);
        fullPlateArms.addResistance(PROTECTION.Slashing, 0.6);
        fullPlateGloves = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Gauntlets, 2);
        fullPlateGloves.addResistance(PROTECTION.Slashing, 0.6);
    }

    @Test
    public void testTryAdd_NewUniqueDominating() {
        // Adding a new non-dominated set should return true
        ParetoDeduplicatingFilter filter = new ParetoDeduplicatingFilter();

        ArmorSet set1 = new ArmorSet();
        set1.addArmor(scaleArms);

        assertTrue("First set should be added", filter.tryAdd(set1));
        assertEquals(1, filter.getWinningSets().size());
    }

    @Test
    public void testTryAdd_Duplicate() {
        // Adding same slot-agnostic combination twice should return false second time
        ParetoDeduplicatingFilter filter = new ParetoDeduplicatingFilter();

        // Create two sets with same slot-agnostic composition
        ArmorSet set1 = new ArmorSet();
        set1.addArmor(scaleArms);
        set1.addArmor(fullPlateGloves);

        ArmorSet set2 = new ArmorSet();
        set2.addArmor(scaleArms);
        set2.addArmor(fullPlateGloves);

        assertTrue("First set should be added", filter.tryAdd(set1));
        assertFalse("Duplicate set should not be added", filter.tryAdd(set2));
        assertEquals(1, filter.getWinningSets().size());
    }

    @Test
    public void testTryAdd_SlotAgnosticDuplicate() {
        // Scale arms + fullplate gloves = fullplate arms + scale gloves (slot-agnostic)
        ParetoDeduplicatingFilter filter = new ParetoDeduplicatingFilter();

        ArmorSet set1 = new ArmorSet();
        set1.addArmor(scaleArms);
        set1.addArmor(fullPlateGloves);

        ArmorSet set2 = new ArmorSet();
        set2.addArmor(fullPlateArms);
        set2.addArmor(scaleGloves);

        assertTrue("First set should be added", filter.tryAdd(set1));
        assertFalse("Slot-agnostic duplicate should not be added", filter.tryAdd(set2));
        assertEquals(1, filter.getWinningSets().size());
    }

    @Test
    public void testTryAdd_Dominated() {
        // Adding a dominated set should return false
        ParetoDeduplicatingFilter filter = new ParetoDeduplicatingFilter();

        // Add lighter set with good resistance first
        ArmorSet light = createArmorSet(2.0, 1.0);
        assertTrue("Light set should be added", filter.tryAdd(light));

        // Try to add heavier set with worse resistance (dominated)
        ArmorSet dominated = createArmorSet(3.0, 0.8);
        assertFalse("Dominated set should not be added", filter.tryAdd(dominated));
        assertEquals(1, filter.getWinningSets().size());
    }

    @Test
    public void testTryAdd_DominatesExisting() {
        // Adding a set that dominates existing should remove old and add new
        ParetoDeduplicatingFilter filter = new ParetoDeduplicatingFilter();

        // Add heavier set first
        ArmorSet heavy = createArmorSet(3.0, 0.8);
        assertTrue("Heavy set should be added", filter.tryAdd(heavy));

        // Add lighter set with better resistance (dominates heavy)
        ArmorSet dominating = createArmorSet(2.0, 1.0);
        assertTrue("Dominating set should be added", filter.tryAdd(dominating));

        Collection<ArmorSet> winners = filter.getWinningSets();
        assertEquals(1, winners.size());
        assertTrue(winners.contains(dominating));
        assertFalse(winners.contains(heavy));
    }

    @Test
    public void testTryAdd_BothOnFrontier() {
        // Two sets that don't dominate each other should both be kept
        ParetoDeduplicatingFilter filter = new ParetoDeduplicatingFilter();

        ArmorSet light = createArmorSet(2.0, 0.5);
        ArmorSet heavy = createArmorSet(3.0, 1.0);

        assertTrue("Light set should be added", filter.tryAdd(light));
        assertTrue("Heavy set should be added", filter.tryAdd(heavy));
        assertEquals(2, filter.getWinningSets().size());
    }

    @Test
    public void testTryAdd_SameEncumbrance() {
        // Two sets with same encumbrance - keep only higher resistance
        ParetoDeduplicatingFilter filter = new ParetoDeduplicatingFilter();

        ArmorSet lower = createArmorSet(5.0, 1.0);
        ArmorSet higher = createArmorSet(5.0, 1.5);

        assertTrue("First set should be added", filter.tryAdd(lower));
        assertTrue("Higher resistance set should be added", filter.tryAdd(higher));

        Collection<ArmorSet> winners = filter.getWinningSets();
        assertEquals(1, winners.size());
        assertTrue(winners.contains(higher));
        assertFalse(winners.contains(lower));
    }

    @Test
    public void testTryAdd_ComplexScenario() {
        // Test with multiple sets forming a frontier
        ParetoDeduplicatingFilter filter = new ParetoDeduplicatingFilter();

        ArmorSet s1 = createArmorSet(1.0, 0.3);
        ArmorSet s2 = createArmorSet(2.0, 0.7);
        ArmorSet s3 = createArmorSet(3.0, 1.0);
        ArmorSet dominated = createArmorSet(2.5, 0.6);

        assertTrue(filter.tryAdd(s1));
        assertTrue(filter.tryAdd(s2));
        assertTrue(filter.tryAdd(s3));
        assertFalse("Dominated set should not be added", filter.tryAdd(dominated));

        assertEquals(3, filter.getWinningSets().size());
    }

    @Test
    public void testGetWinningSets_Empty() {
        ParetoDeduplicatingFilter filter = new ParetoDeduplicatingFilter();
        assertEquals(0, filter.getWinningSets().size());
    }

    /**
     * Helper to create unique armor sets with specific encumbrance and resistance
     */
    private static int uniqueId = 0;
    private ArmorSet createArmorSet(double encumbrance, double resistance) {
        ArmorSet set = new ArmorSet();

        // Create unique armor piece so sets are distinguishable
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
