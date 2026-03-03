package com.lsw_proto_cloud.battle;

import com.lsw_proto_cloud.core.Unit;

public class WaitCommand implements BattleCommand {
    private final Unit unit;

    public WaitCommand(Unit unit) {
        this.unit = unit;
    }

    @Override
    public void execute(Battle battle) {
        battle.wait(unit);
    }
}
