package com.lsw_proto_cloud.core.abilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsw_proto_cloud.core.HeroClass;
import com.lsw_proto_cloud.core.OutputService;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;

import java.util.Comparator;

public class Heal extends Ability {
    private final int effectMultiplier; //for prophet upgrade

    @JsonCreator
    public Heal(
            @JsonProperty("manaCost") int manaCost,
            @JsonProperty("effectMultiplier") int effectMultiplier) {
        super(manaCost);
        this.effectMultiplier = effectMultiplier;
    }

    @Override
    public String getName() {return "Heal";}

    @Override
    public boolean requiresTarget() {return false;}

    @Override
    public void perform(Unit caster, Unit target, Party allyParty, Party enemyParty, OutputService output) {
        if (caster.getMainClass() == HeroClass.PRIEST) { //heal all allies if priest
            for (Unit ally : allyParty.getAliveUnits()) {
                int healAmount = (int)(ally.getMaxHealth() * 0.25) * effectMultiplier;
                ally.setHealth(ally.getHealth() + healAmount);
                output.showMessage(String.format("- %s heals %d health!", ally.getName(), healAmount));
            }
        }
        else { //only heal the lowest-hp ally
            Unit lowest = allyParty.getAliveUnits().stream().min(Comparator.comparing(Unit::getHealth)).orElseThrow();
            int healAmount = (int)(lowest.getMaxHealth() * 0.25) * effectMultiplier;
            lowest.setHealth(lowest.getHealth() + healAmount);
            output.showMessage(String.format("- %s heals %d health!", lowest.getName(), healAmount));
        }
    }
}
