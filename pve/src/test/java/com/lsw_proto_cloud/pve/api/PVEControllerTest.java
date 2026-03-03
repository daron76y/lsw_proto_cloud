package com.lsw_proto_cloud.pve.api;

import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.pve.PVECampaignEngine;
import com.lsw_proto_cloud.test.TestHelpers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * pve/src/test/java/com/lsw_proto_cloud/pve/api/PVEControllerTest.java
 *
 * Tests PVEController without a Spring context — calls methods directly.
 */
class PVEControllerTest {

    private PVEController    controller;
    private StubRestTemplate restTemplate;
    private Party            playerParty;

    @BeforeEach
    void setUp() {
        controller  = new PVEController();
        restTemplate = new StubRestTemplate();
        playerParty  = party("Heroes", warrior("Arthur"), order("Merlin"));
    }

    // ── startCampaign ─────────────────────────────────────────────────────────

    @Test
    void startCampaign_returnsConfirmationWithPartyName() {
        String result = controller.startCampaign(
                playerParty, "http://battle", "http://pve/battleCallback");
        assertTrue(result.contains("Heroes"));
    }

    @Test
    void startCampaign_createsEngine() throws Exception {
        controller.startCampaign(playerParty, "http://battle", "http://pve/battleCallback");
        assertNotNull(getEngine(controller));
    }

    @Test
    void startCampaign_replacesExistingEngine_onSecondCall() throws Exception {
        controller.startCampaign(playerParty, "http://battle", "http://pve/battleCallback");
        PVECampaignEngine first = getEngine(controller);

        Party secondParty = party("Heroes2", warrior("Lancelot"));
        controller.startCampaign(secondParty, "http://battle", "http://pve/battleCallback");
        PVECampaignEngine second = getEngine(controller);

        assertNotSame(first, second);
    }

    // ── pveAction ─────────────────────────────────────────────────────────────

    @Test
    void pveAction_withNoEngine_returnsNotStartedMessage() {
        String result = controller.pveAction("next");
        assertTrue(result.contains("not started"));
    }

    @Test
    void pveAction_trimsAction_beforeForwarding() throws Exception {
        startAndInjectStub();
        // "quit" is a valid CAMPAIGN action — should not error
        String result = controller.pveAction("  quit  ");
        assertTrue(result.contains("quit"));
    }

    @Test
    void pveAction_returnsActionEcho() throws Exception {
        startAndInjectStub();
        String result = controller.pveAction("quit");
        assertTrue(result.contains("quit"));
    }

    @Test
    void pveAction_validCampaignAction_isHandled() throws Exception {
        startAndInjectStub();
        // "view party" is always valid in CAMPAIGN state
        String result = controller.pveAction("view party");
        assertNotNull(result);
        assertFalse(result.contains("not started"));
    }

    // ── battleCallback ────────────────────────────────────────────────────────

    @Test
    void battleCallback_withNoEngine_doesNotThrow() {
        Unit alive = warrior("Arthur");
        Party pa   = party("Heroes",   alive);
        Party pb   = party("Monsters", warrior("Goblin"));

        assertDoesNotThrow(() ->
                controller.battleCallback(Map.of("partyA", pa, "partyB", pb)));
    }

    @Test
    void battleCallback_playerWins_returnsToCAMPAIGN() throws Exception {
        startAndInjectStub();

        // partyA alive = player won
        Unit aliveUnit = warrior("Arthur");
        Party updatedPlayer  = party("Heroes",   aliveUnit);

        Unit deadEnemy = warrior("Goblin");
        deadEnemy.setHealth(0);
        Party updatedEnemy = party("Monsters", deadEnemy);

        controller.battleCallback(Map.of("partyA", updatedPlayer, "partyB", updatedEnemy));

        // Engine should now be back in CAMPAIGN — "quit" should work without error
        String result = controller.pveAction("quit");
        assertTrue(result.contains("quit"));
    }

    @Test
    void battleCallback_playerLoses_returnsToCAMPAIGN() throws Exception {
        startAndInjectStub();

        Unit deadUnit = warrior("Arthur");
        deadUnit.setHealth(0);
        Party defeatedPlayer = party("Heroes",   deadUnit);
        Party enemyParty     = party("Monsters", warrior("Goblin"));

        controller.battleCallback(Map.of("partyA", defeatedPlayer, "partyB", enemyParty));

        // Engine should be back in CAMPAIGN — "quit" should still work
        String result = controller.pveAction("quit");
        assertTrue(result.contains("quit"));
    }

    @Test
    void battleCallback_updatesPlayerParty_withResultUnits() throws Exception {
        startAndInjectStub();

        Unit newHero = warrior("NewHero");
        Party updatedPlayer = party("Heroes", newHero);

        Unit deadEnemy = warrior("Goblin");
        deadEnemy.setHealth(0);
        Party updatedEnemy = party("Monsters", deadEnemy);

        controller.battleCallback(Map.of("partyA", updatedPlayer, "partyB", updatedEnemy));

        // The engine's player party should now contain newHero
        PVECampaignEngine engine = getEngine(controller);
        Field partyField = PVECampaignEngine.class.getDeclaredField("playerParty");
        partyField.setAccessible(true);
        Party engineParty = (Party) partyField.get(engine);
        assertTrue(engineParty.getUnits().contains(newHero));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Starts a campaign and injects the StubRestTemplate into the engine. */
    private void startAndInjectStub() throws Exception {
        controller.startCampaign(playerParty, "http://battle", "http://pve/battleCallback");
        injectRestTemplate(getEngine(controller), restTemplate);
    }

    private static PVECampaignEngine getEngine(PVEController ctrl) throws Exception {
        Field f = PVEController.class.getDeclaredField("engine");
        f.setAccessible(true);
        return (PVECampaignEngine) f.get(ctrl);
    }

    private static void injectRestTemplate(PVECampaignEngine eng, StubRestTemplate stub) throws Exception {
        Field f = PVECampaignEngine.class.getDeclaredField("restTemplate");
        f.setAccessible(true);
        f.set(eng, stub);
    }
}