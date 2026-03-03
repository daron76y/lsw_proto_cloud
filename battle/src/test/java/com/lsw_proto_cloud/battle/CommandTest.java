package com.lsw_proto_cloud.battle;

import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;
import com.lsw_proto_cloud.core.abilities.Heal;
import com.lsw_proto_cloud.test.TestHelpers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

class CommandTest {

    private SpyBattle spy;
    private Unit unitA;
    private Unit unitB;
    private Party allyParty;
    private Party enemyParty;

    @BeforeEach
    void setUp() {
        spy        = new SpyBattle();
        unitA      = warrior("Arthur");
        unitB      = warrior("Goblin");
        allyParty  = party("Allies",  unitA);
        enemyParty = party("Enemies", unitB);
    }

    // ── AttackCommand ─────────────────────────────────────────────────────────

    @Test
    void attackCommand_delegatesToBattleAttack() {
        new AttackCommand(unitA, unitB).execute(spy);
        assertEquals(1, spy.attacks.size());
        assertSame(unitA, spy.attacks.get(0).attacker());
        assertSame(unitB, spy.attacks.get(0).target());
    }

    @Test
    void attackCommand_callsAttackExactlyOnce() {
        new AttackCommand(unitA, unitB).execute(spy);
        assertEquals(1, spy.attacks.size());
    }

    // ── DefendCommand ─────────────────────────────────────────────────────────

    @Test
    void defendCommand_delegatesToBattleDefend() {
        new DefendCommand(unitA).execute(spy);
        assertEquals(1, spy.defends.size());
        assertSame(unitA, spy.defends.get(0));
    }

    @Test
    void defendCommand_callsDefendExactlyOnce() {
        new DefendCommand(unitA).execute(spy);
        assertEquals(1, spy.defends.size());
    }

    // ── WaitCommand ───────────────────────────────────────────────────────────

    @Test
    void waitCommand_delegatesToBattleWait() {
        new WaitCommand(unitA).execute(spy);
        assertEquals(1, spy.waits.size());
        assertSame(unitA, spy.waits.get(0));
    }

    @Test
    void waitCommand_callsWaitExactlyOnce() {
        new WaitCommand(unitA).execute(spy);
        assertEquals(1, spy.waits.size());
    }

    // ── CastCommand ───────────────────────────────────────────────────────────

    @Test
    void castCommand_delegatesToBattleCast_withAllArguments() {
        Heal ability = new Heal(10, 1);
        new CastCommand(unitA, unitB, allyParty, enemyParty, ability).execute(spy);

        assertEquals(1, spy.casts.size());
        var c = spy.casts.get(0);
        assertSame(unitA,      c.caster());
        assertSame(unitB,      c.target());
        assertSame(allyParty,  c.ally());
        assertSame(enemyParty, c.enemy());
        assertSame(ability,    c.ability());
    }

    @Test
    void castCommand_allowsNullTarget() {
        new CastCommand(unitA, null, allyParty, enemyParty, new Heal(10, 1)).execute(spy);
        assertEquals(1, spy.casts.size());
        assertNull(spy.casts.get(0).target());
    }
}