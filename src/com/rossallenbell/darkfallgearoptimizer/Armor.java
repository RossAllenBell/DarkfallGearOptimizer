package com.rossallenbell.darkfallgearoptimizer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class Armor {
    
    public final ARMOR_TYPE type;
    
    public final ARMOR_SLOT slot;
    
    public final double encumbrance;
    
    private final Map<PROTECTION, Double> resistances;
    
    private SlotAgnosticArmor sahArmor;
    
    private boolean sahArmorReferenced = false;
    
    private int hashCode;
    
    public Armor(ARMOR_TYPE armorType, ARMOR_SLOT armorSlot, double encumbrance) {
        type = armorType;
        slot = armorSlot;
        this.encumbrance = encumbrance;
        resistances = new HashMap<PROTECTION, Double>();
        sahArmor = null;
        hashCode = generatehashCode();
    }
    
    public void addResistance(PROTECTION protection, Double value) {
        if(sahArmorReferenced){
            throw new IllegalStateException("You cannot add resistances after referencing the SlotAgnosticArmor");
        }
        resistances.put(protection, value);
        sahArmor = null;
        hashCode = generatehashCode();
    }
    
    private double getResistance(PROTECTION protection){
        return this.resistances.containsKey(protection)? this.resistances.get(protection) : 0;
    }
    
    public double getResistanceScore() {
        double totalWeights = 0;
        for(PROTECTION protection : DarkfallGearOptimizer.protectionWeights.keySet()){
            totalWeights += DarkfallGearOptimizer.protectionWeights.get(protection);
        }
        double score = 0;
        for(PROTECTION protection : DarkfallGearOptimizer.protectionWeights.keySet()){
            score += getResistance(protection) * DarkfallGearOptimizer.protectionWeights.get(protection) / totalWeights;
        }
        return score;
    }
    
    @Override
    public String toString() {
        return this.slot + " - " + this.type;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }

    public int generatehashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(encumbrance);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result
                + ((resistances == null) ? 0 : resistances.hashCode());
        result = prime * result + ((slot == null) ? 0 : slot.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Armor other = (Armor) obj;
        if (Double.doubleToLongBits(encumbrance) != Double
                .doubleToLongBits(other.encumbrance))
            return false;
        if (resistances == null) {
            if (other.resistances != null)
                return false;
        } else if (!resistances.equals(other.resistances))
            return false;
        if (slot != other.slot)
            return false;
        if (type != other.type)
            return false;
        return true;
    }
    
    public SlotAgnosticArmor getSlotAgnosticArmor(){
        if(sahArmor == null){
            sahArmor = new SlotAgnosticArmor(this);
            sahArmorReferenced = true;
        }
        return sahArmor;
    }
    
    public static class SlotAgnosticArmor implements Comparable<SlotAgnosticArmor> {
        
        public static final Set<ARMOR_SLOT> interchangeables = new HashSet<ARMOR_SLOT>(Arrays.asList(new ARMOR_SLOT[] {ARMOR_SLOT.Boots, ARMOR_SLOT.Gauntlets, ARMOR_SLOT.Arms, ARMOR_SLOT.Elbows, ARMOR_SLOT.Shoulders, ARMOR_SLOT.Greaves, ARMOR_SLOT.Girdle}));
        
        private Armor armor;
        
        private int hashCode;
        
        private SlotAgnosticArmor(Armor armor) {
            this.armor = armor;
            hashCode = generateHashCode();
        }
        
        public double getResistanceScore() {
            return armor.getResistanceScore();
        }
        
        public double getEncumbrance(){
            return armor.encumbrance;
        }
        
        @Override
        public String toString(){
            return  (interchangeables.contains(armor.slot)? "(interchangeable)" : armor.slot) + " - " +  armor.type;
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SlotAgnosticArmor other = (SlotAgnosticArmor) obj;
            if (armor.encumbrance != other.armor.encumbrance)
                return false;
            if (armor.type != other.armor.type)
                return false;
            return (interchangeables.contains(armor.slot) && interchangeables.contains(other.armor.slot)) || armor.slot == other.armor.slot;
        }

        /*
         * We can only do this because the backing Armor will throw an
         * exception if it is modified after this SlotAgnosticArmor is
         * referenced 
         */
        public int generateHashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(armor.encumbrance);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime
                    * result
                    + ((armor.resistances == null) ? 0 : armor.resistances
                            .hashCode());
            result = prime * result
                    + ((armor.type == null) ? 0 : armor.type.hashCode());
            return result;
        }

        @Override
        public int compareTo(SlotAgnosticArmor other) {
            return armor.slot.ordinal() - other.armor.slot.ordinal();
        }
        
    }
    
}
