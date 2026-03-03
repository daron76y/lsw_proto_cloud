package com.lsw_proto_cloud.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class Party {
    private final String name;
    private int gold;
    private final EnumMap<Items, Integer> inventory;
    private final List<Unit> units;

    //Constructors
    @JsonCreator
    public Party( //jackson
        @JsonProperty("name") String name,
        @JsonProperty("gold") int gold,
        @JsonProperty("inventory") EnumMap<Items, Integer> inventory,
        @JsonProperty("units") List<Unit> units
    ) {
        this.name = name;
        this.gold = gold;
        this.inventory = inventory;
        this.units = units;
    }

    public Party(String name) {
        this.name = name;
        this.units = new ArrayList<Unit>();
        this.gold = 0;
        this.inventory = new EnumMap<>(Items.class);
    }

    //Methods
    public String getName() {return name;}

    public int getGold() {return gold;}

    public void setGold(int gold) {this.gold = gold;}

    public EnumMap<Items, Integer> getInventory() {return inventory;}

    public void addItem(Items item) {inventory.put(item, inventory.getOrDefault(item, 0) + 1);}

    public void removeItem(Items item) {inventory.put(item, Math.max(inventory.getOrDefault(item, 0) - 1, 0));}

    public void addUnit(Unit unit) {
        if (units.contains(unit)) throw new IllegalStateException("Party already contains unit: " + unit.getName());
        if (units.size() >= 5) throw new IllegalStateException("Party is full");
        units.add(unit);
    }

    public void removeUnit(Unit unit) {
        if (units.contains(unit)) units.remove(unit);
        else throw new IllegalArgumentException("Party does not contain unit: " + unit.getName());
    }

    public List<Unit> getUnits() {return units;}

    @JsonIgnore
    public List<Unit> getAliveUnits() {
        return units.stream().filter(Unit::isAlive).toList();
    }

    @JsonIgnore
    public int getNumAliveUnits() {
        return getAliveUnits().size();
    }

    @JsonIgnore
    public Unit getUnitByName(String name) {
        return units.stream()
                .filter(unit -> unit.getName().equalsIgnoreCase(name) && unit.isAlive())
                .findFirst()
                .orElse(null);
    }

    //TODO: uml
    @JsonIgnore
    public int getCumulativeLevels() {
        return units.stream().mapToInt(Unit::getLevel).sum();
    }

    @JsonIgnore
    public boolean isDefeated() {
        return units.stream().allMatch(Unit::isDead);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Party Name: ").append(name).append("\n");
        units.forEach(unit -> {
            sb.append(unit.toString()).append("\n");
        });
        return sb.toString();
    }
}
