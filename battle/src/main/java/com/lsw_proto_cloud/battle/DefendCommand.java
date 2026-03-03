package com.lsw_proto_cloud.battle;

import com.lsw_proto_cloud.core.Unit;

public class DefendCommand implements BattleCommand {
    private final Unit unit;

    public DefendCommand(Unit unit) {
        this.unit = unit;
    }

    @Override
    public void execute(Battle battle) {
        battle.defend(unit);
    }
}
