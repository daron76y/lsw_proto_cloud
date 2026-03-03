package com.lsw_proto_cloud.core.effects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsw_proto_cloud.core.OutputService;
import com.lsw_proto_cloud.core.Unit;

public class FireShield extends Shield {
    private final double reflectPercent;

    @JsonCreator
    public FireShield(
            @JsonProperty("shieldAmount") int shieldAmount,
            @JsonProperty("reflectPercent") double reflectPercent) {
        super(shieldAmount);
        this.reflectPercent = reflectPercent;
    }

    @Override
    public String getName() {return "Fire Shield";}

    @Override
    public int modifyDamage(Unit attacker, Unit target, int damage, OutputService output) {
        damage = super.modifyDamage(attacker, target, damage, output);
        int reflected = (int)(damage * reflectPercent);
        if (attacker != null && reflected > 0) {
            attacker.setHealth(attacker.getHealth() - reflected);
            output.showMessage(String.format("%s reflected %d damage!", target.getName(), reflected));
        }
        return damage;
    }
}
