
package com.lsw_proto_cloud.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class HeroClassTest {

    // ── isBase / isHybrid / isSpecialization ──────────────────────────────────

    @Test
    void baseClasses_reportIsBase() {
        assertTrue(HeroClass.ORDER.isBase());
        assertTrue(HeroClass.CHAOS.isBase());
        assertTrue(HeroClass.WARRIOR.isBase());
        assertTrue(HeroClass.MAGE.isBase());
    }

    @Test
    void baseClasses_areNeitherHybridNorSpecialization() {
        for (HeroClass base : new HeroClass[]{HeroClass.ORDER, HeroClass.CHAOS, HeroClass.WARRIOR, HeroClass.MAGE}) {
            assertFalse(base.isHybrid(),          base + " should not be hybrid");
            assertFalse(base.isSpecialization(),  base + " should not be specialization");
        }
    }

    @Test
    void hybridClasses_reportIsHybrid() {
        assertTrue(HeroClass.PALADIN.isHybrid());   // ORDER + WARRIOR
        assertTrue(HeroClass.ROGUE.isHybrid());     // CHAOS + WARRIOR
        assertTrue(HeroClass.SORCERER.isHybrid());  // CHAOS + MAGE
        assertTrue(HeroClass.HERETIC.isHybrid());   // ORDER + CHAOS
        assertTrue(HeroClass.PROPHET.isHybrid());   // ORDER + MAGE
        assertTrue(HeroClass.WARLOCK.isHybrid());   // WARRIOR + MAGE
    }

    @Test
    void specializationClasses_reportIsSpecialization() {
        assertTrue(HeroClass.PRIEST.isSpecialization());   // ORDER + ORDER
        assertTrue(HeroClass.INVOKER.isSpecialization());  // CHAOS + CHAOS
        assertTrue(HeroClass.KNIGHT.isSpecialization());   // WARRIOR + WARRIOR
        assertTrue(HeroClass.WIZARD.isSpecialization());   // MAGE + MAGE
    }

    // ── hybridOf ─────────────────────────────────────────────────────────────

    @Test
    void hybridOf_returnsCorrectHybrid() {
        assertEquals(HeroClass.PALADIN, HeroClass.hybridOf(HeroClass.ORDER,   HeroClass.WARRIOR));
        assertEquals(HeroClass.HERETIC, HeroClass.hybridOf(HeroClass.ORDER,   HeroClass.CHAOS));
        assertEquals(HeroClass.PROPHET, HeroClass.hybridOf(HeroClass.ORDER,   HeroClass.MAGE));
        assertEquals(HeroClass.ROGUE,   HeroClass.hybridOf(HeroClass.CHAOS,   HeroClass.WARRIOR));
        assertEquals(HeroClass.SORCERER,HeroClass.hybridOf(HeroClass.CHAOS,   HeroClass.MAGE));
        assertEquals(HeroClass.WARLOCK, HeroClass.hybridOf(HeroClass.WARRIOR, HeroClass.MAGE));
    }

    @Test
    void hybridOf_isCommutative() {
        assertEquals(
                HeroClass.hybridOf(HeroClass.ORDER,   HeroClass.WARRIOR),
                HeroClass.hybridOf(HeroClass.WARRIOR, HeroClass.ORDER)
        );
    }

    // ── stat accumulation ─────────────────────────────────────────────────────

    @Test
    void hybridStats_areSumOfBothParents() {
        // PALADIN = ORDER + WARRIOR
        assertEquals(HeroClass.ORDER.getAttackPerLevel()   + HeroClass.WARRIOR.getAttackPerLevel(),
                HeroClass.PALADIN.getAttackPerLevel());
        assertEquals(HeroClass.ORDER.getDefensePerLevel()  + HeroClass.WARRIOR.getDefensePerLevel(),
                HeroClass.PALADIN.getDefensePerLevel());
        assertEquals(HeroClass.ORDER.getHealthPerLevel()   + HeroClass.WARRIOR.getHealthPerLevel(),
                HeroClass.PALADIN.getHealthPerLevel());
        assertEquals(HeroClass.ORDER.getManaPerLevel()     + HeroClass.WARRIOR.getManaPerLevel(),
                HeroClass.PALADIN.getManaPerLevel());
    }

    @Test
    void specializationStats_areSumOfBothParents() {
        // KNIGHT = WARRIOR + WARRIOR
        assertEquals(HeroClass.WARRIOR.getAttackPerLevel() * 2, HeroClass.KNIGHT.getAttackPerLevel());
    }

    // ── fromString ────────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({"order,ORDER", "CHAOS,CHAOS", "warrior,WARRIOR", "Mage,MAGE", "paladin,PALADIN"})
    void fromString_isCaseInsensitive(String input, String expected) {
        assertEquals(HeroClass.valueOf(expected), HeroClass.fromString(input));
    }

    @Test
    void fromString_throwsOnUnknown() {
        assertThrows(IllegalArgumentException.class, () -> HeroClass.fromString("DRAGON"));
    }

    // ── getAbilities / getEffects not null ────────────────────────────────────

    @Test
    void allClasses_haveNonNullAbilitiesAndEffects() {
        for (HeroClass hc : HeroClass.values()) {
            assertNotNull(hc.getAbilities(), hc + " abilities null");
            assertNotNull(hc.getEffects(),   hc + " effects null");
        }
    }

    // ── parent references ─────────────────────────────────────────────────────

    @Test
    void hybridClasses_haveCorrectParents() {
        assertEquals(HeroClass.ORDER,   HeroClass.PALADIN.getParentA());
        assertEquals(HeroClass.WARRIOR, HeroClass.PALADIN.getParentB());
    }

    @Test
    void baseClasses_haveNullParents() {
        assertNull(HeroClass.WARRIOR.getParentA());
        assertNull(HeroClass.WARRIOR.getParentB());
    }
}