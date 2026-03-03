package com.lsw_proto_cloud.core.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsw_proto_cloud.core.Party;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySQLUserProfileRepo implements UserProfileRepository {

    private final Connection connection;
    private final ObjectMapper mapper = new ObjectMapper();

    public MySQLUserProfileRepo(Connection connection) {
        this.connection = connection;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS user_profiles (
                username VARCHAR(50) PRIMARY KEY,
                password VARCHAR(255) NOT NULL,
                score INT NOT NULL,
                savedParties JSON,
                campaignSaves JSON
            )
        """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create table", e);
        }
    }

    @Override
    public void saveUser(UserProfile user) {
        String sql = """
            INSERT INTO user_profiles
            (username, password, score, savedParties, campaignSaves)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                password = VALUES(password),
                score = VALUES(score),
                savedParties = VALUES(savedParties),
                campaignSaves = VALUES(campaignSaves)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setInt(3, user.getScore());
            ps.setString(4, mapper.writeValueAsString(user.getSavedParties()));
            ps.setString(5, mapper.writeValueAsString(user.getCampaignSaves()));

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    @Override
    public Optional<UserProfile> getUserByName(String username) {
        String sql = "SELECT * FROM user_profiles WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next())
                    return Optional.empty();

                UserProfile user = new UserProfile(
                        rs.getString("username"),
                        rs.getString("password")
                );

                user.setScore(rs.getInt("score"));

                // Deserialize parties
                String partiesJson = rs.getString("savedParties");
                if (partiesJson != null) {
                    List<Party> parties = mapper.readValue(
                            partiesJson,
                            mapper.getTypeFactory()
                                    .constructCollectionType(List.class, Party.class)
                    );
                    user.setSavedParties(parties);
                }

                // Deserialize campaigns
                String campaignsJson = rs.getString("campaignSaves");
                if (campaignsJson != null) {
                    List<UserProfile.CampaignProgress> campaigns =
                            mapper.readValue(
                                    campaignsJson,
                                    mapper.getTypeFactory()
                                            .constructCollectionType(
                                                    List.class,
                                                    UserProfile.CampaignProgress.class
                                            )
                            );
                    user.setCampaignSaves(campaigns);
                }

                return Optional.of(user);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load user", e);
        }
    }

    @Override
    public List<UserProfile> getAllUsers() {

        List<UserProfile> users = new ArrayList<>();
        String sql = "SELECT * FROM user_profiles";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {

                UserProfile user = new UserProfile(
                        rs.getString("username"),
                        rs.getString("password")
                );

                user.setScore(rs.getInt("score"));

                String partiesJson = rs.getString("savedParties");
                if (partiesJson != null) {
                    List<Party> parties = mapper.readValue(
                            partiesJson,
                            mapper.getTypeFactory()
                                    .constructCollectionType(List.class, Party.class)
                    );
                    user.setSavedParties(parties);
                }

                String campaignsJson = rs.getString("campaignSaves");
                if (campaignsJson != null) {
                    List<UserProfile.CampaignProgress> campaigns =
                            mapper.readValue(
                                    campaignsJson,
                                    mapper.getTypeFactory()
                                            .constructCollectionType(
                                                    List.class,
                                                    UserProfile.CampaignProgress.class
                                            )
                            );
                    user.setCampaignSaves(campaigns);
                }

                users.add(user);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users", e);
        }

        return users;
    }

    @Override
    public void deleteUserByName(String username) {
        String sql = "DELETE FROM user_profiles WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    @Override
    public boolean exists(String username) {
        String sql = "SELECT 1 FROM user_profiles WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check user existence", e);
        }
    }
}