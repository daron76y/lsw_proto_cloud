package com.lsw_proto_cloud.battle.api;

import com.lsw_proto_cloud.battle.InputService;
import com.lsw_proto_cloud.battle.BattleCommand;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;
import com.lsw_proto_cloud.battle.WaitCommand;
import com.lsw_proto_cloud.battle.DefendCommand;
import com.lsw_proto_cloud.battle.AttackCommand;
import com.lsw_proto_cloud.battle.CastCommand;
import com.lsw_proto_cloud.core.abilities.Ability;
import java.util.Random;
import java.util.List;


public class AIInputService implements InputService {
     private static final Random random = new Random();

    @Override
    public BattleCommand chooseBattleCommand(Unit unit, Party allyParty, Party enemyParty) {

        List<Unit> enemies = enemyParty.getAliveUnits();
        if (enemies.isEmpty()) {
            return new WaitCommand(unit);
        }
        int choice = random.nextInt(4);

        switch (choice) {
            case 0: 
                Unit attackTarget = enemies.get(random.nextInt(enemies.size()));
                return new AttackCommand(unit, attackTarget);

            case 1: 
                return new DefendCommand(unit);

            case 2: 
                return new WaitCommand(unit);

            case 3:
                List<Ability> abilities = unit.getAbilities();
                if (!abilities.isEmpty()) {
                    Ability ability = abilities.get(random.nextInt(abilities.size()));
                    Unit castTarget = null;
                    if (ability.requiresTarget()) {
                        castTarget = enemies.get(random.nextInt(enemies.size()));
                    }
                    return new CastCommand(unit, castTarget, allyParty, enemyParty, ability);
                } else {
                    
                    Unit fallbackTarget = enemies.get(random.nextInt(enemies.size()));
                    return new AttackCommand(unit, fallbackTarget);
                }
            default:
                return new WaitCommand(unit);
        }
    }
}
