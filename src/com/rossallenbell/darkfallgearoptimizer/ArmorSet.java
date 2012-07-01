package com.rossallenbell.darkfallgearoptimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rossallenbell.darkfallgearoptimizer.Armor.SlotAgnosticArmor;

public class ArmorSet {
    
    private final Map<SlotAgnosticArmor, Integer> saArmors;
    
    double encumbrance;
    
    double resistanceScore;
    
    public ArmorSet() {
        this.saArmors = new HashMap<SlotAgnosticArmor, Integer>();
    }
    
    public double getEncumbrance() {
        return encumbrance;
    }
    
    public double getResistanceScore() {
        return resistanceScore;
    }
    
    public Map<SlotAgnosticArmor, Integer> getSAArmorWithCounts(){
        return saArmors;
    }
    
    public void addArmor(Armor armor) {
        /*
         * This may look funky, but all it's really doing is allowing us to take
         * advantage of the concept involved where an armor set with scale arms
         * and full plate gloves is the same as one with full plate arms and
         * scale gloves
         */
        SlotAgnosticArmor saArmor = armor.getSlotAgnosticArmor();
        if (!saArmors.containsKey(saArmor)) {
            saArmors.put(saArmor, 1);
        } else {
            saArmors.put(saArmor, saArmors.get(saArmor) + 1);
        }
        
        encumbrance += saArmor.getEncumbrance();
        resistanceScore += saArmor.getResistanceScore();
    }
    
    @Override
    public String toString(){
        String rVal = "[encumbrance: " + DarkfallGearOptimizer.formatter.format(encumbrance)
                + "\n resist: " + DarkfallGearOptimizer.formatter.format(resistanceScore);
        List<SlotAgnosticArmor> armorsList = new ArrayList<SlotAgnosticArmor>(saArmors.keySet());
        Collections.sort(armorsList);
        for(Armor.SlotAgnosticArmor saArmor : armorsList){
            for(int i=0; i<saArmors.get(saArmor); i++){
                rVal = rVal + "\n\t" + saArmor;
            }
        }
        rVal = rVal + "\n]\n";
        return rVal;
    }
    
    @Override
    public int hashCode() {
        return saArmors.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArmorSet other = (ArmorSet) obj;
        return saArmors.equals(other.saArmors);
    }
    
}
