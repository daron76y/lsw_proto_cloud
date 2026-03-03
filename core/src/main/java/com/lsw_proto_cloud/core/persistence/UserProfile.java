package com.lsw_proto_cloud.core.persistence;

import com.lsw_proto_cloud.core.Party;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserProfile {

    public static class CampaignProgress {
        private String campaignName;
        private String partyName;
        private int currentRoom;

        public CampaignProgress() {}

        public CampaignProgress(String campaignName, String partyName, int currentRoom) {
            this.campaignName = campaignName;
            this.partyName = partyName;
            this.currentRoom = currentRoom;
        }

        public String getCampaignName() { return campaignName; }
        public String getPartyName() { return partyName; }
        public int getCurrentRoom() { return currentRoom; }
        public void setCurrentRoom(int currentRoom) { this.currentRoom = currentRoom; }
    }

    private String username;
    private String password;
    private int score;
    private List<Party> savedParties;
    private List<CampaignProgress> campaignSaves;

    // Required for deserialization
    public UserProfile() {
        this.savedParties = new ArrayList<>();
        this.campaignSaves = new ArrayList<>();
    }

    public UserProfile(String username, String password) {
        this();
        this.username = username;
        this.password = password;
        this.score = 0;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    // score ==============================
    public int getScore() { return score; }

    public void setScore(int score) {
        this.score = score;
    }

    public void increaseScore(int amount) {
        this.score += amount;
    }

    // parties ==============================
    public void saveParty(Party party) {
        if (getPartyByName(party.getName()).isPresent())
            throw new IllegalStateException("Party already saved: " + party.getName());

        savedParties.add(party);
    }

    public void deleteParty(String partyName) {
        savedParties.removeIf(p -> p.getName().equals(partyName));
        campaignSaves.removeIf(c -> c.getPartyName().equals(partyName));
    }

    public List<Party> getSavedParties() {
        return List.copyOf(savedParties);
    }

    public Optional<Party> getPartyByName(String partyName) {
        return savedParties.stream()
                .filter(p -> p.getName().equals(partyName))
                .findFirst();
    }

    public void setSavedParties(List<Party> parties) {
        this.savedParties = new ArrayList<>(parties);
    }

    // campaigns ==============================
    public void saveCampaign(String campaignName, String partyName, int currentRoom) {
        if (getPartyByName(partyName).isEmpty())
            throw new IllegalArgumentException("Party does not exist: " + partyName);

        deleteCampaignByName(campaignName);
        campaignSaves.add(new CampaignProgress(campaignName, partyName, currentRoom));
    }

    public void deleteCampaignByName(String name) {
        campaignSaves.removeIf(c -> c.getCampaignName().equals(name));
    }

    public List<CampaignProgress> getCampaignSaves() {
        return List.copyOf(campaignSaves);
    }

    public Optional<CampaignProgress> getCampaignByName(String campaignName) {
        return campaignSaves.stream()
                .filter(c -> c.getCampaignName().equals(campaignName))
                .findFirst();
    }

    public void setCampaignSaves(List<CampaignProgress> saves) {
        this.campaignSaves = new ArrayList<>(saves);
    }

    public void updateCampaignRoom(String campaignName, int newRoom) {
        getCampaignByName(campaignName)
                .ifPresent(c -> c.setCurrentRoom(newRoom));
    }
}