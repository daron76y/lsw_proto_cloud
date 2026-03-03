package com.lsw_proto_cloud.battle;

import com.lsw_proto_cloud.core.*;

public interface InputService {
    BattleCommand chooseBattleCommand(Unit unit, Party allyParty, Party enemyParty);
}
