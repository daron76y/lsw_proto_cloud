package com.lsw_proto_cloud.core;

import java.util.List;

public interface UnitFactory {
    Party generateEnemyParty(int playerCumulativeLevel);
    List<Unit> generateHeroRecruits(int numRecruits);
}
