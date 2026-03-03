package com.lsw_proto_cloud.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsw_proto_cloud.core.abilities.Ability;
import com.lsw_proto_cloud.core.effects.Effect;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class Unit {
    private final String name;

    //TODO: classes and levels
    private EnumMap<HeroClass, Integer> classLevels;
    private HeroClass mainClass;

    private final List<Effect> effects;
    private final List<Ability> abilities;
    private int experience;

    private int maxHealth;
    private int maxMana;
    private int attack;
    private int defense;
    private int health;
    private int mana;

    //Constructor
    @JsonCreator
    public Unit(
            @JsonProperty("name") String name,
            @JsonProperty("classLevels") EnumMap<HeroClass, Integer> classLevels,
            @JsonProperty("mainClass") HeroClass mainClass,
            @JsonProperty("effects") List<Effect> effects,
            @JsonProperty("abilities") List<Ability> abilities,
            @JsonProperty("experience") int experience,
            @JsonProperty("maxHealth") int maxHealth,
            @JsonProperty("maxMana") int maxMana,
            @JsonProperty("attack") int attack,
            @JsonProperty("defense") int defense,
            @JsonProperty("health") int health,
            @JsonProperty("mana") int mana
    ) {
        this.name = name;
        this.classLevels = classLevels;
        this.mainClass = mainClass;
        this.effects = effects;
        this.abilities = abilities;
        this.experience = experience;
        this.maxHealth = maxHealth;
        this.maxMana = maxMana;
        this.attack = attack;
        this.defense = defense;
        this.health = health;
        this.mana = mana;
    }

    public Unit(String name, int atk, int def, int maxHp, int maxMp, HeroClass startingClass) {
        this.name = name;

        //TODO: classes
        classLevels = new EnumMap<>(HeroClass.class);
        classLevels.put(HeroClass.ORDER, 0);
        classLevels.put(HeroClass.CHAOS, 0);
        classLevels.put(HeroClass.WARRIOR, 0);
        classLevels.put(HeroClass.MAGE, 0);
        mainClass = startingClass;
        classLevels.put(mainClass, 1); //init starting class to level 1

        effects = new ArrayList<>(startingClass.getEffects());
        abilities = new ArrayList<>(startingClass.getAbilities());
        experience = 0;

        this.attack = atk;
        this.defense = def;
        this.health = this.maxHealth = maxHp;
        this.mana = this.maxMana = maxMp;
    }

    public Unit(String name, HeroClass startingClass) {
        //default stats constructor
        //TODO: 5, 5, 100, 50
        this(name, 5, 5, 5, 50, startingClass);
    }

    //Getters and Setters
    public String getName() {return name;}

    public int getMaxHealth() {return maxHealth;}

    public void setMaxHealth(int maxHealth) {this.maxHealth = maxHealth;}

    public int getMaxMana() {return maxMana;}

    public void setMaxMana(int maxMana) {this.maxMana = maxMana;}

    public int getAttack() {return attack;}

    public void setAttack(int attack) {this.attack = attack;}

    public int getDefense() {return defense;}

    public void setDefense(int defence) {this.defense = defence;}

    public int getHealth() {return health;}

    public void setHealth(int health) {this.health = Math.min(Math.max(0, health), maxHealth);}

    public int getMana() {return mana;}

    public void setMana(int mana) {this.mana = Math.min(Math.max(0, mana), maxMana);}

    public List<Effect> getEffects() {return this.effects;}

    public void addEffect(Effect effect) {this.effects.add(effect);}

    public void removeEffect(Effect effect) {this.effects.remove(effect);}

    public void clearDebuffEffects() {
        effects.removeIf(e -> !mainClass.getEffects().contains(e));
    }

    public List<Ability> getAbilities() {return this.abilities;}

    public void addAbility(Ability ability) {this.abilities.add(ability);}

    public void removeAbility(Ability ability) {this.abilities.remove(ability);}

    @JsonIgnore
    public Ability getAbilityByName(String name) {
        return abilities.stream()
                .filter(ability -> ability.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    //Other Methods
    public int applyDamage(int damage) {
        //apply defense
        damage -= defense;

        //ensure damage is not negative
        damage = Math.max(0, damage);

        //inflict damage onto health
        health = Math.max(0, health - damage);

        //return damage dealt
        return damage;
    }

    //TODO: class/leveling methods
    public EnumMap<HeroClass, Integer> getClassLevels() {return this.classLevels;}
    public void setClassLevels(EnumMap<HeroClass, Integer> classLevels) {this.classLevels = classLevels;}

    public HeroClass getMainClass() {return mainClass;}

    public void setMainClass(HeroClass heroClass) {
        if (mainClass == heroClass) throw new IllegalArgumentException("This unit is already a " + heroClass);
        if (mainClass.isHybrid()) throw new IllegalArgumentException("This unit is a permanent hybrid!");
        if (mainClass.isSpecialization() && !heroClass.isHybrid()) throw new IllegalArgumentException("Specialized units may only upgrade to hybrids!");
        if (heroClass.isSpecialization() && classLevels.get(heroClass.getParentA()) < 5) throw new IllegalArgumentException("This unit does not meet the minimum level to specialize into " + heroClass);
        if (heroClass.isHybrid() && (classLevels.get(heroClass.getParentA()) < 5 || classLevels.get(heroClass.getParentB()) < 5)) throw new  IllegalArgumentException("This unit does not meet he minimum levels to hybridize into " + heroClass);

        //unit may change class
        mainClass = heroClass;
        abilities.clear();
        abilities.addAll(heroClass.getAbilities());
        effects.clear();
        effects.addAll(heroClass.getEffects());
    }

    public void levelUpClass(HeroClass heroClass) {
        //TODO: handle class transformations for hybrid and specialization
        if (!classLevels.containsKey(heroClass)) throw new IllegalArgumentException("This unit does not have this class");
        if (getLevel() >= 20) throw new IllegalArgumentException("This unit is at the max level: " + getLevel());
        if (experience < expNeededForLvl(classLevels.get(heroClass) + 1)) throw new IllegalArgumentException("Not enough experience!");

        //level up class and get stat bonuses
        experience -= expNeededForLvl(classLevels.get(heroClass) + 1);
        classLevels.put(heroClass, classLevels.get(heroClass) + 1);
        attack += 1 + heroClass.getAttackPerLevel();
        defense += 1 + heroClass.getDefensePerLevel();
        health = maxHealth += 5 + heroClass.getHealthPerLevel();
        mana = maxMana += 2 + heroClass.getManaPerLevel();
    }

    @JsonIgnore
    public int getLevel() {
        return classLevels.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int expNeededForLvl(int lvl) {
        if (lvl <= 0) return 0;
        return expNeededForLvl(lvl - 1) + 500 + 75 * lvl + 20 * lvl * lvl;
    }

    public int getExperience() {return experience;}

    public void gainExperience(int experience) {this.experience += experience;}

    @JsonIgnore
    public boolean isAlive() {return health > 0;}

    @JsonIgnore
    public boolean isDead() {return health <= 0;}

    @Override
    public String toString() {
        return String.format("[%s]\tatk: %d|def: %d|hp: %d|mp: %d|lvl: %d|xp: %d, abilities=%s|effects=%s", name, attack,  defense, health, mana, getLevel(), experience, getAbilities(), getEffects());
    }
}
