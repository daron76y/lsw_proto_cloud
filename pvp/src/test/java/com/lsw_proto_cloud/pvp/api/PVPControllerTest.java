package com.lsw_proto_cloud.pvp.api;

import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.pvp.PVPMatchEngine;
import com.lsw_proto_cloud.test.TestHelpers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * pvp/src/test/java/com/lsw_proto_cloud/pvp/api/PVPControllerTest.java
 *
 * Tests PVPController without Spring context — calls methods directly.
 */
class PVPControllerTest {

    private PVPController    controller;
    private StubRestTemplate restTemplate;
    private Party            partyA;
    private Party            partyB;

    @BeforeEach
    void setUp() {
        controller   = new PVPController();
        restTemplate = new StubRestTemplate();
        partyA       = party("TeamA", warrior("Arthur"));
        partyB       = party("TeamB", warrior("Goblin"));
    }

    // ── startMatch ───────────────────────────────────────────────────────────

    @Test
    void startMatch_returnsConfirmationMessage() throws Exception {
        PVPController.PvPStartRequest req = buildRequest(partyA, partyB);
        String result = controller.startMatch(req, "http://battle", "http://pvp/callback");
        assertTrue(result.contains("TeamA"));
        assertTrue(result.contains("TeamB"));
    }

    @Test
    void startMatch_createsEngine_andCallsStartMatch() throws Exception {
        PVPController.PvPStartRequest req = buildRequest(partyA, partyB);
        controller.startMatch(req, "http://battle", "http://pvp/callback");

        PVPMatchEngine engine = getEngine(controller);
        assertNotNull(engine);
    }

    // ── submitAction ─────────────────────────────────────────────────────────

    @Test
    void submitAction_withNoEngine_returnsNoMatchMessage() {
        String result = controller.submitAction("attack Goblin");
        assertTrue(result.contains("No PvP match"));
    }

    @Test
    void submitAction_whenNotInBattle_returnsNoMatchMessage() throws Exception {
        // Start then simulate battle ending
        PVPController.PvPStartRequest req = buildRequest(partyA, partyB);
        controller.startMatch(req, "http://battle", "http://pvp/callback");

        // Inject stub so startMatch doesn't fail on real HTTP
        PVPMatchEngine engine = getEngine(controller);
        injectRestTemplate(engine, restTemplate);

        // Simulate battle finish via callback
        Unit deadUnit = warrior("Arthur");
        deadUnit.setHealth(0);
        Party deadParty  = party("TeamA", deadUnit);
        Party aliveParty = party("TeamB", warrior("Goblin"));
        controller.receiveBattleResult(Map.of("partyA", deadParty, "partyB", aliveParty));

        String result = controller.submitAction("attack");
        assertTrue(result.contains("No PvP match"));
    }

    // ── receiveBattleResult ───────────────────────────────────────────────────

    @Test
    void receiveBattleResult_returnsWinnerName_whenPartyAWins() throws Exception {
        startAndInjectStub();

        Unit aliveUnit = warrior("Arthur");
        Party winner   = party("TeamA", aliveUnit);
        Party loser    = party("TeamB", warrior("Goblin"));

        String result = controller.receiveBattleResult(Map.of("partyA", winner, "partyB", loser));
        assertTrue(result.contains("TeamA"));
    }

    @Test
    void receiveBattleResult_returnsWinnerName_whenPartyBWins() throws Exception {
        startAndInjectStub();

        Unit deadUnit = warrior("Arthur");
        deadUnit.setHealth(0);
        Party loser  = party("TeamA", deadUnit);
        Party winner = party("TeamB", warrior("Goblin"));

        String result = controller.receiveBattleResult(Map.of("partyA", loser, "partyB", winner));
        assertTrue(result.contains("TeamB"));
    }

    @Test
    void receiveBattleResult_withNoEngine_doesNotThrow() {
        Unit alive = warrior("Arthur");
        Party pa   = party("TeamA", alive);
        Party pb   = party("TeamB", warrior("Goblin"));

        assertDoesNotThrow(() ->
                controller.receiveBattleResult(Map.of("partyA", pa, "partyB", pb)));
    }

    @Test
    void receiveBattleResult_setsEngineNotInBattle() throws Exception {
        startAndInjectStub();

        Unit dead = warrior("Arthur");
        dead.setHealth(0);
        Party loser  = party("TeamA", dead);
        Party winner = party("TeamB", warrior("Goblin"));

        controller.receiveBattleResult(Map.of("partyA", loser, "partyB", winner));

        PVPMatchEngine engine = getEngine(controller);
        assertFalse(engine.isInBattle());
    }

    // ── PvPStartRequest DTO ───────────────────────────────────────────────────

    @Test
    void pvpStartRequest_gettersAndSetters() {
        PVPController.PvPStartRequest req = new PVPController.PvPStartRequest();
        req.setPlayerA(partyA);
        req.setPlayerB(partyB);
        assertSame(partyA, req.getPlayerA());
        assertSame(partyB, req.getPlayerB());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private PVPController.PvPStartRequest buildRequest(Party a, Party b) {
        PVPController.PvPStartRequest req = new PVPController.PvPStartRequest();
        req.setPlayerA(a);
        req.setPlayerB(b);
        return req;
    }

    /** Starts a match and injects the stub RestTemplate into the engine. */
    private void startAndInjectStub() throws Exception {
        PVPController.PvPStartRequest req = buildRequest(partyA, partyB);
        controller.startMatch(req, "http://battle", "http://pvp/callback");
        injectRestTemplate(getEngine(controller), restTemplate);
    }

    private static PVPMatchEngine getEngine(PVPController ctrl) throws Exception {
        Field f = PVPController.class.getDeclaredField("engine");
        f.setAccessible(true);
        return (PVPMatchEngine) f.get(ctrl);
    }

    private static void injectRestTemplate(PVPMatchEngine eng, StubRestTemplate stub) throws Exception {
        Field f = PVPMatchEngine.class.getDeclaredField("restTemplate");
        f.setAccessible(true);
        f.set(eng, stub);
    }
}