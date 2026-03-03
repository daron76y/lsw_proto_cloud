package com.lsw_proto_cloud.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lsw_proto_cloud.core.abilities.*;
import com.lsw_proto_cloud.core.effects.Effect;
import com.lsw_proto_cloud.core.effects.ManaBurn;
import com.lsw_proto_cloud.core.effects.SneakAttack;

import java.util.*;

public enum HeroClass {
    //base classes
    ORDER(
        0, 2, 0, 5,
        List.of(
            new Protect(25, 1),
            new Heal(35, 1)
        )
    ),
    CHAOS(
        3, 0, 5, 0,
        List.of(
            new Fireball(30, 1),
            new ChainLightning(40, 0.25)
        )
    ),
    WARRIOR(
        2, 3, 0, 0,
        List.of(
            new BerserkerAttack(60)
        )
    ),
    MAGE(
        1, 0, 0, 5,
        List.of(
            new Replenish(80, 1)
        )
    ),

    //hybrid classes
    PRIEST(ORDER, ORDER,
        List.of(
            new Heal(35, 1)
        ),
        List.of()
    ),
    HERETIC(ORDER, CHAOS,
        List.of(
            new Protect(25, 1)
        ),
        List.of()
    ),
    PALADIN(ORDER, WARRIOR,
        List.of(
            new BerserkerAttack(60)
        ),
        List.of()
    ),
    PROPHET(ORDER, MAGE,
        List.of(
            new Heal(35, 2),
            new Protect(25, 2),
            new Replenish(80, 2)),
        List.of()
    ),
    INVOKER(CHAOS, CHAOS,
        List.of(
            new ChainLightning(40, 0.50)),
        List.of()
    ),
    ROGUE(CHAOS, WARRIOR,
        List.of(
            new Fireball(30, 1),
            new ChainLightning(40, 0.25),
            new BerserkerAttack(60)
        ),
        List.of(
            new SneakAttack()
        )
    ),
    SORCERER(CHAOS, MAGE,
        List.of(
            new Fireball(30, 2)),
        List.of()
    ),
    KNIGHT(WARRIOR, WARRIOR,
        List.of(
            new BerserkerAttack(60)),
        List.of()
    ),
    WARLOCK(WARRIOR, MAGE,
        List.of(
            new BerserkerAttack(60),
            new Replenish(80, 1)
        ),
        List.of(
            new ManaBurn()
        )
    ),
    WIZARD(MAGE, MAGE,
        List.of(
            new Replenish(40, 1)),
        List.of()
    );

    @JsonCreator
    public static HeroClass fromString(String value) {
        return HeroClass.valueOf(value.toUpperCase());
    }

    //fields
    private final int attackPerLevel;
    private final int defensePerLevel;
    private final int healthPerLevel;
    private final int manaPerLevel;
    private final List<Ability> abilities;
    private final List<Effect> effects;
    private final HeroClass parentA;
    private final HeroClass parentB;

    //static hybrid map
    private static final Map<Set<HeroClass>, HeroClass> hybrids = new HashMap<>();
    static {
        for (HeroClass heroClass : HeroClass.values()) { //iterate through all classes
            if (heroClass.parentA != null && heroClass.parentB != null) { //this is a hybrid
                Set<HeroClass> pair = new HashSet<>(Set.of());
                pair.add(heroClass.parentA);
                pair.add(heroClass.parentB);
                hybrids.put(pair, heroClass);
            }
        }
    }

    //base class constructor
    HeroClass(int attackPerLevel,  int defensePerLevel, int healthPerLevel, int manaPerLevel, List<Ability> abilities) {
        this.attackPerLevel = attackPerLevel;
        this.defensePerLevel = defensePerLevel;
        this.healthPerLevel = healthPerLevel;
        this.manaPerLevel = manaPerLevel;
        this.abilities = abilities;
        this.effects = List.of(); //base classes don't grant any effects
        this.parentA = null;
        this.parentB = null;
    }

    //hybrid class constructor
    HeroClass(HeroClass parentA, HeroClass parentB, List<Ability> abilities, List<Effect> effects) {
        this.attackPerLevel = parentA.attackPerLevel + parentB.attackPerLevel;
        this.defensePerLevel = parentA.defensePerLevel + parentB.defensePerLevel;
        this.healthPerLevel = parentA.healthPerLevel + parentB.healthPerLevel;
        this.manaPerLevel = parentA.manaPerLevel + parentB.manaPerLevel;
        this.abilities = abilities;
        this.effects = effects;
        this.parentA = parentA;
        this.parentB = parentB;
    }

    //getters
    public int getAttackPerLevel() {return attackPerLevel;}
    public int getDefensePerLevel() {return defensePerLevel;}
    public int getHealthPerLevel() {return healthPerLevel;}
    public int getManaPerLevel() {return manaPerLevel;}
    public List<Ability> getAbilities() {return abilities;}
    public List<Effect> getEffects() {return effects;}
    public HeroClass getParentA() {return parentA;}
    public HeroClass getParentB() {return parentB;}

    public static HeroClass hybridOf(HeroClass a, HeroClass b) {
        return hybrids.get(Set.of(a, b));
    }

    @JsonIgnore public boolean isHybrid() {return parentA != null && parentB != null && parentA != parentB;}
    @JsonIgnore public boolean isSpecialization() {return parentA != null && parentB != null && parentA == parentB;}
    @JsonIgnore public boolean isBase() {return parentA == null && parentB == null;}
}
