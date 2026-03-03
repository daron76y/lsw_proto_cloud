package com.lsw_proto_cloud.core.abilities;

import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.core.effects.FireShield;
import com.lsw_proto_cloud.core.effects.Shield;
import com.lsw_proto_cloud.test.TestHelpers.RecordingOutput;
import org.junit.jupiter.api.Test;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

class AbilitiesTest {

    // ═══════════════════════════════════════════════════════════════════
    // BerserkerAttack
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void berserkerAttack_getName() {
        assertEquals("BerserkerAttack", new BerserkerAttack(60).getName());
    }

    @Test
    void berserkerAttack_requiresTarget() {
        assertTrue(new BerserkerAttack(60).requiresTarget());
    }

    @Test
    void berserkerAttack_damagesPrimaryTarget() {
        Unit caster = makeUnit("Arthur", HeroClass.WARRIOR, 20, 0, 100, 0);
        Unit target = makeUnit("Goblin", HeroClass.WARRIOR, 5,  0, 100, 0);
        Party ally  = party("A", caster);
        Party enemy = party("B", target);

        new BerserkerAttack(60).execute(caster, target, ally, enemy, new RecordingOutput());

        assertEquals(80, target.getHealth()); // 100 - 20
    }

    @Test
    void berserkerAttack_splashDamages_upToTwoOtherEnemies() {
        Unit caster = makeUnit("Arthur", HeroClass.WARRIOR, 20, 0, 100, 0);
        Unit target = makeUnit("E1",     HeroClass.WARRIOR, 0,  0, 100, 0);
        Unit e2     = makeUnit("E2",     HeroClass.WARRIOR, 0,  0, 100, 0);
        Unit e3     = makeUnit("E3",     HeroClass.WARRIOR, 0,  0, 100, 0);
        Party ally  = party("A", caster);
        Party enemy = party("B", target, e2, e3);

        new BerserkerAttack(60).execute(caster, target, ally, enemy, new RecordingOutput());

        // splash = 25% of 20 = 5
        assertEquals(95, e2.getHealth());
        assertEquals(95, e3.getHealth());
    }

    @Test
    void berserkerAttack_doesNotSplashPrimaryTarget() {
        Unit caster = makeUnit("Arthur", HeroClass.WARRIOR, 20, 0, 100, 0);
        Unit target = makeUnit("E1",     HeroClass.WARRIOR, 0,  0, 100, 0);
        Unit e2     = makeUnit("E2",     HeroClass.WARRIOR, 0,  0, 100, 0);
        Party ally  = party("A", caster);
        Party enemy = party("B", target, e2);

        new BerserkerAttack(60).execute(caster, target, ally, enemy, new RecordingOutput());

        // primary hit = 20, splash hit = 5 → total on target should be exactly 20
        assertEquals(80, target.getHealth());
    }

    @Test
    void berserkerAttack_paladin_healsSelfBeforeAttacking() {
        Unit paladin = makeUnit("Paladin", HeroClass.PALADIN, 20, 0, 100, 100);
        Unit target  = makeUnit("Goblin",  HeroClass.WARRIOR, 5,  0, 100, 0);
        Party ally   = party("A", paladin);
        Party enemy  = party("B", target);
        paladin.setHealth(80);

        new BerserkerAttack(60).execute(paladin, target, ally, enemy, new RecordingOutput());

        // 10% of 80 = 8 healed before attacking
        assertEquals(88, paladin.getHealth());
    }

    // ═══════════════════════════════════════════════════════════════════
    // ChainLightning
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void chainLightning_getName() {
        assertEquals("ChainLightning", new ChainLightning(40, 0.25).getName());
    }

    @Test
    void chainLightning_requiresTarget() {
        assertTrue(new ChainLightning(40, 0.25).requiresTarget());
    }

    @Test
    void chainLightning_damagesPrimaryTarget_thenChains() {
        Unit caster = makeUnit("Mage", HeroClass.CHAOS, 20, 0, 100, 100);
        Unit t1     = makeUnit("E1",   HeroClass.WARRIOR, 0, 0, 100, 0);
        Unit t2     = makeUnit("E2",   HeroClass.WARRIOR, 0, 0, 100, 0);
        Party ally  = party("A", caster);
        Party enemy = party("B", t1, t2);

        new ChainLightning(40, 0.25).execute(caster, t1, ally, enemy, new RecordingOutput());

        assertEquals(80, t1.getHealth());  // 100 - 20
        assertEquals(95, t2.getHealth());  // 100 - 25% of 20 = 5
    }

    @Test
    void chainLightning_doesNotHitPrimaryTargetTwice() {
        Unit caster = makeUnit("Mage", HeroClass.CHAOS, 20, 0, 100, 100);
        Unit t1     = makeUnit("E1",   HeroClass.WARRIOR, 0, 0, 100, 0);
        Party ally  = party("A", caster);
        Party enemy = party("B", t1);

        new ChainLightning(40, 0.25).execute(caster, t1, ally, enemy, new RecordingOutput());

        assertEquals(80, t1.getHealth()); // hit once only
    }

    @Test
    void chainLightning_invokerMultiplier_increasesChainDamage() {
        // INVOKER uses 0.50 instead of 0.25
        Unit caster = makeUnit("Invoker", HeroClass.INVOKER, 20, 0, 100, 100);
        Unit t1     = makeUnit("E1",      HeroClass.WARRIOR, 0,  0, 100, 0);
        Unit t2     = makeUnit("E2",      HeroClass.WARRIOR, 0,  0, 100, 0);
        Party ally  = party("A", caster);
        Party enemy = party("B", t1, t2);

        new ChainLightning(40, 0.50).execute(caster, t1, ally, enemy, new RecordingOutput());

        assertEquals(80, t1.getHealth()); // 20 damage
        assertEquals(90, t2.getHealth()); // 50% of 20 = 10 damage
    }

    // ═══════════════════════════════════════════════════════════════════
    // Fireball
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void fireball_getName() {
        assertEquals("Fireball", new Fireball(30, 1).getName());
    }

    @Test
    void fireball_requiresTarget() {
        assertTrue(new Fireball(30, 1).requiresTarget());
    }

    @Test
    void fireball_damagesTargetAndBothNeighbors() {
        Unit caster = makeUnit("Mage", HeroClass.CHAOS, 10, 0, 100, 100);
        Unit e1     = makeUnit("E1",   HeroClass.WARRIOR, 0, 0, 100, 0);
        Unit e2     = makeUnit("E2",   HeroClass.WARRIOR, 0, 0, 100, 0); // target
        Unit e3     = makeUnit("E3",   HeroClass.WARRIOR, 0, 0, 100, 0);
        Party ally  = party("A", caster);
        Party enemy = party("B", e1, e2, e3);

        new Fireball(30, 1).execute(caster, e2, ally, enemy, new RecordingOutput());

        assertEquals(90, e1.getHealth());
        assertEquals(90, e2.getHealth());
        assertEquals(90, e3.getHealth());
    }

    @Test
    void fireball_onlyHitsTarget_whenNoNeighbors() {
        Unit caster = makeUnit("Mage",   HeroClass.CHAOS, 10, 0, 100, 100);
        Unit target = makeUnit("Goblin", HeroClass.WARRIOR, 0, 0, 100, 0);
        Party ally  = party("A", caster);
        Party enemy = party("B", target);

        new Fireball(30, 1).execute(caster, target, ally, enemy, new RecordingOutput());

        assertEquals(90, target.getHealth());
    }

    @Test
    void fireball_sorcererMultiplier_doublesDamage() {
        Unit caster = makeUnit("Sorcerer", HeroClass.SORCERER, 10, 0, 100, 100);
        Unit target = makeUnit("Goblin",   HeroClass.WARRIOR,   0, 0, 100, 0);
        Party ally  = party("A", caster);
        Party enemy = party("B", target);

        new Fireball(30, 2).execute(caster, target, ally, enemy, new RecordingOutput());

        assertEquals(80, target.getHealth()); // 10 * 2 = 20 damage
    }

    // ═══════════════════════════════════════════════════════════════════
    // Heal
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void heal_getName() {
        assertEquals("Heal", new Heal(35, 1).getName());
    }

    @Test
    void heal_doesNotRequireTarget() {
        assertFalse(new Heal(35, 1).requiresTarget());
    }

    @Test
    void heal_healsLowestHpAlly_whenNotPriest() {
        Unit caster = makeUnit("Healer", HeroClass.ORDER, 5, 5, 100, 100);
        Unit weak   = makeUnit("Weak",   HeroClass.ORDER, 5, 5, 100, 100);
        Unit strong = makeUnit("Strong", HeroClass.ORDER, 5, 5, 100, 100);
        weak.setHealth(30);
        strong.setHealth(70);
        Party ally = party("A", caster, weak, strong);

        new Heal(35, 1).execute(caster, null, ally, new Party("B"), new RecordingOutput());

        assertEquals(55,  weak.getHealth());   // 30 + 25% of 100
        assertEquals(70, strong.getHealth());  // unchanged
    }

    @Test
    void heal_healsAllAllies_whenPriest() {
        Unit priest = makeUnit("Priest", HeroClass.PRIEST, 5, 5, 100, 100);
        Unit a1     = makeUnit("A1",     HeroClass.ORDER,  5, 5, 100, 100);
        Unit a2     = makeUnit("A2",     HeroClass.ORDER,  5, 5, 100, 100);
        a1.setHealth(50);
        a2.setHealth(60);
        Party ally = party("A", priest, a1, a2);

        new Heal(35, 1).execute(priest, null, ally, new Party("B"), new RecordingOutput());

        assertEquals(75, a1.getHealth()); // 50 + 25
        assertEquals(85, a2.getHealth()); // 60 + 25
    }

    @Test
    void heal_effectMultiplierScalesHealAmount() {
        Unit caster = makeUnit("Prophet", HeroClass.ORDER, 5, 5, 100, 100);
        Unit ally   = makeUnit("Ally",    HeroClass.ORDER, 5, 5, 100, 100);
        ally.setHealth(10);
        Party allyParty = party("A", caster, ally);

        new Heal(35, 2).execute(caster, null, allyParty, new Party("B"), new RecordingOutput());

        assertEquals(60, ally.getHealth()); // 10 + 25*2 = 60
    }

    // ═══════════════════════════════════════════════════════════════════
    // Protect
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void protect_getName() {
        assertEquals("Protect", new Protect(25, 1).getName());
    }

    @Test
    void protect_doesNotRequireTarget() {
        assertFalse(new Protect(25, 1).requiresTarget());
    }

    @Test
    void protect_addsShieldEffect_toAllAllies() {
        Unit caster = makeUnit("Cleric", HeroClass.ORDER, 5, 5, 100, 100);
        Unit ally   = makeUnit("Ally",   HeroClass.ORDER, 5, 5, 100, 100);
        Party allyParty = party("A", caster, ally);

        new Protect(25, 1).execute(caster, null, allyParty, new Party("B"), new RecordingOutput());

        assertTrue(caster.getEffects().stream().anyMatch(e -> e instanceof Shield));
        assertTrue(ally.getEffects().stream().anyMatch(e -> e instanceof Shield));
    }

    @Test
    void protect_shieldAmount_isTenPercentOfCurrentHealth() {
        Unit caster = makeUnit("Cleric", HeroClass.ORDER, 5, 5, 100, 100);
        caster.setHealth(80);
        Party allyParty = party("A", caster);

        new Protect(25, 1).execute(caster, null, allyParty, new Party("B"), new RecordingOutput());

        // 10% of 80 = 8 shield
        Shield shield = (Shield) caster.getEffects().stream()
                .filter(e -> e instanceof Shield).findFirst().orElseThrow();
        // Absorb 8 damage: should absorb fully and leave 0 remaining
        assertEquals(0, shield.modifyDamage(null, caster, 8, new RecordingOutput()));
    }

    @Test
    void protect_addsFireShield_whenHeretic() {
        Unit heretic = makeUnit("Heretic", HeroClass.HERETIC, 5, 5, 100, 100);
        Party allyParty = party("A", heretic);

        new Protect(25, 1).execute(heretic, null, allyParty, new Party("B"), new RecordingOutput());

        assertTrue(heretic.getEffects().stream().anyMatch(e -> e instanceof FireShield));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Replenish
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void replenish_getName() {
        assertEquals("Replenish", new Replenish(80, 1).getName());
    }

    @Test
    void replenish_doesNotRequireTarget() {
        assertFalse(new Replenish(80, 1).requiresTarget());
    }

    @Test
    void replenish_givesCaster60Mana_andAllies30() {
        Unit caster = makeUnit("Wizard", HeroClass.MAGE, 5, 5, 100, 10);
        Unit ally   = makeUnit("Ally",   HeroClass.MAGE, 5, 5, 100, 5);
        Party allyParty = party("A", caster, ally);

        caster.setMaxMana(50);
        ally.setMaxMana(50);
        new Replenish(80, 1).execute(caster, null, allyParty, new Party("B"), new RecordingOutput());

        assertEquals(50, caster.getMana()); // 10+60=70 capped at maxMana=50
        assertEquals(35, ally.getMana());   // 5+30=35
    }

    @Test
    void replenish_effectMultiplierScalesMana() {
        Unit caster = makeUnit("Prophet", HeroClass.MAGE, 5, 5, 100, 0);
        Party allyParty = party("A", caster);
        caster.setMaxMana(200);
        new Replenish(80, 2).execute(caster, null, allyParty, new Party("B"), new RecordingOutput());

        assertEquals(120, caster.getMana()); // 0+120 = 120
    }

    // ── Ability base — execute always announces cast ───────────────────────────

    @Test
    void execute_alwaysShowsCastMessage() {
        Unit caster = makeUnit("Merlin", HeroClass.ORDER, 5, 5, 100, 100);
        Party allyParty = party("A", caster);
        RecordingOutput out = new RecordingOutput();

        new Heal(35, 1).execute(caster, null, allyParty, new Party("B"), out);

        assertTrue(out.anyMatch("Merlin casts Heal!"));
    }
}