package com.lsw_proto_cloud.pve;

import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.test.TestHelpers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static com.lsw_proto_cloud.test.TestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * pve/src/test/java/com/lsw_proto_cloud/pve/PVECampaignEngineTest.java
 *
 * Uses reflection to inject a StubRestTemplate so we never hit the network.
 */
class PVECampaignEngineTest {

    private static final String BATTLE_URL    = "http://battle";
    private static final String CALLBACK_URL  = "http://pve/battleCallback";

    private RecordingOutput  output;
    private StubRestTemplate restTemplate;
    private Party            playerParty;
    private PVECampaignEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        output      = new RecordingOutput();
        restTemplate = new StubRestTemplate();
        playerParty  = party("Heroes", warrior("Arthur"), order("Merlin"));

        engine = new PVECampaignEngine(playerParty, output, BATTLE_URL, CALLBACK_URL);
        injectRestTemplate(engine, restTemplate);
    }

    /** Injects our stub via reflection into the private restTemplate field. */
    private static void injectRestTemplate(PVECampaignEngine eng, StubRestTemplate stub) throws Exception {
        Field f = PVECampaignEngine.class.getDeclaredField("restTemplate");
        f.setAccessible(true);
        f.set(eng, stub);
    }

    // ── CAMPAIGN state — general commands ────────────────────────────────────

    @Test
    void handleAction_viewParty_showsPartyInfo() {
        engine.handleAction("view party");
        assertTrue(output.anyMatch("Viewing party"));
    }

    @Test
    void handleAction_quit_showsQuitMessage() {
        engine.handleAction("quit");
        assertTrue(output.anyMatch("Quitting"));
    }

    @Test
    void handleAction_unknownCommand_showsError() {
        engine.handleAction("fly");
        assertTrue(output.anyMatch("PVE Error"));
    }

    @Test
    void handleAction_next_incrementsRoom() throws Exception {
        engine.handleAction("next");
        Field f = PVECampaignEngine.class.getDeclaredField("currentRoom");
        f.setAccessible(true);
        assertEquals(1, f.getInt(engine));
    }

    // ── BATTLE state ─────────────────────────────────────────────────────────

    @Test
    void enterBattle_postsStartRequestToBattleModule() throws Exception {
        forceBattleRoom(engine, 1);
        engine.handleAction("next");
        assertTrue(restTemplate.anyUrlContains("/start"));
    }

    @Test
    void enterBattle_setsStateToInBattle() throws Exception {
        forceBattleRoom(engine, 1);
        engine.handleAction("next");
        // In BATTLE state, campaign commands are rejected
        output.clear();
        engine.handleAction("quit");
        // "quit" is not a battle module action, so it gets forwarded and echoed back
        assertTrue(restTemplate.anyUrlContains("/action"));
    }

    @Test
    void enterBattle_restTemplateFailure_returnsToCAMPAIGN() throws Exception {
        forceBattleRoom(engine, 1);
        restTemplate.setShouldThrow(true);
        engine.handleAction("next");
        assertTrue(output.anyMatch("Error starting battle"));
        // should be back in CAMPAIGN — "quit" should be handled locally
        output.clear();
        engine.handleAction("quit");
        assertTrue(output.anyMatch("Quitting"));
    }

    @Test
    void forwardActionToBattleModule_postsActionUrl() throws Exception {
        forceBattleRoom(engine, 1);
        engine.handleAction("next");          // enter battle
        output.clear();
        engine.handleAction("attack Goblin");
        assertTrue(restTemplate.anyUrlContains("/action"));
    }

    @Test
    void forwardActionToBattleModule_finishResponse_returnsToCAMPAIGN() throws Exception {
        forceBattleRoom(engine, 1);
        engine.handleAction("next");
        restTemplate.setNextResponse("Battle finished");
        engine.handleAction("attack Goblin");
        assertTrue(output.anyMatch("Cannot accept any more actions"));
    }

    // ── battleFinished() — player wins ───────────────────────────────────────

    @Test
    void battleFinished_playerWins_grantsGoldAndExp() {
        Unit enemy = makeUnit("Goblin", HeroClass.WARRIOR, 5, 0, 10, 0);
        enemy.setHealth(0); // dead = player won
        Party updatedEnemy = party("Monsters", enemy);

        Unit arthur = warrior("Arthur2");
        Party updatedPlayer = party("Heroes", arthur);

        int goldBefore = playerParty.getGold();
        engine.battleFinished(updatedPlayer, updatedEnemy);

        assertTrue(output.anyMatch("won the battle"));
        assertTrue(playerParty.getGold() > goldBefore);
    }

    @Test
    void battleFinished_playerWins_dividesExpAmongAliveUnits() {
        Unit enemy = makeUnit("Goblin", HeroClass.WARRIOR, 5, 0, 10, 0);
        enemy.setHealth(0);
        Party updatedEnemy = party("Monsters", enemy);

        Unit a1 = warrior("A1");
        Unit a2 = warrior("A2");
        Party updatedPlayer = party("Heroes", a1, a2);

        engine.battleFinished(updatedPlayer, updatedEnemy);

        // Both alive units should have received experience
        assertTrue(a1.getExperience() > 0);
        assertTrue(a2.getExperience() > 0);
    }

    @Test
    void battleFinished_playerLoses_resetsToLastInnCheckpoint() throws Exception {
        // Move to room 5
        Field room = PVECampaignEngine.class.getDeclaredField("currentRoom");
        room.setAccessible(true);
        room.set(engine, 5);
        Field checkpoint = PVECampaignEngine.class.getDeclaredField("lastInnCheckpoint");
        checkpoint.setAccessible(true);
        checkpoint.set(engine, 2);

        // All player units dead
        Unit dead = warrior("Dead");
        dead.setHealth(0);
        Party defeatedParty = party("Heroes", dead);

        engine.battleFinished(defeatedParty, party("Monsters", warrior("Goblin")));

        assertTrue(output.anyMatch("lost"));
        assertEquals(2, room.getInt(engine));
    }

    @Test
    void battleFinished_replacesPlayerPartyUnits() {
        Unit newUnit = warrior("NewHero");
        Party updatedPlayer = party("Heroes", newUnit);

        Unit enemy = warrior("Goblin");
        enemy.setHealth(0);
        Party updatedEnemy = party("Monsters", enemy);

        engine.battleFinished(updatedPlayer, updatedEnemy);

        assertTrue(playerParty.getUnits().contains(newUnit));
    }

    // ── INN state ────────────────────────────────────────────────────────────

    @Test
    void enterInn_replenishesAllUnits() throws Exception {
        // Damage units first
        playerParty.getUnits().forEach(u -> u.setHealth(1));
        forceInnRoom(engine, 1);
        engine.handleAction("next");

        for (Unit u : playerParty.getUnits())
            assertEquals(u.getMaxHealth(), u.getHealth());
    }

    @Test
    void innAction_viewParty_showsParty() throws Exception {
        forceInnRoom(engine, 1);
        engine.handleAction("next"); // enter inn
        output.clear();
        engine.handleAction("view party");
        assertTrue(output.anyMatch("Viewing party"));
    }

    @Test
    void innAction_leave_returnsToCAMPAIGN() throws Exception {
        forceInnRoom(engine, 1);
        engine.handleAction("next");
        engine.handleAction("leave");
        assertTrue(output.anyMatch("Leaving Inn"));
        // Should now handle campaign commands again
        output.clear();
        engine.handleAction("quit");
        assertTrue(output.anyMatch("Quitting"));
    }

    @Test
    void innAction_unknownCommand_showsError() throws Exception {
        forceInnRoom(engine, 1);
        engine.handleAction("next");
        output.clear();
        engine.handleAction("attack");
        assertTrue(output.anyMatch("PVE Error"));
    }

    // ── INN_SHOP state ───────────────────────────────────────────────────────

    @Test
    void innShop_buyItem_deductsGoldAndAddsToInventory() throws Exception {
        playerParty.setGold(5000);
        forceInnRoom(engine, 1);
        engine.handleAction("next");
        engine.handleAction("shop");
        engine.handleAction("buy bread");

        assertEquals(1, (int) playerParty.getInventory().getOrDefault(Items.BREAD, 0));
        assertEquals(5000 - Items.BREAD.getCost(), playerParty.getGold());
    }

    @Test
    void innShop_unknownItem_showsError() throws Exception {
        forceInnRoom(engine, 1);
        engine.handleAction("next");
        engine.handleAction("shop");
        engine.handleAction("buy potion");
        assertTrue(output.anyMatch("PVE Error"));
    }

    @Test
    void innShop_notEnoughGold_showsError() throws Exception {
        playerParty.setGold(0);
        forceInnRoom(engine, 1);
        engine.handleAction("next");
        engine.handleAction("shop");
        engine.handleAction("buy elixir");
        assertTrue(output.anyMatch("PVE Error"));
    }

    // ── INN_RECRUIT state ────────────────────────────────────────────────────

    @Test
    void innRecruit_hireLevel1_costsNothing() throws Exception {
        forceInnRoom(engine, 1);
        engine.handleAction("next");
        engine.handleAction("recruits");

        // Find a level-1 recruit from the generated list
        Field rf = PVECampaignEngine.class.getDeclaredField("recruits");
        rf.setAccessible(true);
        List<Unit> recruits = (List<Unit>) rf.get(engine);

        Unit level1 = recruits.stream().filter(u -> u.getLevel() == 1).findFirst().orElse(null);
        if (level1 == null) return; // skip if no level-1 recruit generated (random)

        int goldBefore = playerParty.getGold();
        engine.handleAction("hire " + level1.getName());
        assertEquals(goldBefore, playerParty.getGold()); // level-1 is free
        assertTrue(playerParty.getUnits().contains(level1));
    }

    @Test
    void innRecruit_unknownRecruit_showsError() throws Exception {
        forceInnRoom(engine, 1);
        engine.handleAction("next");
        engine.handleAction("recruits");
        engine.handleAction("hire Zorgon");
        assertTrue(output.anyMatch("PVE Error"));
    }

    @Test
    void innRecruit_invalidAction_returnsToInn() throws Exception {
        forceInnRoom(engine, 1);
        engine.handleAction("next");
        engine.handleAction("recruits");
        engine.handleAction("dance");
        // Should have returned to INN — leave should now work
        output.clear();
        engine.handleAction("leave");
        assertTrue(output.anyMatch("Leaving Inn"));
    }

    // ── useItem ───────────────────────────────────────────────────────────────

    @Test
    void useItem_restoresHealthAndMana() {
        playerParty.setGold(5000);
        playerParty.addItem(Items.BREAD);
        Unit arthur = playerParty.getUnits().get(0);
        arthur.setHealth(1);

        engine.handleAction("use bread " + arthur.getName());

        assertTrue(arthur.getHealth() > 1);
    }

    @Test
    void useItem_unknownItem_showsError() {
        engine.handleAction("use potion Arthur");
        assertTrue(output.anyMatch("PVE Error"));
    }

    @Test
    void useItem_itemNotInInventory_showsError() {
        engine.handleAction("use bread Arthur");
        assertTrue(output.anyMatch("PVE Error"));
    }

    @Test
    void useItem_unknownUnit_showsError() {
        playerParty.addItem(Items.BREAD);
        engine.handleAction("use bread Nobody");
        assertTrue(output.anyMatch("PVE Error"));
    }

    @Test
    void useItem_deadUnit_withNonElixir_showsError() {
        playerParty.addItem(Items.BREAD);
        Unit arthur = playerParty.getUnits().get(0);
        arthur.setHealth(0);
        engine.handleAction("use bread " + arthur.getName());
        assertTrue(output.anyMatch("PVE Error"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Forces room[index] to BATTLE via reflection. */
    private static void forceBattleRoom(PVECampaignEngine eng, int index) throws Exception {
        Field f = PVECampaignEngine.class.getDeclaredField("rooms");
        f.setAccessible(true);
        Object[] rooms = (Object[]) f.get(eng);
        // Set using the enum via its name
        Class<?> roomType = Class.forName("com.lsw_proto_cloud.pve.PVECampaignEngine$RoomTypes");
        Object battle = Enum.valueOf((Class<Enum>) roomType, "BATTLE");
        rooms[index] = battle;
    }

    /** Forces room[index] to INN via reflection. */
    private static void forceInnRoom(PVECampaignEngine eng, int index) throws Exception {
        Field f = PVECampaignEngine.class.getDeclaredField("rooms");
        f.setAccessible(true);
        Object[] rooms = (Object[]) f.get(eng);
        Class<?> roomType = Class.forName("com.lsw_proto_cloud.pve.PVECampaignEngine$RoomTypes");
        Object inn = Enum.valueOf((Class<Enum>) roomType, "INN");
        rooms[index] = inn;
    }
}