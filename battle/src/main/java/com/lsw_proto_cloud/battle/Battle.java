package com.lsw_proto_cloud.battle;

import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;
import com.lsw_proto_cloud.core.abilities.Ability;

public interface Battle {
    void attack(Unit attacker, Unit target);
    void defend(Unit unit);
    void wait(Unit unit);
    void cast(Unit caster, Unit target, Party allyParty, Party enemyParty, Ability ability);
}
