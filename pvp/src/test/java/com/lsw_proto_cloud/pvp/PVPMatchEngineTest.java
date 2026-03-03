package com.lsw_proto_cloud.pvp;

import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.test.TestHelpers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * pvp/src/test/java/com/lsw_proto_cloud/pvp/PVPMatchEngineTest.java
 */
class PVPMatchEngineTest {

    private static final String BATTLE_URL   = "http://battle";
    private static final String CALLBACK_URL = "http://pvp/battleCallback";

    private RecordingOutput  output;
    private StubRestTemplate restTemplate;
    private Party            playerOne;
    private Party            playerTwo;
    private PVPMatchEngine   engine;

    @BeforeEach
    void setUp() throws Exception {
        output      = new RecordingOutput();
        restTemplate = new StubRestTemplate();
        playerOne    = party("TeamA", warrior("Arthur"));
        playerTwo    = party("TeamB", warrior("Goblin"));

        engine = new PVPMatchEngine(playerOne, playerTwo, BATTLE_URL, CALLBACK_URL, output);
        injectRestTemplate(engine, restTemplate);
    }

    private static void injectRestTemplate(PVPMatchEngine eng, StubRestTemplate stub) throws Exception {
        Field f = PVPMatchEngine.class.getDeclaredField("restTemplate");
        f.setAccessible(true);
        f.set(eng, stub);
    }

    // ── startMatch() ─────────────────────────────────────────────────────────

    @Test
    void startMatch_setsInBattleTrue() {
        engine.startMatch();
        assertTrue(engine.isInBattle());
    }

    @Test
    void startMatch_postsStartRequestToBattleModule() {
        engine.startMatch();
        assertTrue(restTemplate.anyUrlContains("/start"));
    }

    @Test
    void startMatch_includesCallbackUrl_inRequest() {
        engine.startMatch();
        assertTrue(restTemplate.anyBodyContains("callbackURL")
                || restTemplate.anyBodyContains(CALLBACK_URL));
    }

    @Test
    void startMatch_showsStartMessage() {
        engine.startMatch();
        assertTrue(output.anyMatch("Starting PvP match"));
    }

    @Test
    void startMatch_restTemplateFailure_setsInBattleFalse() {
        restTemplate.setShouldThrow(true);
        engine.startMatch();
        assertFalse(engine.isInBattle());
        assertTrue(output.anyMatch("Failed to start"));
    }

    // ── handleAction() ───────────────────────────────────────────────────────

    @Test
    void handleAction_whenNotInBattle_showsNoMatchMessage() {
        // engine not started — isInBattle = false
        engine.handleAction("attack Goblin");
        assertTrue(output.anyMatch("No battle in progress"));
    }

    @Test
    void handleAction_whenInBattle_forwardsToActionUrl() {
        engine.startMatch();
        output.clear();
        engine.handleAction("attack Goblin");
        assertTrue(restTemplate.anyUrlContains("/action"));
    }

    @Test
    void handleAction_forwardsExactAction_toBattleModule() {
        engine.startMatch();
        restTemplate.postedBodies.clear();
        engine.handleAction("defend");
        assertTrue(restTemplate.postedBodies.stream()
                .anyMatch(b -> b.toString().contains("defend")));
    }

    @Test
    void handleAction_finishedResponse_setsInBattleFalse() {
        engine.startMatch();
        restTemplate.setNextResponse("Battle finished");
        engine.handleAction("attack Goblin");
        assertFalse(engine.isInBattle());
        assertTrue(output.anyMatch("PvP match finished"));
    }

    @Test
    void handleAction_restTemplateFailure_showsError() {
        engine.startMatch();
        restTemplate.setShouldThrow(true);
        engine.handleAction("attack Goblin");
        assertTrue(output.anyMatch("Error sending battle action"));
    }

    @Test
    void handleAction_nonFinishedResponse_staysInBattle() {
        engine.startMatch();
        restTemplate.setNextResponse("Action submitted");
        engine.handleAction("wait");
        assertTrue(engine.isInBattle());
    }

    // ── isInBattle() ─────────────────────────────────────────────────────────

    @Test
    void isInBattle_falseBeforeMatchStarts() {
        assertFalse(engine.isInBattle());
    }

    @Test
    void isInBattle_trueAfterSuccessfulStart() {
        engine.startMatch();
        assertTrue(engine.isInBattle());
    }
}