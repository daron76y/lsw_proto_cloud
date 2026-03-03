package com.lsw_proto_cloud.test;

import com.lsw_proto_cloud.battle.*;
import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.core.abilities.Ability;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared test fakes — no Mockito.
 * Place in src/test/java/com/lsw_proto_cloud/test/
 */
public class TestHelpers {

    // ── OutputService that records messages ───────────────────────────────────

    public static class RecordingOutput implements OutputService {
        public final List<String> messages = new ArrayList<>();

        @Override public void showMessage(String msg)        { messages.add(msg); }
        @Override public void showParty(List<Party> p)       {}
        @Override public void announceTurn(Unit u)           {}
        @Override public void showUnitBasic(Unit u)          {}
        @Override public void showUnitAdvanced(Unit u)       {}
        @Override public void showInventory(Party p)         {}
        @Override public void showItemShop()                 {}

        public boolean anyMatch(String substr) {
            return messages.stream().anyMatch(m -> m.contains(substr));
        }
    }

    // ── Battle spy that records every call ────────────────────────────────────

    public static class SpyBattle implements Battle {
        public record AttackCall(Unit attacker, Unit target) {}
        public record CastCall(Unit caster, Unit target, Party ally, Party enemy, Ability ability) {}

        public final List<AttackCall> attacks = new ArrayList<>();
        public final List<Unit>       defends = new ArrayList<>();
        public final List<Unit>       waits   = new ArrayList<>();
        public final List<CastCall>   casts   = new ArrayList<>();

        @Override public void attack(Unit a, Unit t)                                { attacks.add(new AttackCall(a, t)); }
        @Override public void defend(Unit u)                                        { defends.add(u); }
        @Override public void wait(Unit u)                                          { waits.add(u); }
        @Override public void cast(Unit c, Unit t, Party al, Party en, Ability ab) { casts.add(new CastCall(c, t, al, en, ab)); }
    }

    // ── Factory helpers ───────────────────────────────────────────────────────

    /** Unit with explicit stats. */
    public static Unit makeUnit(String name, HeroClass cls, int atk, int def, int hp, int mp) {
        return new Unit(name, atk, def, hp, mp, cls);
    }

    /** WARRIOR: 10 atk, 2 def, 100 hp, 0 mp. */
    public static Unit warrior(String name) {
        return makeUnit(name, HeroClass.WARRIOR, 10, 2, 100, 0);
    }

    /** ORDER: 5 atk, 5 def, 100 hp, 50 mp. */
    public static Unit order(String name) {
        return makeUnit(name, HeroClass.ORDER, 5, 5, 100, 50);
    }

    /** Party containing the given units. */
    public static Party party(String name, Unit... units) {
        Party p = new Party(name);
        for (Unit u : units) p.addUnit(u);
        return p;
    }
}