package com.lsw_proto_cloud.test;

import com.lsw_proto_cloud.core.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared test fakes for the pve/pvp modules.
 * pve/src/test/java/com/lsw_proto_cloud/test/TestHelpers.java
 * pvp/src/test/java/com/lsw_proto_cloud/test/TestHelpers.java  (identical copy)
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

        public void clear() { messages.clear(); }
    }

    // ── RestTemplate stub that records calls and returns preset responses ──────

    public static class StubRestTemplate extends RestTemplate {
        public final List<String> postedUrls    = new ArrayList<>();
        public final List<Object> postedBodies  = new ArrayList<>();
        private String nextResponse = "ok";
        private boolean shouldThrow = false;

        public void setNextResponse(String response) { this.nextResponse = response; }
        public void setShouldThrow(boolean shouldThrow) { this.shouldThrow = shouldThrow; }

        @Override
        public <T> T postForObject(String url, Object request, Class<T> responseType,
                                   Object... uriVariables) {
            if (shouldThrow) throw new RuntimeException("Connection refused");
            postedUrls.add(url);
            postedBodies.add(request);
            return responseType.cast(nextResponse);
        }

        public boolean anyUrlContains(String substr) {
            return postedUrls.stream().anyMatch(u -> u.contains(substr));
        }

        public boolean anyBodyContains(String substr) {
            return postedBodies.stream()
                    .map(Object::toString)
                    .anyMatch(b -> b.contains(substr));
        }
    }

    // ── Factory helpers ───────────────────────────────────────────────────────

    public static Unit makeUnit(String name, HeroClass cls, int atk, int def, int hp, int mp) {
        return new Unit(name, atk, def, hp, mp, cls);
    }

    public static Unit warrior(String name) {
        return makeUnit(name, HeroClass.WARRIOR, 10, 2, 100, 0);
    }

    public static Unit order(String name) {
        return makeUnit(name, HeroClass.ORDER, 5, 5, 100, 50);
    }

    public static Party party(String name, Unit... units) {
        Party p = new Party(name);
        for (Unit u : units) p.addUnit(u);
        return p;
    }
}