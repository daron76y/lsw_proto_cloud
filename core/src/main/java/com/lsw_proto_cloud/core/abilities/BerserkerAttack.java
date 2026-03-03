package com.lsw_proto_cloud.core.abilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsw_proto_cloud.core.HeroClass;
import com.lsw_proto_cloud.core.OutputService;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;
import com.lsw_proto_cloud.core.effects.Stunned;

public class BerserkerAttack extends Ability {
    @JsonCreator
    public BerserkerAttack(@JsonProperty("manaCost") int manaCost) {super(manaCost);}

    @Override
    public String getName() {return "BerserkerAttack";}

    @Override
    public boolean requiresTarget() {return true;}

    @Override
    public void perform(Unit caster, Unit target, Party allyParty, Party enemyParty, OutputService output) {
        //paladin upgrade
        if (caster.getMainClass() == HeroClass.PALADIN) {
            int healAmount = (int)(caster.getHealth() * 0.10);
            caster.setHealth(caster.getHealth() + healAmount);
            output.showMessage(String.format("- %s heals %d HP!", caster.getName(), healAmount));
        }

        //attack the initial target
        int damage = caster.getAttack();
        int inflictedDamage = target.applyDamage(damage);
        output.showMessage(String.format("- inflicts %d damage to %s", inflictedDamage, target.getName()));
        if (stun(caster, target)) output.showMessage(String.format("- %s stunned!", target.getName()));

        // damage 2 more units for 25% of original dmg
        damage = (int)(damage * 0.25);
        int count = 0;
        for (Unit enemy : enemyParty.getAliveUnits()) {
            if (count >= 2) break;
            if (enemy.equals(target)) continue;
            inflictedDamage = enemy.applyDamage(damage);
            count++;
            output.showMessage(String.format("- inflicts %d damage to %s", inflictedDamage, enemy.getName()));
            if (stun(caster, enemy)) output.showMessage(String.format("- %s stunned!", enemy.getName()));
        }
    }

    //knight upgrade
    private boolean stun(Unit caster, Unit target) {
        if (caster.getMainClass() == HeroClass.KNIGHT && Math.random() < 0.5) {
            target.addEffect(new Stunned(1));
            return true;
        }
        return false;
    }
}
