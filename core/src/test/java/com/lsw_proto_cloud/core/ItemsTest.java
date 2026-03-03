package com.lsw_proto_cloud.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class ItemsTest {

    @ParameterizedTest
    @EnumSource(Items.class)
    void allItems_havePositiveCost(Items item) {
        assertTrue(item.getCost() > 0, item + " should have positive cost");
    }

    @Test
    void elixir_fullRestores_healthAndMana() {
        assertEquals(Integer.MAX_VALUE, Items.ELIXIR.getHealthBoost());
        assertEquals(Integer.MAX_VALUE, Items.ELIXIR.getManaBoost());
    }

    @Test
    void foodItems_haveZeroManaBoost() {
        assertEquals(0, Items.BREAD.getManaBoost());
        assertEquals(0, Items.CHEESE.getManaBoost());
        assertEquals(0, Items.STEAK.getManaBoost());
    }

    @Test
    void drinkItems_haveZeroHealthBoost() {
        assertEquals(0, Items.WATER.getHealthBoost());
        assertEquals(0, Items.JUICE.getHealthBoost());
        assertEquals(0, Items.WINE.getHealthBoost());
    }

    @Test
    void costIncreases_withItemQuality() {
        assertTrue(Items.CHEESE.getCost() > Items.BREAD.getCost());
        assertTrue(Items.STEAK.getCost()  > Items.CHEESE.getCost());
        assertTrue(Items.JUICE.getCost()  > Items.WATER.getCost());
        assertTrue(Items.WINE.getCost()   > Items.JUICE.getCost());
    }

    @Test
    void fromString_isCaseInsensitive() {
        assertEquals(Items.BREAD, Items.fromString("bread"));
        assertEquals(Items.WINE,  Items.fromString("WINE"));
        assertEquals(Items.STEAK, Items.fromString("Steak"));
    }

    @Test
    void fromString_throwsOnUnknown() {
        assertThrows(IllegalArgumentException.class, () -> Items.fromString("POTION"));
    }
}