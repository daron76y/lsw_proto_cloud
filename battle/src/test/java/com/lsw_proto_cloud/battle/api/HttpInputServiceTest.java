package com.lsw_proto_cloud.battle.api;

import com.lsw_proto_cloud.battle.*;
import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.core.abilities.Fireball;
import com.lsw_proto_cloud.core.abilities.Heal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpInputServiceTest {

    private HttpInputService service;
    private Unit unit;
    private Unit enemy;
    private Party allyParty;
    private Party enemyParty;

    @BeforeEach
    void setUp() {
        service    = new HttpInputService();
        unit       = order("Merlin");
        enemy      = warrior("Goblin");
        allyParty  = party("Allies",  unit);
        enemyParty = party("Enemies", enemy);
    }

    // ── attack ───────────────────────────────────────────────────────────────

    @Test
    void attack_returnsAttackCommand() {
        service.submitAction("attack Goblin");
        assertInstanceOf(AttackCommand.class,
                service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    @Test
    void attack_isCaseInsensitive() {
        service.submitAction("ATTACK Goblin");
        assertInstanceOf(AttackCommand.class,
                service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    @Test
    void attack_missingTarget_throws() {
        service.submitAction("attack");
        assertThrows(IllegalArgumentException.class,
                () -> service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    // ── defend ───────────────────────────────────────────────────────────────

    @Test
    void defend_returnsDefendCommand() {
        service.submitAction("defend");
        assertInstanceOf(DefendCommand.class,
                service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    @Test
    void defend_isCaseInsensitive() {
        service.submitAction("DEFEND");
        assertInstanceOf(DefendCommand.class,
                service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    // ── wait ─────────────────────────────────────────────────────────────────

    @Test
    void wait_returnsWaitCommand() {
        service.submitAction("wait");
        assertInstanceOf(WaitCommand.class,
                service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    @Test
    void wait_isCaseInsensitive() {
        service.submitAction("WAIT");
        assertInstanceOf(WaitCommand.class,
                service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    // ── cast (no target) ─────────────────────────────────────────────────────

    @Test
    void cast_noTarget_returnsCastCommand() {
        // Heal is an ORDER innate ability and does not require a target
        service.submitAction("cast Heal");
        assertInstanceOf(CastCommand.class,
                service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    // ── cast (with target) ───────────────────────────────────────────────────

    @Test
    void cast_withTarget_returnsCastCommand() {
        unit.addAbility(new Fireball(10, 1));
        service.submitAction("cast Fireball Goblin");
        assertInstanceOf(CastCommand.class,
                service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    @Test
    void cast_requiresTarget_butNoneGiven_throws() {
        unit.addAbility(new Fireball(10, 1));
        service.submitAction("cast Fireball");
        assertThrows(IllegalArgumentException.class,
                () -> service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    @Test
    void cast_unknownAbility_throws() {
        service.submitAction("cast Nuke");
        assertThrows(IllegalArgumentException.class,
                () -> service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    @Test
    void cast_missingAbilityName_throws() {
        service.submitAction("cast");
        assertThrows(IllegalArgumentException.class,
                () -> service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    // ── unknown action ────────────────────────────────────────────────────────

    @Test
    void unknownAction_throws() {
        service.submitAction("flee");
        assertThrows(IllegalArgumentException.class,
                () -> service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    // ── whitespace handling ───────────────────────────────────────────────────

    @Test
    void trimsLeadingAndTrailingWhitespace() {
        service.submitAction("  defend  ");
        assertInstanceOf(DefendCommand.class,
                service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    // ── queue ordering & blocking ─────────────────────────────────────────────

    @Test
    void processesActionsInFifoOrder() {
        service.submitAction("defend");
        service.submitAction("wait");
        assertInstanceOf(DefendCommand.class, service.chooseBattleCommand(unit, allyParty, enemyParty));
        assertInstanceOf(WaitCommand.class,   service.chooseBattleCommand(unit, allyParty, enemyParty));
    }

    @Test
    void blocksUntilActionSubmitted() throws InterruptedException {
        new Thread(() -> {
            try { Thread.sleep(80); } catch (InterruptedException ignored) {}
            service.submitAction("wait");
        }).start();

        assertInstanceOf(WaitCommand.class,
                service.chooseBattleCommand(unit, allyParty, enemyParty));
    }
}