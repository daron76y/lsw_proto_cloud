package com.lsw_proto_cloud.core.persistence.test;

import com.lsw_proto_cloud.core.*;
import com.lsw_proto_cloud.core.abilities.BerserkerAttack;
import com.lsw_proto_cloud.core.abilities.Fireball;
import com.lsw_proto_cloud.core.persistence.MySQLUserProfileRepo;
import com.lsw_proto_cloud.core.persistence.UserProfile;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MySQLUserProfileRepoTest {

    private MySQLUserProfileRepo repo;
    private Connection connection;

    @BeforeAll
    void setupDatabase() throws Exception {
        // Read environment variables if set, otherwise use defaults
        String host = System.getenv().getOrDefault("MYSQL_HOST", "localhost");
        String port = System.getenv().getOrDefault("MYSQL_PORT", "3307"); //must match whatever port the sql container was run with
        String user = System.getenv().getOrDefault("MYSQL_USER", "root");
        String password = System.getenv().getOrDefault("MYSQL_PASSWORD", "rootpassword");
        String dbName = System.getenv().getOrDefault("MYSQL_DATABASE", "lsw_game");

        String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", host, port, dbName);
        connection = DriverManager.getConnection(url, user, password);
        repo = new MySQLUserProfileRepo(connection);

        System.out.printf("Connected to MySQL at %s:%s/%s as %s%n", host, port, dbName, user);
    }

    @AfterAll
    void teardown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @AfterEach
    void cleanup() {
        // remove any users created during tests to keep DB clean
        repo.getAllUsers().forEach(user -> repo.deleteUserByName(user.getUsername()));
    }

    @Test
    void testSaveAndRetrieveUserProfile() {
        UserProfile user = new UserProfile("testUser", "password123");
        user.increaseScore(100);

        // Create a party with two units
        Party party = new Party("AlphaTeam");
        Unit u1 = new Unit("Warrior", 10, 5, 100, 50, HeroClass.WARRIOR);
        Unit u2 = new Unit("Mage", 8, 8, 90, 60, HeroClass.MAGE);
        u1.addAbility(new BerserkerAttack(20));
        u2.addAbility(new Fireball(25, 1));
        party.addUnit(u1);
        party.addUnit(u2);

        user.saveParty(party);

        // Save a campaign
        user.saveCampaign("EpicCampaign", "AlphaTeam", 5);

        // Save to repository
        repo.saveUser(user);

        // Retrieve from repository
        Optional<UserProfile> retrieved = repo.getUserByName("testUser");
        assertTrue(retrieved.isPresent(), "User should be present in DB");

        UserProfile dbUser = retrieved.get();
        assertEquals("testUser", dbUser.getUsername());
        assertEquals("password123", dbUser.getPassword());
        assertEquals(100, dbUser.getScore());

        // Verify party
        Optional<Party> dbParty = dbUser.getPartyByName("AlphaTeam");
        assertTrue(dbParty.isPresent(), "Party should be present");
        assertEquals(2, dbParty.get().getUnits().size());

        // Verify campaign
        Optional<UserProfile.CampaignProgress> dbCampaign =
                dbUser.getCampaignByName("EpicCampaign");
        assertTrue(dbCampaign.isPresent(), "Campaign should be present");
        assertEquals(5, dbCampaign.get().getCurrentRoom());
    }

    @Test
    void testDeleteUserProfile() {
        UserProfile user = new UserProfile("deleteMe", "pw");
        repo.saveUser(user);

        assertTrue(repo.exists("deleteMe"), "User should exist before deletion");

        repo.deleteUserByName("deleteMe");

        assertFalse(repo.exists("deleteMe"), "User should no longer exist");
    }

    @Test
    void testUpdateCampaignRoom() {
        UserProfile user = new UserProfile("campaignUser", "pw");
        Party party = new Party("DeltaTeam");
        Unit unit = new Unit("Rogue", 12, 6, 95, 40, HeroClass.ROGUE);
        party.addUnit(unit);
        user.saveParty(party);
        user.saveCampaign("DungeonRun", "DeltaTeam", 3);

        repo.saveUser(user);

        Optional<UserProfile> retrieved = repo.getUserByName("campaignUser");
        assertTrue(retrieved.isPresent());

        retrieved.get().updateCampaignRoom("DungeonRun", 7);
        repo.saveUser(retrieved.get());

        Optional<UserProfile.CampaignProgress> updatedCampaign =
                repo.getUserByName("campaignUser")
                        .flatMap(u -> u.getCampaignByName("DungeonRun"));

        assertTrue(updatedCampaign.isPresent());
        assertEquals(7, updatedCampaign.get().getCurrentRoom());
    }

    @Test
    void testMultiplePartiesAndCampaigns() {
        UserProfile user = new UserProfile("multiUser", "pwMulti");

        // Create multiple parties
        for (int p = 1; p <= 3; p++) {
            Party party = new Party("Party" + p);
            // Each party gets multiple units
            for (int u = 1; u <= 3; u++) {
                Unit unit = new Unit(
                        "Unit" + u + "_P" + p,
                        5 + u,
                        3 + u,
                        50 + u * 10,
                        20 + u * 5,
                        HeroClass.ORDER
                );
                // Add multiple abilities to each unit
                unit.addAbility(new BerserkerAttack(20));
                unit.addAbility(new Fireball(25, 1));
                party.addUnit(unit);
            }
            user.saveParty(party);

            // Add multiple campaigns for this party
            for (int c = 1; c <= 2; c++) {
                user.saveCampaign("Campaign" + c + "_P" + p, party.getName(), c * 2);
            }
        }

        // Save the user
        repo.saveUser(user);

        // Retrieve and verify
        Optional<UserProfile> retrieved = repo.getUserByName("multiUser");
        assertTrue(retrieved.isPresent());

        UserProfile dbUser = retrieved.get();

        // Verify all parties
        assertEquals(3, dbUser.getSavedParties().size());
        for (int p = 1; p <= 3; p++) {
            Optional<Party> dbParty = dbUser.getPartyByName("Party" + p);
            assertTrue(dbParty.isPresent());
            assertEquals(3, dbParty.get().getUnits().size());

            for (int u = 1; u <= 3; u++) {
                Unit dbUnit = dbParty.get().getUnits().get(u - 1);
                assertEquals("Unit" + u + "_P" + p, dbUnit.getName());
                assertEquals(4, dbUnit.getAbilities().size());
            }

            // Verify campaigns for this party
            for (int c = 1; c <= 2; c++) {
                Optional<UserProfile.CampaignProgress> dbCampaign =
                        dbUser.getCampaignByName("Campaign" + c + "_P" + p);
                assertTrue(dbCampaign.isPresent());
                assertEquals(c * 2, dbCampaign.get().getCurrentRoom());
            }
        }
    }
}