package com.lsw_proto_cloud.battle;

import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.core.abilities.Fireball;
import com.lsw_proto_cloud.core.abilities.Heal;
import com.lsw_proto_cloud.core.effects.Stunned;
import com.lsw_proto_cloud.test.TestHelpers.*;
import org.junit.jupiter.api.Test;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

class BattleEngineTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    /** InputService that always attacks the first alive enemy. */
    private static InputService alwaysAttack() {
        return (u, al, en) -> {
            Unit t = en.getAliveUnits().isEmpty() ? null : en.getAliveUnits().get(0);
            return t != null ? new AttackCommand(u, t) : new WaitCommand(u);
        };
    }

    /** InputService that always waits. */
    private static InputService alwaysWait() {
        return (u, al, en) -> new WaitCommand(u);
    }

    private static BattleEngine engine(Party pa, Party pb,
                                       InputService ia, InputService ib,
                                       RecordingOutput out) {
        return new BattleEngine(pa, pb, ia, ib, out);
    }

    // ── runBattle – winner resolution ─────────────────────────────────────────

    @Test
    void runBattle_partyAWins_whenPartyBDefeated() throws InterruptedException {
        Unit a = makeUnit("Arthur", HeroClass.WARRIOR, 200, 0, 100, 0);
        Unit b = warrior("Goblin");
        Party pa = party("A", a);
        Party pb = party("B", b);

        Party winner = engine(pa, pb, alwaysAttack(), alwaysWait(), new RecordingOutput()).runBattle();

        assertSame(pa, winner);
        assertTrue(b.isDead());
    }

    @Test
    void runBattle_partyBWins_whenPartyADefeated() throws InterruptedException {
        Unit a = warrior("Arthur");
        Unit b = makeUnit("Goblin", HeroClass.WARRIOR, 200, 0, 100, 0);
        Party pa = party("A", a);
        Party pb = party("B", b);

        Party winner = engine(pa, pb, alwaysWait(), alwaysAttack(), new RecordingOutput()).runBattle();

        assertSame(pb, winner);
        assertTrue(a.isDead());
    }

    @Test
    void runBattle_higherLevelUnitActsFirst() throws InterruptedException {
        // If the higher-level unit acts first and one-shots the enemy,
        // the lower-level unit never gets a turn.
        Unit strong = makeUnit("Strong", HeroClass.WARRIOR, 200, 0, 100, 0);
        Unit weak   = warrior("Weak");

        // Level strong up so its level > weak's level
        strong.gainExperience(strong.expNeededForLvl(2));
        strong.levelUpClass(HeroClass.WARRIOR);

        Party pa = party("A", strong);
        Party pb = party("B", weak);

        int[] bTurns = {0};
        InputService inputB = (u, al, en) -> { bTurns[0]++; return new WaitCommand(u); };

        engine(pa, pb, alwaysAttack(), inputB, new RecordingOutput()).runBattle();

        // strong kills weak on turn 1 before weak ever acts
        assertEquals(0, bTurns[0]);
        assertTrue(weak.isDead());
    }

    @Test
    void runBattle_deadUnitsInInitialList_areSkipped() throws InterruptedException {
        Unit alive = warrior("Arthur");
        Unit dead  = makeUnit("Ghost", HeroClass.WARRIOR, 0, 0, 0, 0); // 0 hp = dead
        Party pa   = new Party("A");
        pa.addUnit(alive);
        pa.addUnit(dead);

        Unit b   = makeUnit("Goblin", HeroClass.WARRIOR, 200, 0, 100, 0);
        Party pb = party("B", b);

        RecordingOutput out = new RecordingOutput();
        engine(pa, pb, alwaysWait(), alwaysAttack(), out).runBattle();

        assertFalse(out.anyMatch("Ghost"));
    }

    @Test
    void runBattle_stunnedUnit_skipsTurnAndRequeues() throws InterruptedException {
        Unit a = warrior("Arthur");
        Unit b = makeUnit("Goblin", HeroClass.WARRIOR, 200, 0, 100, 0);
        a.addEffect(new Stunned(1));

        Party pa = party("A", a);
        Party pb = party("B", b);

        RecordingOutput out = new RecordingOutput();
        engine(pa, pb, alwaysWait(), alwaysAttack(), out).runBattle();

        assertTrue(out.anyMatch("Arthur's turn has been cancelled!"));
    }

    @Test
    void runBattle_badCommand_retriesUntilSuccess() throws InterruptedException {
        Unit a = warrior("Arthur");
        Unit b = makeUnit("Goblin", HeroClass.WARRIOR, 200, 0, 100, 0);
        Party pa = party("A", a);
        Party pb = party("B", b);

        int[] calls = {0};
        InputService inputA = (u, al, en) -> {
            calls[0]++;
            if (calls[0] == 1) throw new RuntimeException("bad input");
            return new WaitCommand(u);
        };

        RecordingOutput out = new RecordingOutput();
        engine(pa, pb, inputA, alwaysAttack(), out).runBattle();

        assertTrue(out.anyMatch("bad input"));
        assertTrue(calls[0] >= 2);
    }

    @Test
    void runBattle_clearsDebuffEffects_fromAllUnitsAfterBattle() throws InterruptedException {
        Unit a = warrior("Arthur");
        Unit b = makeUnit("Goblin", HeroClass.WARRIOR, 200, 0, 100, 0);
        a.addEffect(new Stunned(99));
        Party pa = party("A", a);
        Party pb = party("B", b);

        engine(pa, pb, alwaysWait(), alwaysAttack(), new RecordingOutput()).runBattle();

        // WARRIOR has no innate effects, so list should be empty after battle
        assertTrue(a.getEffects().isEmpty());
        assertTrue(b.getEffects().isEmpty());
    }

    @Test
    void runBattle_effectDurationDecrementsEachTurn() throws InterruptedException {
        // Give a unit a 1-turn stun; after its (cancelled) turn the effect should expire
        Unit a = warrior("Arthur");
        Unit b = makeUnit("Goblin", HeroClass.WARRIOR, 200, 0, 100, 0);
        Stunned stun = new Stunned(1);
        a.addEffect(stun);
        Party pa = party("A", a);
        Party pb = party("B", b);

        engine(pa, pb, alwaysWait(), alwaysAttack(), new RecordingOutput()).runBattle();

        // After the battle stun's duration should have been decremented and cleared
        assertFalse(a.getEffects().contains(stun));
    }

    // ── attack() ─────────────────────────────────────────────────────────────

    @Test
    void attack_dealsDamageMinusDefense() {
        Unit a   = makeUnit("Arthur", HeroClass.WARRIOR, 20, 0, 100, 0);
        Unit b   = makeUnit("Goblin", HeroClass.WARRIOR, 5,  2, 100, 0);
        Party pa = party("A", a);
        Party pb = party("B", b);
        RecordingOutput out = new RecordingOutput();
        BattleEngine eng = new BattleEngine(pa, pb, alwaysWait(), alwaysWait(), out);

        eng.attack(a, b);

        assertEquals(82, b.getHealth()); // 100 - (20-2)
        assertTrue(out.anyMatch("Arthur attacks Goblin for 20"));
    }

    @Test
    void attack_throwsOnNullTarget() {
        Unit a   = warrior("Arthur");
        Party pa = party("A", a);
        Party pb = new Party("B");
        BattleEngine eng = new BattleEngine(pa, pb, alwaysWait(), alwaysWait(), new RecordingOutput());

        assertThrows(IllegalArgumentException.class, () -> eng.attack(a, null));
    }

    @Test
    void attack_throwsOnDeadTarget() {
        Unit a    = warrior("Arthur");
        Unit dead = makeUnit("Ghost", HeroClass.WARRIOR, 0, 0, 0, 0);
        Party pa  = party("A", a);
        Party pb  = new Party("B");
        BattleEngine eng = new BattleEngine(pa, pb, alwaysWait(), alwaysWait(), new RecordingOutput());

        assertThrows(IllegalArgumentException.class, () -> eng.attack(a, dead));
    }

    @Test
    void attack_triggersOnAttackEffects() {
        // WARLOCK has ManaBurn as an innate onAttack effect
        Unit warlock = makeUnit("Warlock", HeroClass.WARLOCK, 10, 0, 100, 50);
        Unit target  = makeUnit("Mage",    HeroClass.MAGE,    5,  0, 100, 50);
        Party pa = party("A", warlock);
        Party pb = party("B", target);
        BattleEngine eng = new BattleEngine(pa, pb, alwaysWait(), alwaysWait(), new RecordingOutput());

        int manaBefore = target.getMana();
        eng.attack(warlock, target);

        assertTrue(target.getMana() < manaBefore);
    }

    // ── defend() ─────────────────────────────────────────────────────────────

    @Test
    void defend_increasesHealthByTen() {
        Unit a   = makeUnit("Arthur", HeroClass.ORDER, 5, 5, 100, 50);
        a.setHealth(80);
        Party pa = party("A", a);
        BattleEngine eng = new BattleEngine(pa, new Party("B"), alwaysWait(), alwaysWait(), new RecordingOutput());

        eng.defend(a);

        assertEquals(90, a.getHealth());
    }

    @Test
    void defend_increasesManaByFive() {
        Unit a   = makeUnit("Arthur", HeroClass.ORDER, 5, 5, 100, 50);
        a.setMana(40);
        Party pa = party("A", a);
        BattleEngine eng = new BattleEngine(pa, new Party("B"), alwaysWait(), alwaysWait(), new RecordingOutput());

        eng.defend(a);

        assertEquals(45, a.getMana());
    }

    @Test
    void defend_showsMessage() {
        Unit a   = makeUnit("Arthur", HeroClass.ORDER, 5, 5, 100, 50);
        RecordingOutput out = new RecordingOutput();
        BattleEngine eng = new BattleEngine(party("A", a), new Party("B"), alwaysWait(), alwaysWait(), out);

        eng.defend(a);

        assertTrue(out.anyMatch("Arthur defends! +10hp +5mp"));
    }

    @Test
    void defend_healthCapsAtMaxHealth() {
        Unit a = makeUnit("Arthur", HeroClass.ORDER, 5, 5, 100, 50);
        // health already at max (100); adding 10 should stay at 100
        BattleEngine eng = new BattleEngine(party("A", a), new Party("B"), alwaysWait(), alwaysWait(), new RecordingOutput());

        eng.defend(a);

        assertEquals(100, a.getHealth());
    }

    // ── wait() ────────────────────────────────────────────────────────────────

    @Test
    void wait_showsMessage() {
        Unit a = warrior("Arthur");
        RecordingOutput out = new RecordingOutput();
        BattleEngine eng = new BattleEngine(party("A", a), new Party("B"), alwaysWait(), alwaysWait(), out);

        eng.wait(a);

        assertTrue(out.anyMatch("Arthur waits!"));
    }

    // ── cast() ────────────────────────────────────────────────────────────────

    @Test
    void cast_executesAbilityAndDeductsMana() {
        Unit caster = order("Merlin"); // 50 mp
        Party pa    = party("A", caster);
        RecordingOutput out = new RecordingOutput();
        BattleEngine eng = new BattleEngine(pa, new Party("B"), alwaysWait(), alwaysWait(), out);

        eng.cast(caster, null, pa, new Party("B"), new Heal(20, 1));

        assertEquals(30, caster.getMana()); // 50 - 20
        assertTrue(out.anyMatch("Merlin casts Heal!"));
    }

    @Test
    void cast_throwsWhenNotEnoughMana() {
        Unit caster = makeUnit("Merlin", HeroClass.ORDER, 5, 5, 100, 5);
        Party pa    = party("A", caster);
        BattleEngine eng = new BattleEngine(pa, new Party("B"), alwaysWait(), alwaysWait(), new RecordingOutput());

        assertThrows(IllegalStateException.class,
                () -> eng.cast(caster, null, pa, new Party("B"), new Heal(50, 1)));
    }

    @Test
    void cast_throwsWhenRequiresTarget_andTargetIsNull() {
        Unit caster = order("Merlin");
        Party pa    = party("A", caster);
        BattleEngine eng = new BattleEngine(pa, new Party("B"), alwaysWait(), alwaysWait(), new RecordingOutput());

        assertThrows(IllegalArgumentException.class,
                () -> eng.cast(caster, null, pa, new Party("B"), new Fireball(10, 1)));
    }

    @Test
    void cast_throwsWhenRequiresTarget_andTargetIsDead() {
        Unit caster = order("Merlin");
        Unit dead   = makeUnit("Ghost", HeroClass.WARRIOR, 0, 0, 0, 0);
        Party pa    = party("A", caster);
        BattleEngine eng = new BattleEngine(pa, new Party("B"), alwaysWait(), alwaysWait(), new RecordingOutput());

        assertThrows(IllegalArgumentException.class,
                () -> eng.cast(caster, dead, pa, new Party("B"), new Fireball(10, 1)));
    }

    @Test
    void cast_allowsNullTarget_whenAbilityDoesNotRequireTarget() {
        Unit caster = order("Merlin");
        Party pa    = party("A", caster);
        BattleEngine eng = new BattleEngine(pa, new Party("B"), alwaysWait(), alwaysWait(), new RecordingOutput());

        assertDoesNotThrow(() -> eng.cast(caster, null, pa, new Party("B"), new Heal(10, 1)));
    }

    // ── getters ───────────────────────────────────────────────────────────────

    @Test
    void getPartyA_returnsCorrectParty() {
        Party pa = party("A", warrior("Arthur"));
        Party pb = party("B", warrior("Goblin"));
        BattleEngine eng = new BattleEngine(pa, pb, alwaysWait(), alwaysWait(), new RecordingOutput());
        assertSame(pa, eng.getPartyA());
    }

    @Test
    void getPartyB_returnsCorrectParty() {
        Party pa = party("A", warrior("Arthur"));
        Party pb = party("B", warrior("Goblin"));
        BattleEngine eng = new BattleEngine(pa, pb, alwaysWait(), alwaysWait(), new RecordingOutput());
        assertSame(pb, eng.getPartyB());
    }
}