package com.lsw_proto_cloud.core.abilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsw_proto_cloud.core.OutputService;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChainLightning extends Ability {
    private final double subsequentDamageMultiplier; //for invoker upgrade

    @JsonCreator
    public ChainLightning(
            @JsonProperty("manaCost") int manaCost,
            @JsonProperty("subsequentDamageMultiplier") double subsequentDamageMultiplier) {
        super(manaCost);
        this.subsequentDamageMultiplier = subsequentDamageMultiplier;
    }

    @Override
    public String getName() {return "ChainLightning";}

    @Override
    public boolean requiresTarget() {return true;}

    @Override
    public void perform(Unit caster, Unit target, Party allyParty, Party enemyParty, OutputService output) {
        //damage initial target
        int damage = caster.getAttack();
        int inflictedDamage = target.applyDamage(damage);
        output.showMessage(String.format("- inflicts %d damage to %s", inflictedDamage, target.getName()));

        //randomly damage all other enemies for a percentage of the previous damage
        List<Unit> enemies = new ArrayList<>(enemyParty.getUnits()); //do not shuffle original list
        Collections.shuffle(enemies);
        for (Unit enemy : enemies) {
            if (enemy.equals(target)) continue;
            damage = (int)(damage * subsequentDamageMultiplier);
            inflictedDamage = enemy.applyDamage(damage);
            output.showMessage(String.format("- inflicts %d damage to %s", inflictedDamage, enemy.getName()));
        }
    }
}
