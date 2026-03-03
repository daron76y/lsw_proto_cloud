package com.lsw_proto_cloud.battle.api;

import com.lsw_proto_cloud.battle.InputService;
import com.lsw_proto_cloud.battle.BattleCommand;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;

public class AIInputService implements InputService {
    @Override
    public BattleCommand chooseBattleCommand(Unit unit, Party allyParty, Party enemyParty) {
        return null;
    }
}
