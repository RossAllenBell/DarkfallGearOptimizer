package com.rossallenbell.darkfallgearoptimizer.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.rossallenbell.darkfallgearoptimizer.Armor;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class CsvArmorProviderTest {

	String data = "Type,Slot,Name,Skill,Mastery,Leather,Iron Ingot,Selentine Ingot,Theyril Ingot,Leenspar Ingot,Gold,Encumbrance,Bludgeoning,Piercing,Slashing,Acid,Cold,Fire,Holy,Lightning,Unholy,Impact,Fiend Claw,Ratka,Dragon Scales\n"
			+ "NoArmor,Boots,Boots,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\n"
			+ "NoArmor,Gauntlets,Gauntlets,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\n"
			+ "Chain,Arms,Sleeves,1,0,1,2,0,0,0,0,5,0.34,0.38,0.43,0.78,0.78,0.78,0.78,0.57,0.78,0.43,0,0,0";
	
	Armor armorInData1;
	Armor armorInData2;
	Armor armorInData3;
	Armor armorNotInData1;
	
	@Before
	public void setUp() {
	    armorInData1 = new Armor(ARMOR_TYPE.NoArmor, ARMOR_SLOT.Boots, 0);
	    armorInData2 = new Armor(ARMOR_TYPE.NoArmor, ARMOR_SLOT.Gauntlets, 0);
	    for(PROTECTION protection : PROTECTION.values()){
	        armorInData1.addResistance(protection, 0d);
	        armorInData2.addResistance(protection, 0d);
	    }
	    
	    armorInData3 = new Armor(ARMOR_TYPE.Chain, ARMOR_SLOT.Arms, 5);
	    armorInData3.addResistance(PROTECTION.Bludgeoning, 0.34);
	    armorInData3.addResistance(PROTECTION.Piercing, 0.38);
	    armorInData3.addResistance(PROTECTION.Slashing, 0.43);
	    armorInData3.addResistance(PROTECTION.Acid, 0.78);
	    armorInData3.addResistance(PROTECTION.Cold, 0.78);
	    armorInData3.addResistance(PROTECTION.Fire, 0.78);
	    armorInData3.addResistance(PROTECTION.Holy, 0.78);
	    armorInData3.addResistance(PROTECTION.Lightning, 0.57);
	    armorInData3.addResistance(PROTECTION.Unholy, 0.78);
	    armorInData3.addResistance(PROTECTION.Impact, 0.43);
	    
	    armorNotInData1 = new Armor(ARMOR_TYPE.Chain, ARMOR_SLOT.Boots, 5);
        armorNotInData1.addResistance(PROTECTION.Bludgeoning, 0.34);
        armorNotInData1.addResistance(PROTECTION.Piercing, 0.38);
        armorNotInData1.addResistance(PROTECTION.Slashing, 0.43);
        armorNotInData1.addResistance(PROTECTION.Acid, 0.78);
        armorNotInData1.addResistance(PROTECTION.Cold, 0.78);
        armorNotInData1.addResistance(PROTECTION.Fire, 0.78);
        armorNotInData1.addResistance(PROTECTION.Holy, 0.78);
        armorNotInData1.addResistance(PROTECTION.Lightning, 0.57);
        armorNotInData1.addResistance(PROTECTION.Unholy, 0.78);
        armorNotInData1.addResistance(PROTECTION.Impact, 0.43);
	}

	@Test
	public void testReadFileBufferedReader() throws IOException {
		CsvArmorProvider csvArmorProvider = new CsvArmorProvider();
		Set<Armor> results = csvArmorProvider.read(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data.getBytes()))));
		assertTrue(results.contains(armorInData1));
		assertTrue(results.contains(armorInData2));
		assertTrue(results.contains(armorInData3));
		assertFalse(results.contains(armorNotInData1));
	}
}
