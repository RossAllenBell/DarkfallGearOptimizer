package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;
import com.rossallenbell.darkfallgearoptimizer.data.CsvArmorProvider;

public class ArmorSetTest {
    
    String data = "Type,Slot,Name,Skill,Mastery,Cloth,Bone,Leather,Iron,Selentine,Theyril,Leenspar,Gold,Encumbrance,Bludgeoning,Piercing,Slashing,Acid,Cold,Fire,Holy,Lightning,Unholy,Impact,FiendClaw,Ratka,DragonScales\n"
            + "NoArmor,Chest,Cuirass,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\n"
            + "Chain,Arms,Sleeves,1,0,0,0,1,2,0,0,0,0,5,0.34,0.38,0.43,0.78,0.78,0.78,0.78,0.57,0.78,0.43,0,0,0\n"
            + "Banded,Elbows,Elbow Guards,25,0,0,0,1,3,0,0,0,5,6,0.4,0.45,0.5,0.91,0.91,0.91,0.91,0.67,0.91,0.5,0,0,0";
    
    Set<Armor> armors;
    
    @Before
    public void setUp() throws IOException {
        CsvArmorProvider csvArmorProvider = new CsvArmorProvider();
        armors = csvArmorProvider
                .read(new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(data.getBytes()))));
    }
    
    @Test
    public void testAddArmor() {
        ArmorSet armorSet = new ArmorSet();
        for (Armor armor : armors) {
            armorSet.addArmor(armor);
        }
        assertEquals(11, armorSet.getEncumbrance(), 0);
        assertEquals(1.31, armorSet.getResistanceScore(), 0.01);
    }
    
    @Test
    public void testSlotAgnosticism(){
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
        
        ArmorSet armorSet1 = new ArmorSet();
        armorSet1.addArmor(scaleLegs);
        armorSet1.addArmor(scaleArms);
        armorSet1.addArmor(fullPlateGloves);
        armorSet1.addArmor(infernalChest);
        
        ArmorSet armorSet2 = new ArmorSet();
        armorSet2.addArmor(scaleLegs);
        armorSet2.addArmor(scaleGloves);
        armorSet2.addArmor(fullPlateArms);
        armorSet2.addArmor(infernalChest);
        
        ArmorSet armorSet3 = new ArmorSet();
        armorSet3.addArmor(scaleLegs);
        armorSet3.addArmor(fullPlateGloves);
        armorSet3.addArmor(fullPlateArms);
        armorSet3.addArmor(infernalChest);
        
        assertEquals(armorSet1.hashCode(), armorSet2.hashCode());
        assertTrue(armorSet1.hashCode() != armorSet3.hashCode());
        assertTrue(armorSet1.equals(armorSet2));
        assertTrue(!armorSet1.equals(armorSet3));
        
    }
    
}
