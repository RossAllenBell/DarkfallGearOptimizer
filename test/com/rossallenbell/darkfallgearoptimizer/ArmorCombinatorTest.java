package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class ArmorCombinatorTest {
    
    Armor scaleArms;
    Armor scaleGloves;
    Armor scaleLegs;
    Armor fullPlateArms;
    Armor fullPlateGloves;
    Armor infernalChest;
    
    @Before
    public void setUp() {
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
    public void testGetSlotBuckets() {
        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, scaleLegs, fullPlateArms,
                fullPlateGloves, infernalChest }));
        Map<ARMOR_SLOT, List<Armor>> slotBuckets = new ArmorCombinator(armors)
                .getSlotBuckets();
        assertEquals(slotBuckets.keySet().size(), 4);
        assertEquals(2, slotBuckets.get(ARMOR_SLOT.Arms).size());
        assertEquals(2, slotBuckets.get(ARMOR_SLOT.Gauntlets).size());
        assertEquals(1, slotBuckets.get(ARMOR_SLOT.Legs).size());
        assertTrue(slotBuckets.get(ARMOR_SLOT.Gauntlets).contains(
                fullPlateGloves));
    }
    
    @Test
    public void testGetArmorSets(){
        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, scaleLegs, fullPlateArms,
                fullPlateGloves, infernalChest }));
        ArmorCombinator combinator = new ArmorCombinator(armors);
        Set<ArmorSet> armorSets = combinator.getArmorSets();
        assertEquals(3, armorSets.size());
    }
    
}
