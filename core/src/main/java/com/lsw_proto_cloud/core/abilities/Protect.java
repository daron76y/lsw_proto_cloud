package com.lsw_proto_cloud.core.abilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsw_proto_cloud.core.HeroClass;
import com.lsw_proto_cloud.core.OutputService;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;
import com.lsw_proto_cloud.core.effects.Effect;
import com.lsw_proto_cloud.core.effects.FireShield;
import com.lsw_proto_cloud.core.effects.Shield;

public class Protect extends Ability {
    private final int effectMultiplier; //for prophet upgrade

    @JsonCreator
    public Protect(
            @JsonProperty("manaCost") int manaCost,
            @JsonProperty("effectMultiplier") int effectMultiplier) {
        super(manaCost);
        this.effectMultiplier = effectMultiplier;
    }

    @Override
    public String getName() {return "Protect";}

    @Override
    public boolean requiresTarget() {return false;}

    @Override
    public void perform(Unit caster, Unit target, Party allyParty, Party enemyParty, OutputService output) {
        for (Unit ally : allyParty.getAliveUnits()) {
            int shieldAmount = (int)(ally.getHealth() * 0.10) * effectMultiplier;

            Effect shieldEffect = (caster.getMainClass() == HeroClass.HERETIC) ?
                    new FireShield(shieldAmount, 0.10) :
                    new Shield(shieldAmount);
            ally.addEffect(shieldEffect);

            output.showMessage(String.format("- %s gets %d %ss!", ally.getName(), shieldAmount, shieldEffect.getName()));
        }
    }
}
