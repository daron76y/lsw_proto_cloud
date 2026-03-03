package com.lsw_proto_cloud.battle.api;

import com.lsw_proto_cloud.battle.*;
import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.core.abilities.Ability;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HttpInputService implements InputService {

    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

    // Call this method to submit a new action string from HTTP
    public void submitAction(String action) {
        inputQueue.offer(action);
    }

    @Override
    public BattleCommand chooseBattleCommand(Unit unit, Party allyParty, Party enemyParty) {
        try {
            // Wait until a command is available
            String action = inputQueue.take().trim();
            String[] tokens = action.split("\\s+");

            switch (tokens[0].toLowerCase()) {
                case "attack":
                    if (tokens.length < 2) throw new IllegalArgumentException("Usage: attack <target>");
                    Unit target = enemyParty.getUnitByName(tokens[1]);
                    return new AttackCommand(unit, target);

                case "defend":
                    return new DefendCommand(unit);

                case "wait":
                    return new WaitCommand(unit);

                case "cast":
                    if (tokens.length < 2) throw new IllegalArgumentException("Usage: cast <ability> [target]");
                    String abilityName = tokens[1];
                    Ability ability = unit.getAbilityByName(abilityName);
                    if (ability == null) throw new IllegalArgumentException("Ability not found: " + abilityName);

                    Unit castTarget = null;
                    if (ability.requiresTarget()) {
                        if (tokens.length < 3) throw new IllegalArgumentException("Ability requires a target.");
                        castTarget = enemyParty.getUnitByName(tokens[2]);
                    }

                    return new CastCommand(unit, castTarget, allyParty, enemyParty, ability);

                default:
                    throw new IllegalArgumentException("Unknown action: " + tokens[0]);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for action", e);
        }
    }
}