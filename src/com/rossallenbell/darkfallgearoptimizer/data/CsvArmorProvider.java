package com.rossallenbell.darkfallgearoptimizer.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.rossallenbell.darkfallgearoptimizer.Armor;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class CsvArmorProvider {
	
	private static final String TYPE = "Type";
	private static final String SLOT = "Slot";
	private static final String ENCUMBRANCE = "Encumbrance";
	private static final String BLUDGEONING = "Bludgeoning";
	private static final String PIERCING = "Piercing";
	private static final String SLASHING = "Slashing";
	private static final String ACID = "Acid";
	private static final String COLD = "Cold";
	private static final String FIRE = "Fire";
	private static final String HOLY = "Holy";
	private static final String LIGHTNING = "Lightning";
	private static final String UNHOLY = "Unholy";
	private static final String IMPACT = "Impact";
	private static final String[] KNOWN_COLUMNS = { TYPE, SLOT, ENCUMBRANCE, BLUDGEONING, PIERCING, SLASHING, ACID, COLD, FIRE, HOLY, LIGHTNING, UNHOLY, IMPACT };
		
	public Set<Armor> readFilePath(String path) throws IOException {
		File file = new File(path);
		return readFile(file);
	}

	public Set<Armor> readFile(File file) throws FileNotFoundException,
			IOException {
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		return read(bufferedReader);
	}

	public Set<Armor> read(BufferedReader bufferedReader)
			throws IOException {
	    Set<Armor> armors = new HashSet<Armor>();
		String line = bufferedReader.readLine();
		Map<String,Integer> columnsMap = generateColumnsMap(line);
		while ((line = bufferedReader.readLine()) != null) {
			String[] pieces = line.split(",");
			ARMOR_TYPE armorType = ARMOR_TYPE.valueOf(pieces[columnsMap.get(TYPE)]);
			ARMOR_SLOT armorSlot = ARMOR_SLOT.valueOf(pieces[columnsMap.get(SLOT)]);
			double encumbrance = Double.parseDouble(pieces[columnsMap.get(ENCUMBRANCE)]);
			Armor armor = new Armor(armorType, armorSlot, encumbrance);
			for(PROTECTION protection : PROTECTION.values()) {
			    if(columnsMap.containsKey(protection.toString())){
			        armor.addResistance(protection, Double.parseDouble(pieces[columnsMap.get(protection.toString())]));
			    }
			}
			armors.add(armor);
		}
		bufferedReader.close();
		return armors;
	}
	
	private static Map<String, Integer> generateColumnsMap(String headerRow) {
		String[] pieces = headerRow.split(",");
		Map<String, Integer> columnsToIndexes = new HashMap<String, Integer>();
		for(int i=0; i<pieces.length; i++){
			for(String knownColumn : KNOWN_COLUMNS){
				if(pieces[i].equalsIgnoreCase(knownColumn)){
					columnsToIndexes.put(knownColumn, i);
					break;
				}
			}
		}
		return columnsToIndexes;
	}

}
