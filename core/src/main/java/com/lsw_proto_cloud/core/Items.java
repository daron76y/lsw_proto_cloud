package com.lsw_proto_cloud.core;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Items {
    BREAD(200, 20, 0),
    CHEESE(500, 50, 0),
    STEAK(1000, 200, 0),
    WATER(150, 0, 10),
    JUICE(400, 0, 30),
    WINE(750, 0, 100),
    ELIXIR(2000, Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int cost;
    private final int healthBoost;
    private final int manaBoost;

    @JsonCreator
    public static Items fromString(String value) {return Items.valueOf(value.toUpperCase());}

    Items(int cost, int healthBoost, int manaBoost) {
        this.cost = cost;
        this.healthBoost = healthBoost;
        this.manaBoost = manaBoost;
    }

    public int getCost() {return this.cost;}
    public int getHealthBoost() {return this.healthBoost;}
    public int getManaBoost() {return this.manaBoost;}
}
