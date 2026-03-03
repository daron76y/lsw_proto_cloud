package com.lsw_proto_cloud.core.effects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.lsw_proto_cloud.core.OutputService;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;

import java.util.List;
import java.util.Random;

public class SneakAttack extends Effect {
    @JsonCreator
    public SneakAttack() {
        super(-1); //sneak attack is infinite
    }

    @Override
    public String getName() {return "Sneak Attack";}

    @Override
    public void onAttack(Unit attacker, Party allyParty, Unit target, Party enemyParty, OutputService output) {
        if (Math.random() < 0.5) {
            List<Unit> enemies = enemyParty.getAliveUnits();
            Unit extraTarget = enemies.get(new Random().nextInt(enemies.size()));
            int inflictedDamage = extraTarget.applyDamage(attacker.getAttack());
            output.showMessage(String.format("- %s sneak attacks %s for %d damage!", attacker.getName(), extraTarget.getName(), inflictedDamage));
        }
    }

    @Override
    public boolean isExpired() {return false;}
}
