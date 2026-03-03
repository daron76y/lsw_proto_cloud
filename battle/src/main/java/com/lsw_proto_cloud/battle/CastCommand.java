package com.lsw_proto_cloud.battle;

import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;
import com.lsw_proto_cloud.core.abilities.Ability;

public class CastCommand implements BattleCommand {
    private final Unit caster;
    private final Unit target;
    private final Party allyParty;
    private final Party enemyParty;
    private final Ability ability;

    public CastCommand(Unit caster, Unit target, Party allyParty, Party enemyParty, Ability ability) {
        this.caster = caster;
        this.target = target;
        this.allyParty = allyParty;
        this.enemyParty = enemyParty;
        this.ability = ability;
    }

    @Override
    public void execute(Battle battle) {
        battle.cast(caster, target, allyParty, enemyParty, ability);
    }
}
