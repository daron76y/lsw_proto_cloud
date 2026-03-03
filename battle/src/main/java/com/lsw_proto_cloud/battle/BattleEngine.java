package com.lsw_proto_cloud.battle;

import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.core.effects.Effect;
import com.lsw_proto_cloud.core.abilities.Ability;

import java.util.*;

public class BattleEngine implements Battle {
    private final Queue<Unit> turnQueue;
    private final Party partyA;
    private final Party partyB;
    private final InputService partyAInput;
    private final InputService partyBInput;
    private final OutputService output;

    public BattleEngine(Party partyA, Party partyB,
                        InputService partyAInput,
                        InputService partyBInput,
                        OutputService output) {
        this.partyA = partyA;
        this.partyB = partyB;
        this.partyAInput = partyAInput;
        this.partyBInput = partyBInput;
        this.output = output;
        this.turnQueue = new LinkedList<>();
    }

    public Party runBattle() throws InterruptedException {
        // Initialize turn queue
        List<Unit> allUnits = new ArrayList<>();
        allUnits.addAll(partyA.getUnits());
        allUnits.addAll(partyB.getUnits());
        allUnits.removeIf(Unit::isDead);
        allUnits.sort(Comparator.comparingInt(Unit::getLevel).reversed());
        turnQueue.addAll(allUnits);

        while (!turnQueue.isEmpty() && !isBattleOver()) {
            Unit currentUnit = turnQueue.poll();
            if (currentUnit == null || currentUnit.isDead()) continue;

            if (currentUnit.getEffects().stream().anyMatch(Effect::preventsAction)) {
                output.showMessage(currentUnit.getName() + "'s turn has been cancelled!");
                turnQueue.add(currentUnit);
                continue;
            }

            // Output party states
            output.showParty(List.of(partyA, partyB));
            output.announceTurn(currentUnit);

            // Determine input service
            InputService input = partyA.getUnits().contains(currentUnit) ? partyAInput : partyBInput;
            Party allyParty = partyA.getUnits().contains(currentUnit) ? partyA : partyB;
            Party enemyParty = (allyParty == partyA) ? partyB : partyA;

            // Wait for the next battle command for this unit
            BattleCommand command;
            while (true) {
                try {
                    output.showMessage("Actions: [attack <target>] [defend] [wait] [cast <ability> <target>]");
                    command = input.chooseBattleCommand(currentUnit, allyParty, enemyParty);
                    command.execute(this);
                    break;
                } catch (Exception e) {
                    output.showMessage("Error: " + e.getMessage());
                }
            }

            // Update effects
            for (Effect effect : currentUnit.getEffects())
                effect.decrementDuration();
            currentUnit.getEffects().removeIf(Effect::isExpired);

            if (currentUnit.isAlive()) turnQueue.add(currentUnit);
        }

        // Clear effects after battle
        partyA.getUnits().forEach(Unit::clearDebuffEffects);
        partyB.getUnits().forEach(Unit::clearDebuffEffects);

        return (partyA.isDefeated()) ? partyB : partyA;
    }

    private boolean isBattleOver() {
        return partyA.isDefeated() || partyB.isDefeated();
    }

    // ============== Battle commands =================
    public void attack(Unit attacker, Unit target) {
        if (target == null || target.isDead())
            throw new IllegalArgumentException("Invalid target");

        int damage = attacker.getAttack();
        for (Effect e : target.getEffects()) damage = e.modifyDamage(attacker, target, damage, output);
        target.applyDamage(damage);
        output.showMessage(attacker.getName() + " attacks " + target.getName() + " for " + damage);

        Party ally = partyA.getUnits().contains(attacker) ? partyA : partyB;
        Party enemy = partyB.getUnits().contains(attacker) ? partyB : partyA;
        for (Effect e : attacker.getEffects()) e.onAttack(attacker, ally, target, enemy, output);
    }

    public void defend(Unit unit) {
        unit.setHealth(unit.getHealth() + 10);
        unit.setMana(unit.getMana() + 5);
        output.showMessage(unit.getName() + " defends! +10hp +5mp");
    }

    public void wait(Unit unit) {
        output.showMessage(unit.getName() + " waits!");
    }

    public void cast(Unit caster, Unit target, Party ally, Party enemy, Ability ability) {
        if (caster.getMana() < ability.getManaCost())
            throw new IllegalStateException("Not enough mana");
        if (ability.requiresTarget() && (target == null || target.isDead()))
            throw new IllegalArgumentException("Invalid target for ability");

        caster.setMana(caster.getMana() - ability.getManaCost());
        ability.execute(caster, target, ally, enemy, output);
    }

    public Party getPartyA() {return partyA;}
    public Party getPartyB() {return partyB;}
}