package com.lsw_proto_cloud.core.effects;

import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.test.TestHelpers.RecordingOutput;
import org.junit.jupiter.api.Test;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

class EffectsTest {

    // ── Effect base (via Stunned) ─────────────────────────────────────────────

    @Test
    void decrementDuration_reducesCountByOne() {
        Stunned s = new Stunned(3);
        s.decrementDuration();
        assertEquals(2, s.getDuration());
    }

    @Test
    void isExpired_trueWhenDurationReachesZero() {
        Stunned s = new Stunned(1);
        s.decrementDuration();
        assertTrue(s.isExpired());
    }

    @Test
    void isExpired_falseWhileDurationPositive() {
        assertFalse(new Stunned(2).isExpired());
    }

    // ── Stunned ───────────────────────────────────────────────────────────────

    @Test
    void stunned_preventsAction() {
        assertTrue(new Stunned(1).preventsAction());
    }

    @Test
    void stunned_getName() {
        assertEquals("Stunned", new Stunned(1).getName());
    }

    // ── Shield ────────────────────────────────────────────────────────────────

    @Test
    void shield_absorbsDamage_upToShieldAmount() {
        Shield shield = new Shield(30);
        int remaining = shield.modifyDamage(null, warrior("Hero"), 20, new RecordingOutput());
        assertEquals(0, remaining);
    }

    @Test
    void shield_passesExcessDamageThrough() {
        Shield shield = new Shield(10);
        int remaining = shield.modifyDamage(null, warrior("Hero"), 25, new RecordingOutput());
        assertEquals(15, remaining);
    }

    @Test
    void shield_expiresWhenFullyDepleted() {
        Shield shield = new Shield(10);
        shield.modifyDamage(null, warrior("Hero"), 10, new RecordingOutput());
        assertTrue(shield.isExpired());
    }

    @Test
    void shield_notExpired_whenHpRemains() {
        Shield shield = new Shield(20);
        shield.modifyDamage(null, warrior("Hero"), 5, new RecordingOutput());
        assertFalse(shield.isExpired());
    }

    @Test
    void shield_doesNotPreventAction() {
        assertFalse(new Shield(10).preventsAction());
    }

    @Test
    void shield_showsAbsorbMessage() {
        RecordingOutput out = new RecordingOutput();
        new Shield(30).modifyDamage(null, warrior("Hero"), 20, out);
        assertTrue(out.anyMatch("shielded 20"));
    }

    @Test
    void shield_getName() {
        assertEquals("Shield", new Shield(10).getName());
    }

    // ── FireShield ────────────────────────────────────────────────────────────

    @Test
    void fireShield_reflectsPercentageOfPassthroughDamageToAttacker() {
        // shield=5, damage=20 → absorbs 5, passthrough=15, reflect=10% of 15=1
        FireShield fs       = new FireShield(5, 0.10);
        Unit attacker       = warrior("Attacker");
        int healthBefore    = attacker.getHealth();

        fs.modifyDamage(attacker, warrior("Defender"), 20, new RecordingOutput());

        assertEquals(healthBefore - 1, attacker.getHealth());
    }

    @Test
    void fireShield_noReflect_whenDamageFullyAbsorbed() {
        FireShield fs    = new FireShield(100, 0.10);
        Unit attacker    = warrior("Attacker");
        int healthBefore = attacker.getHealth();

        fs.modifyDamage(attacker, warrior("Defender"), 5, new RecordingOutput());

        assertEquals(healthBefore, attacker.getHealth());
    }

    @Test
    void fireShield_stillAbsorbsDamage_likeShield() {
        FireShield fs  = new FireShield(10, 0.10);
        Unit attacker  = warrior("Attacker");
        int remaining  = fs.modifyDamage(attacker, warrior("Defender"), 25, new RecordingOutput());
        assertEquals(15, remaining);
    }

    @Test
    void fireShield_getName() {
        assertEquals("Fire Shield", new FireShield(10, 0.1).getName());
    }

    // ── ManaBurn ──────────────────────────────────────────────────────────────

    @Test
    void manaBurn_burnsTenPercentOfTargetMaxMana() {
        ManaBurn mb  = new ManaBurn();
        Unit target  = order("Merlin");  // maxMp=50, currentMp=50
        Party ally   = party("A", warrior("A1"));
        Party enemy  = party("B", target);

        mb.onAttack(warrior("Rogue"), ally, target, enemy, new RecordingOutput());

        assertEquals(45, target.getMana()); // 50 - 10% of 50 = 5 burned
    }

    @Test
    void manaBurn_neverExpires() {
        assertFalse(new ManaBurn().isExpired());
    }

    @Test
    void manaBurn_doesNotPreventAction() {
        assertFalse(new ManaBurn().preventsAction());
    }

    @Test
    void manaBurn_showsBurnMessage() {
        ManaBurn mb      = new ManaBurn();
        Unit target      = order("Merlin");
        RecordingOutput out = new RecordingOutput();

        mb.onAttack(warrior("Rogue"), party("A"), target, party("B", target), out);

        assertTrue(out.anyMatch("burns away"));
    }

    @Test
    void manaBurn_getName() {
        assertEquals("Mana Burn", new ManaBurn().getName());
    }

    // ── SneakAttack ───────────────────────────────────────────────────────────

    @Test
    void sneakAttack_neverExpires() {
        assertFalse(new SneakAttack().isExpired());
    }

    @Test
    void sneakAttack_doesNotPreventAction() {
        assertFalse(new SneakAttack().preventsAction());
    }

    @Test
    void sneakAttack_getName() {
        assertEquals("Sneak Attack", new SneakAttack().getName());
    }

    @Test
    void sneakAttack_probabilisticallyDamagesAnEnemy() {
        SneakAttack sa   = new SneakAttack();
        Unit rogue       = warrior("Rogue");
        Unit enemy       = warrior("Goblin");
        Party allyParty  = party("A", rogue);
        Party enemyParty = party("B", enemy);
        RecordingOutput out = new RecordingOutput();

        // 200 trials at 50% — expect many hits
        for (int i = 0; i < 200; i++) {
            enemy.setHealth(100); // reset so enemy doesn't die
            sa.onAttack(rogue, allyParty, warrior("Target"), enemyParty, out);
        }

        assertTrue(out.messages.stream().anyMatch(m -> m.contains("sneak attacks")));
    }
}