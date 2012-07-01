package com.rossallenbell.darkfallgearoptimizer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ArmorSet {
    
    private final Set<Armor> armors;
    
    private final Map<Armor.SlotAgnosticArmor, Integer> saArmors;
    
    public ArmorSet() {
        this.armors = new HashSet<Armor>();
        this.saArmors = new HashMap<Armor.SlotAgnosticArmor, Integer>();
    }
    
    public double getEncumbrance() {
        double encumbrance = 0;
        for (Armor armor : armors) {
            encumbrance += armor.encumbrance;
        }
        return encumbrance;
    }
    
    public double getResistanceScore() {
        double resistanceScore = 0;
        for (Armor armor : armors) {
            resistanceScore += armor.getResistanceScore();
        }
        return resistanceScore;
    }
    
    public void addArmor(Armor armor) {
        for (Armor existingArmor : armors) {
            if (existingArmor.slot.equals(armor.slot)) {
                throw new IllegalArgumentException("The armor: " + armor
                        + " is for a slot already occupied in the armor set: "
                        + this);
            }
        }
        armors.add(armor);
        
        /*
         * This may look funky, but all it's really doing is allowing us to take
         * advantage of the concept involved where an armor set with scale arms
         * and full plate gloves is the same as one with full plate arms and
         * scale gloves
         */
        Armor.SlotAgnosticArmor sahArmor = armor.getSlotAgnosticArmor();
        if (!saArmors.containsKey(sahArmor)) {
            saArmors.put(sahArmor, 0);
        }
        saArmors.put(sahArmor, saArmors.get(sahArmor) + 1);
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
    
    @Override
    public String toString() {
        return "[encumbrance=" + getEncumbrance() + ",\n\tresistanceScore="
                + getResistanceScore() + ",\n\t"
                + Arrays.toString(armors.toArray()) + "]";
    }
    
}
