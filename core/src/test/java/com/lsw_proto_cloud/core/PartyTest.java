package com.lsw_proto_cloud.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

class PartyTest {

    private Party party;
    private Unit  unitA;
    private Unit  unitB;

    @BeforeEach
    void setUp() {
        party = new Party("Heroes");
        unitA = warrior("Arthur");
        unitB = warrior("Lancelot");
    }

    // ── addUnit ───────────────────────────────────────────────────────────────

    @Test
    void addUnit_addsUnitToList() {
        party.addUnit(unitA);
        assertTrue(party.getUnits().contains(unitA));
    }

    @Test
    void addUnit_throwsOnDuplicate() {
        party.addUnit(unitA);
        assertThrows(IllegalStateException.class, () -> party.addUnit(unitA));
    }

    @Test
    void addUnit_throwsWhenFull() {
        for (int i = 0; i < 5; i++) party.addUnit(warrior("U" + i));
        assertThrows(IllegalStateException.class, () -> party.addUnit(unitA));
    }

    // ── removeUnit ────────────────────────────────────────────────────────────

    @Test
    void removeUnit_removesFromList() {
        party.addUnit(unitA);
        party.removeUnit(unitA);
        assertFalse(party.getUnits().contains(unitA));
    }

    @Test
    void removeUnit_throwsWhenUnitNotPresent() {
        assertThrows(IllegalArgumentException.class, () -> party.removeUnit(unitA));
    }

    // ── getUnitByName ─────────────────────────────────────────────────────────

    @Test
    void getUnitByName_returnsAliveUnit() {
        party.addUnit(unitA);
        assertSame(unitA, party.getUnitByName("Arthur"));
    }

    @Test
    void getUnitByName_isCaseInsensitive() {
        party.addUnit(unitA);
        assertSame(unitA, party.getUnitByName("arthur"));
    }

    @Test
    void getUnitByName_returnsNull_forDeadUnit() {
        unitA.setHealth(0);
        party.addUnit(unitA);
        assertNull(party.getUnitByName("Arthur"));
    }

    @Test
    void getUnitByName_returnsNull_whenNameNotFound() {
        assertNull(party.getUnitByName("Nobody"));
    }

    // ── isDefeated ────────────────────────────────────────────────────────────

    @Test
    void isDefeated_true_whenAllUnitsDead() {
        unitA.setHealth(0);
        unitB.setHealth(0);
        party.addUnit(unitA);
        party.addUnit(unitB);
        assertTrue(party.isDefeated());
    }

    @Test
    void isDefeated_false_whenAtLeastOneUnitAlive() {
        unitA.setHealth(0);
        party.addUnit(unitA);
        party.addUnit(unitB);
        assertFalse(party.isDefeated());
    }

    @Test
    void isDefeated_true_whenPartyIsEmpty() {
        assertTrue(party.isDefeated());
    }

    // ── getAliveUnits / getNumAliveUnits ──────────────────────────────────────

    @Test
    void getAliveUnits_excludesDeadUnits() {
        unitA.setHealth(0);
        party.addUnit(unitA);
        party.addUnit(unitB);
        assertFalse(party.getAliveUnits().contains(unitA));
        assertTrue(party.getAliveUnits().contains(unitB));
    }

    @Test
    void getNumAliveUnits_countsOnlyLivingUnits() {
        unitA.setHealth(0);
        party.addUnit(unitA);
        party.addUnit(unitB);
        assertEquals(1, party.getNumAliveUnits());
    }

    // ── inventory ─────────────────────────────────────────────────────────────

    @Test
    void addItem_incrementsItemCount() {
        party.addItem(Items.BREAD);
        party.addItem(Items.BREAD);
        assertEquals(2, party.getInventory().get(Items.BREAD));
    }

    @Test
    void removeItem_decrementsItemCount() {
        party.addItem(Items.BREAD);
        party.addItem(Items.BREAD);
        party.removeItem(Items.BREAD);
        assertEquals(1, party.getInventory().get(Items.BREAD));
    }

    @Test
    void removeItem_floorAtZero_whenItemNeverAdded() {
        party.removeItem(Items.BREAD);
        assertEquals(0, party.getInventory().get(Items.BREAD));
    }

    // ── gold ─────────────────────────────────────────────────────────────────

    @Test
    void setGold_updatesGoldValue() {
        party.setGold(500);
        assertEquals(500, party.getGold());
    }

    @Test
    void newParty_startsWithZeroGold() {
        assertEquals(0, party.getGold());
    }

    // ── getCumulativeLevels ───────────────────────────────────────────────────

    @Test
    void getCumulativeLevels_sumOfAllUnitLevels() {
        party.addUnit(unitA); // level 1
        party.addUnit(unitB); // level 1
        assertEquals(2, party.getCumulativeLevels());
    }

    // ── getName / toString ────────────────────────────────────────────────────

    @Test
    void getName_returnsPartyName() {
        assertEquals("Heroes", party.getName());
    }

    @Test
    void toString_containsPartyName() {
        assertTrue(party.toString().contains("Heroes"));
    }
}