package com.lsw_proto_cloud.core.effects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsw_proto_cloud.core.OutputService;
import com.lsw_proto_cloud.core.Unit;

public class Shield extends Effect {
    private int shieldAmount;

    @JsonCreator
    public Shield(@JsonProperty("shieldAmount") int shieldAmount) {
        super(-1); //shield effect is infinite
        this.shieldAmount = shieldAmount;
    }

    @Override
    public String getName() {return "Shield";}

    @Override
    public int modifyDamage(Unit attacker, Unit target, int damage, OutputService output) {
        if (shieldAmount <= 0) return damage;
        int absorbedDamage = Math.min(shieldAmount, damage);
        shieldAmount -= absorbedDamage;
        output.showMessage(String.format("%s shielded %d damage!", target.getName(), absorbedDamage));
        return damage - absorbedDamage;
    }

    @Override
    public boolean isExpired() {return shieldAmount <= 0;}
}
