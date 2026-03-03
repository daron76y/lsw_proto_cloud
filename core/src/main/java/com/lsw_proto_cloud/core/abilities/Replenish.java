package com.lsw_proto_cloud.core.abilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsw_proto_cloud.core.OutputService;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;

public class Replenish extends Ability {
    private final int effectMultiplier; //for prophet upgrade

    @JsonCreator
    public Replenish(
            @JsonProperty("manaCost") int manaCost,
            @JsonProperty("effectMultiplier") int effectMultiplier) {
        super(manaCost);
        this.effectMultiplier = effectMultiplier;
    }

    @Override
    public String getName() {return "Replenish";}

    @Override
    public boolean requiresTarget() {return false;}

    @Override
    public void perform(Unit caster, Unit target, Party allyParty, Party enemyParty, OutputService output) {
        caster.setMana(caster.getMana() + 60 * effectMultiplier);
        output.showMessage(String.format("- %s gets 60 mana!", caster.getName()));
        for (Unit ally : allyParty.getAliveUnits()) {
            if (ally.equals(caster)) continue;
            ally.setMana(ally.getMana() + 30 * effectMultiplier);
            output.showMessage(String.format("- %s gets 30 mana!", ally.getName()));
        }
    }
}
