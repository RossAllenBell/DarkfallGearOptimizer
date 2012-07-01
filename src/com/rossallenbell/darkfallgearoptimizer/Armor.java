package com.rossallenbell.darkfallgearoptimizer;

import java.util.HashMap;
import java.util.Map;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class Armor {
    
    public final ARMOR_TYPE type;
    
    public final ARMOR_SLOT slot;
    
    public final double encumbrance;
    
    private final Map<PROTECTION, Double> resistances;
    
    private SlotAgnosticArmor sahArmor;
    
    public Armor(ARMOR_TYPE armorType, ARMOR_SLOT armorSlot, double encumbrance) {
        type = armorType;
        slot = armorSlot;
        this.encumbrance = encumbrance;
        resistances = new HashMap<PROTECTION, Double>();
        sahArmor = null;
    }
    
    public void addResistance(PROTECTION protection, Double value) {
        resistances.put(protection, value);
        sahArmor = null;
    }
    
    public double getResistanceScore() {
        return resistances.get(PROTECTION.Slashing);
    }
    
    @Override
    public String toString() {
        return this.slot + " - " + this.type;
    }
    
    @Override
    public int hashCode() {
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
        }
        return sahArmor;
    }
    
    public static class SlotAgnosticArmor {
        
        private Armor armor;
        
        private SlotAgnosticArmor(Armor armor) {
            this.armor = armor;
        }
        
        @Override
        public int hashCode() {
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
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SlotAgnosticArmor other = (SlotAgnosticArmor) obj;
            if (Double.doubleToLongBits(armor.encumbrance) != Double
                    .doubleToLongBits(other.armor.encumbrance))
                return false;
            if (armor.resistances == null) {
                if (other.armor.resistances != null)
                    return false;
            } else if (!armor.resistances.equals(other.armor.resistances))
                return false;
            if (armor.type != other.armor.type)
                return false;
            return true;
        }
        
    }
    
}
