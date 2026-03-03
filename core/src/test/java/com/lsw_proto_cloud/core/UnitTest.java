package com.lsw_proto_cloud.core;

import com.lsw_proto_cloud.core.abilities.Heal;
import com.lsw_proto_cloud.core.effects.Stunned;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

class UnitTest {

    private Unit unit; // WARRIOR: 10 atk, 2 def, 100 hp, 0 mp

    @BeforeEach
    void setUp() {
        unit = warrior("Arthur");
    }

    // ── isAlive / isDead ──────────────────────────────────────────────────────

    @Test
    void isAlive_whenHealthAboveZero() {
        assertTrue(unit.isAlive());
        assertFalse(unit.isDead());
    }

    @Test
    void isDead_whenHealthIsZero() {
        unit.setHealth(0);
        assertTrue(unit.isDead());
        assertFalse(unit.isAlive());
    }

    // ── setHealth clamping ────────────────────────────────────────────────────

    @Test
    void setHealth_clampsAtMaxHealth() {
        unit.setHealth(9999);
        assertEquals(unit.getMaxHealth(), unit.getHealth());
    }

    @Test
    void setHealth_clampsAtZero() {
        unit.setHealth(-50);
        assertEquals(0, unit.getHealth());
    }

    // ── setMana clamping ──────────────────────────────────────────────────────

    @Test
    void setMana_clampsAtMaxMana() {
        Unit m = order("Merlin"); // 50 mp
        m.setMana(9999);
        assertEquals(m.getMaxMana(), m.getMana());
    }

    @Test
    void setMana_clampsAtZero() {
        Unit m = order("Merlin");
        m.setMana(-99);
        assertEquals(0, m.getMana());
    }

    // ── applyDamage ───────────────────────────────────────────────────────────

    @Test
    void applyDamage_returnsNetDamageDealt() {
        // unit: 2 def. Attack for 20 → net = 18
        assertEquals(18, unit.applyDamage(20));
    }

    @Test
    void applyDamage_reducesHealthByNetDamage() {
        unit.applyDamage(20); // net 18
        assertEquals(82, unit.getHealth());
    }

    @Test
    void applyDamage_returnsZero_whenDamageBelowDefense() {
        assertEquals(0, unit.applyDamage(1)); // 1 - 2 def = negative → 0
        assertEquals(100, unit.getHealth());
    }

    @Test
    void applyDamage_healthNeverGoesNegative() {
        unit.applyDamage(99999);
        assertEquals(0, unit.getHealth());
    }

    // ── effects ───────────────────────────────────────────────────────────────

    @Test
    void addEffect_addsToList() {
        Stunned s = new Stunned(2);
        unit.addEffect(s);
        assertTrue(unit.getEffects().contains(s));
    }

    @Test
    void removeEffect_removesFromList() {
        Stunned s = new Stunned(2);
        unit.addEffect(s);
        unit.removeEffect(s);
        assertFalse(unit.getEffects().contains(s));
    }

    @Test
    void clearDebuffEffects_removesNonInnateEffects() {
        // WARRIOR has no innate effects — any added effect is a debuff
        unit.addEffect(new Stunned(3));
        unit.clearDebuffEffects();
        assertTrue(unit.getEffects().isEmpty());
    }

    @Test
    void clearDebuffEffects_retainsInnateClassEffects() {
        // ROGUE's SneakAttack is an innate effect
        Unit rogue = new Unit("Scarlet", HeroClass.ROGUE);
        int innate = rogue.getEffects().size();
        rogue.addEffect(new Stunned(2));
        rogue.clearDebuffEffects();
        assertEquals(innate, rogue.getEffects().size());
    }

    // ── abilities ─────────────────────────────────────────────────────────────

    @Test
    void getAbilityByName_returnsCorrectAbility() {
        String name = unit.getAbilities().get(0).getName();
        assertNotNull(unit.getAbilityByName(name));
    }

    @Test
    void getAbilityByName_isCaseInsensitive() {
        String lower = unit.getAbilities().get(0).getName().toLowerCase();
        assertNotNull(unit.getAbilityByName(lower));
    }

    @Test
    void getAbilityByName_returnsNull_whenNotFound() {
        assertNull(unit.getAbilityByName("Nonexistent"));
    }

    @Test
    void addAbility_addsToList() {
        Heal h = new Heal(10, 1);
        unit.addAbility(h);
        assertTrue(unit.getAbilities().contains(h));
    }

    @Test
    void removeAbility_removesFromList() {
        Heal h = new Heal(10, 1);
        unit.addAbility(h);
        unit.removeAbility(h);
        assertFalse(unit.getAbilities().contains(h));
    }

    // ── leveling ──────────────────────────────────────────────────────────────

    @Test
    void newUnit_startsAtLevelOne() {
        assertEquals(1, unit.getLevel());
    }

    @Test
    void getLevel_isSumOfAllClassLevels() {
        unit.gainExperience(unit.expNeededForLvl(2));
        unit.levelUpClass(HeroClass.WARRIOR);
        // WARRIOR now at 2, all others at 0 → total = 2
        assertEquals(2, unit.getLevel());
    }

    @Test
    void gainExperience_incrementsExperience() {
        unit.gainExperience(500);
        assertEquals(500, unit.getExperience());
    }

    @Test
    void levelUpClass_increasesLevel() {
        unit.gainExperience(unit.expNeededForLvl(2));
        unit.levelUpClass(HeroClass.WARRIOR);
        assertEquals(2, unit.getLevel());
    }

    @Test
    void levelUpClass_boostsStats() {
        int atkBefore = unit.getAttack();
        int defBefore = unit.getDefense();
        int hpBefore  = unit.getMaxHealth();
        int mpBefore  = unit.getMaxMana();
        unit.gainExperience(unit.expNeededForLvl(2));
        unit.levelUpClass(HeroClass.WARRIOR);
        assertTrue(unit.getAttack()    > atkBefore);
        assertTrue(unit.getDefense()   > defBefore);
        assertTrue(unit.getMaxHealth() > hpBefore);
        assertTrue(unit.getMaxMana()   > mpBefore);
    }

    @Test
    void levelUpClass_consumesExperience() {
        int xpNeeded = unit.expNeededForLvl(2);
        unit.gainExperience(xpNeeded);
        unit.levelUpClass(HeroClass.WARRIOR);
        assertEquals(0, unit.getExperience());
    }

    @Test
    void levelUpClass_throwsWithInsufficientExperience() {
        assertThrows(IllegalArgumentException.class,
                () -> unit.levelUpClass(HeroClass.WARRIOR));
    }

    @Test
    void levelUpClass_throwsForUnownedClass() {
        // PALADIN is a hybrid — not in the base classLevels map
        assertThrows(IllegalArgumentException.class,
                () -> unit.levelUpClass(HeroClass.PALADIN));
    }

    @Test
    void expNeededForLvl_zeroAtLevelZero() {
        assertEquals(0, unit.expNeededForLvl(0));
    }

    @Test
    void expNeededForLvl_strictlyIncreasing() {
        int prev = unit.expNeededForLvl(1);
        for (int lvl = 2; lvl <= 5; lvl++) {
            int next = unit.expNeededForLvl(lvl);
            assertTrue(next > prev, "xp curve should increase each level");
            prev = next;
        }
    }

    // ── setMainClass ──────────────────────────────────────────────────────────

    @Test
    void setMainClass_throwsWhenSameClass() {
        assertThrows(IllegalArgumentException.class,
                () -> unit.setMainClass(HeroClass.WARRIOR));
    }

    @Test
    void setMainClass_throwsWhenAlreadyHybrid() {
        Unit rogue = new Unit("Scarlet", HeroClass.ROGUE);
        assertThrows(IllegalArgumentException.class,
                () -> rogue.setMainClass(HeroClass.WARRIOR));
    }

    @Test
    void setMainClass_throwsWhenSpecializationLevelNotMet() {
        // KNIGHT needs WARRIOR ≥ 5; unit is only level 1
        assertThrows(IllegalArgumentException.class,
                () -> unit.setMainClass(HeroClass.KNIGHT));
    }

    @Test
    void setMainClass_throwsWhenHybridParentLevelsNotMet() {
        // PALADIN needs ORDER ≥ 5 and WARRIOR ≥ 5
        assertThrows(IllegalArgumentException.class,
                () -> unit.setMainClass(HeroClass.PALADIN));
    }

    @Test
    void setMainClass_succeeds_andUpdatesAbilitiesAndEffects() {
        // Level WARRIOR to 5 to unlock KNIGHT specialization
        for (int lvl = 2; lvl <= 5; lvl++) {
            unit.gainExperience(unit.expNeededForLvl(lvl));
            unit.levelUpClass(HeroClass.WARRIOR);
        }
        unit.setMainClass(HeroClass.KNIGHT);
        assertEquals(HeroClass.KNIGHT, unit.getMainClass());
        assertEquals(HeroClass.KNIGHT.getAbilities().size(), unit.getAbilities().size());
        assertEquals(HeroClass.KNIGHT.getEffects().size(),   unit.getEffects().size());
    }

    @Test
    void setMainClass_specializationCanOnlyUpgradeToHybrid() {
        for (int lvl = 2; lvl <= 5; lvl++) {
            unit.gainExperience(unit.expNeededForLvl(lvl));
            unit.levelUpClass(HeroClass.WARRIOR);
        }
        unit.setMainClass(HeroClass.KNIGHT);
        // KNIGHT is a specialization — cannot revert to a base class
        assertThrows(IllegalArgumentException.class,
                () -> unit.setMainClass(HeroClass.WARRIOR));
    }

    // ── getName / toString ────────────────────────────────────────────────────

    @Test
    void getName_returnsCorrectName() {
        assertEquals("Arthur", unit.getName());
    }

    @Test
    void toString_containsName() {
        assertTrue(unit.toString().contains("Arthur"));
    }
}