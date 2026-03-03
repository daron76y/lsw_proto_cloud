package com.lsw_proto_cloud.pve;

import com.lsw_proto_cloud.battle.api.RestTemplateProvider;
import com.lsw_proto_cloud.core.*;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class PVECampaignEngine {
    private final Party playerParty;
    private final OutputService output;
    private final UnitFactory unitFactory = new UnitFactoryCSV();
    private final RestTemplate restTemplate = RestTemplateProvider.get();
    private final int totalRooms = 30;
    private int currentRoom = 0;
    private int lastInnCheckpoint = 0;

    private List<Unit> recruits = new ArrayList<>();

    private enum RoomTypes { BATTLE, INN }
    private final RoomTypes[] rooms = new RoomTypes[totalRooms + 1];

    private enum CampaignState {CAMPAIGN, BATTLE, INN, INN_RECRUIT, INN_SHOP}
    private CampaignState currentState = CampaignState.CAMPAIGN;

    private final String battleModuleURL; //URL for the battle module
    private final String battleCallbackURL; // URL for battleController to POST results back here

    public PVECampaignEngine(Party playerParty, OutputService output,
                             String battleModuleURL, String battleCallbackURL) {
        this.playerParty = playerParty;
        this.output = output;
        this.battleModuleURL = battleModuleURL;
        this.battleCallbackURL = battleCallbackURL;
    }

    public void handleAction(String action) {
        action = action.toLowerCase().trim();

        try {
            if (currentState == CampaignState.BATTLE) { //battle commands, redirect to battle module
                forwardActionToBattleModule(action);
            }
            else if (currentState == CampaignState.INN) { //inn commands
                output.showMessage("[shop] [recruits] [view party] [leave]");
                switch (action.toLowerCase()) {
                    case "shop":
                        currentState = CampaignState.INN_SHOP;
                        output.showItemShop();
                        output.showMessage("Your gold: " + playerParty.getGold());
                        output.showMessage("[buy <item>]");
                        break;
                    case "recruits":
                        currentState = CampaignState.INN_RECRUIT;
                        output.showMessage("Available Recruits:");
                        for (int i=0; i<recruits.size(); i++) {
                            Unit recruit = recruits.get(i);
                            int cost = recruit.getLevel() == 1 ? 0 : recruit.getLevel() * 200;
                            output.showMessage((i+1) + ". " + recruit.getName() + " - lvl: " + recruit.getLevel() + " - cost: " + cost);
                        }
                        output.showMessage("Your gold: " + playerParty.getGold());
                        output.showMessage("[hire <heroName>]");
                        break;
                    case "view party": viewParty(); break;
                    case "leave":
                        output.showMessage("Leaving Inn...");
                        currentState = CampaignState.CAMPAIGN;
                        break;
                    default: throw new IllegalArgumentException();
                }
            }
            else if (currentState == CampaignState.INN_SHOP) {
                handleInnShopAction(action);
            }
            else if (currentState == CampaignState.INN_RECRUIT) {
                handleInnRecruitAction(action);
            }
            else if (currentState == CampaignState.CAMPAIGN){ //general out-of-room commands
                output.showMessage("[next] [view party] [use item] [quit]");
                action = action.toLowerCase().trim();
                if (action.equals("next")) nextRoom();
                else if (action.equals("view party")) viewParty();
                else if (action.startsWith("use ")) useItem(action);
                else if (action.equals("quit")) quit();
                else throw new IllegalArgumentException("Unknown command: " + action);
            }
            else throw new IllegalStateException("Illegal campaign state");
        } catch(Exception e) {
            output.showMessage("PVE Error: " + e.getMessage());
        }
    }

    private void nextRoom() {
        output.showMessage("Entering the next room...");
        currentRoom++;

        //if the current room is new and undiscovered so far, discover its type (BATTLE or INN)
        if (rooms[currentRoom] == null) {
            //determine the type of the room based on player levels (higher => more battles)
            int cumulativeLevel = playerParty.getCumulativeLevels();
            int probabilityShift = cumulativeLevel / 10 * 3;
            if (Math.random() * 100 <= 60 + probabilityShift)
                rooms[currentRoom] = RoomTypes.BATTLE;
            else
                rooms[currentRoom] = RoomTypes.INN;
        }

        //if the room has been discovered, enter it
        if (rooms[currentRoom] == RoomTypes.BATTLE) enterBattleRoom();
        else enterInn();
    }

    private void enterBattleRoom() {
        if (currentState != CampaignState.CAMPAIGN) throw new IllegalStateException("Cannot enter battle right now");
        output.showMessage("Entering a battle...");
        currentState = CampaignState.BATTLE;

        //generate a random enemy party from the factory, based on the players cumulative levels for difficulty scaling
        Party enemyParty = unitFactory.generateEnemyParty(playerParty.getCumulativeLevels());

        //create a request object to send to the battle module over http
        Map<String, Object> battleStartRequest = new HashMap<>();
        battleStartRequest.put("partyA", playerParty);
        battleStartRequest.put("partyB", enemyParty);
        battleStartRequest.put("inputA", "HTTP");
        battleStartRequest.put("inputB", "HTTP"); //TODO: replace with "AI"
        battleStartRequest.put("callbackURL", battleCallbackURL); //source url (this module), that battle can send results to

        //POST the battle start request to the battle module
        try {
            restTemplate.postForObject(
                battleModuleURL + "/start",
                battleStartRequest,
                String.class
            );
            output.showMessage("Battle started successfully! Please view battle module.");
            /*
                AFTER THIS, THE BATTLE MODULE WILL USE THE battleCallbackURL TO SEND A POST REQUEST
                TO http://localhost:8081/pve/battleCallback, WHICH CALLS battleFinished() BELOW!!!
            */
        } catch (Exception e) {
            output.showMessage("Error starting battle: " + e.getMessage());
            currentState = CampaignState.CAMPAIGN;
        }
    }

    public void battleFinished(Party updatedPlayerParty, Party updatedEnemyParty) {
        output.showMessage("Battle finished! Returning to campaign loop...");
        currentState = CampaignState.CAMPAIGN;

        //clear the player party, and replace it with the updated units returned from the battle module
        this.playerParty.getUnits().clear();
        this.playerParty.getUnits().addAll(updatedPlayerParty.getUnits());

        if (!playerParty.isDefeated()) { //player won the battle
            output.showMessage(playerParty.getName() + " won the battle!");

            // calculate battle rewards (gold and exp) ================================
            int totalExperienceEarned = 0;
            int totalGoldEarned = 0;

            // get exp and gold per enemy unit defeated
            for (Unit enemy : updatedEnemyParty.getUnits()) {
                totalExperienceEarned += 50 * enemy.getLevel();
                totalGoldEarned += 75 * enemy.getLevel();
            }

            // divide exp amongst all alive members
            int numStandingUnits = playerParty.getAliveUnits().size();
            for (Unit standingUnit : playerParty.getAliveUnits())
                standingUnit.gainExperience(totalExperienceEarned / numStandingUnits);

            // gain gold for the party
            playerParty.setGold(playerParty.getGold() + totalGoldEarned);

            //output reward messages
            output.showMessage("+ " + totalGoldEarned + " Gold Earned!");
            output.showMessage("+ " +  totalExperienceEarned + " Total Experience Earned!");
        }
        else { //player lost the battle
            output.showMessage(playerParty.getName() + " lost!");
            output.showMessage("Returning to previous inn!");
            currentRoom = lastInnCheckpoint;
        }
    }

    private void forwardActionToBattleModule(String action) {
        if (currentState != CampaignState.BATTLE) throw new IllegalStateException("cannot accept battle actions right now");

        String url = battleModuleURL + "/action";
        try {
            //redirect the body of this POST request to the battle module, because it's a battle action
            String response = restTemplate.postForObject(url, action, String.class);
            output.showMessage("Battle module response: " + response);

            //TODO: might be a redundant check
            //disallow sending action requests if a battle has finished already (just in case the currentlyInBattle check doesn't work)
            if (response != null && response.toLowerCase().contains("finish")) {
                currentState = CampaignState.CAMPAIGN;
                output.showMessage("Battle has finished. Cannot accept any more actions");
            }
        } catch (Exception e) {
            output.showMessage("Error sending battle action: " + e.getMessage());
        }
    }

    private void enterInn() {
        if (currentState != CampaignState.CAMPAIGN) throw new IllegalStateException("Cannot enter Inn right now");
        output.showMessage("You arrived at an inn! Replenishing all heroes...");
        output.showMessage("[shop] [recruits] [view party] [leave]");
        currentState = CampaignState.INN;
        lastInnCheckpoint = currentRoom;

        //replenish all heroes
        for (Unit u : playerParty.getUnits()) {
            u.setHealth(u.getMaxHealth());
            u.setMana(u.getMaxMana());
        }

        //create new recruits
        int numRecruits = new Random().nextInt(5) + 1;
        recruits = unitFactory.generateHeroRecruits(numRecruits);
    }

    private void handleInnRecruitAction(String action) {
        if (currentState != CampaignState.INN_RECRUIT) throw new  IllegalStateException("Cannot recruit heroes now");
        if (action.startsWith("hire ")) {
            //get the selected recruit
            String name = action.split(" ")[1];
            Unit selectedUnit = recruits.stream()
                    .filter(u -> u.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
            if (selectedUnit == null) throw new IllegalArgumentException("Recruit " + name + " does not exist");

            //get cost and check if player can hire
            int cost = (selectedUnit.getLevel() == 1) ? 0 : selectedUnit.getLevel() * 200;
            if (playerParty.getGold() < cost) throw new IllegalArgumentException("Not enough gold to hire " + name);

            //hire recruit
            playerParty.setGold(playerParty.getGold() - cost);
            playerParty.addUnit(selectedUnit);
            output.showMessage("Hired " + name + " for " + cost + "g!");

            //return to inn menu
            currentState = CampaignState.INN;
        }
        else {
            //invalid action. Cancel and return to main Inn menu.
            currentState = CampaignState.INN;
            throw new IllegalArgumentException();
        }
    }

    private void handleInnShopAction(String action) {
        if (currentState != CampaignState.INN_SHOP) throw new IllegalStateException("Cannot buy items now");
        if (action.startsWith("buy ")) {
            //get the item
            String itemName = action.split(" ")[1];
            Items item = null;
            for (Items i : Items.values())
                if (itemName.equalsIgnoreCase(i.toString()))
                    item = i;
            if (item == null) throw new IllegalArgumentException("Item " + itemName + " does not exist");

            //get cost and check
            int cost = item.getCost();
            if (playerParty.getGold() < cost) throw new IllegalArgumentException("Not enough gold to buy " + itemName);

            //buy item
            playerParty.setGold(playerParty.getGold() - cost);
            playerParty.addItem(item);
            output.showMessage("Bought 1 " + itemName + " for " + cost + "g!");

            //return to inn menu
            currentState = CampaignState.INN;
        }
        else {
            //invalid action. Cancel and return to main Inn menu.
            currentState = CampaignState.INN;
            throw new IllegalArgumentException();
        }
    }

    private void viewParty() {
        output.showMessage("Viewing party...");
        output.showParty(List.of(playerParty));
        output.showInventory(playerParty);
        output.showMessage("Your gold: " + playerParty.getGold());
    }

    private void useItem(String action) {
        output.showMessage("Item usage in REST mode is not implemented.");
        String[] tokens = action.split(" ");
        if (tokens.length != 3) throw new IllegalArgumentException("Must specify item and unit");

        //get the item
        Items item = null;
        for (Items i : Items.values())
            if (tokens[1].equalsIgnoreCase(i.toString())) item = i;
        if (item == null) throw new IllegalArgumentException("Item " + tokens[1] + " does not exist");

        //check if player has the item in their party
        if (!playerParty.getInventory().containsKey(item) || playerParty.getInventory().get(item) <= 0)
            throw new IllegalArgumentException("You do not have this item.");

        //get the specified unit
        Unit unit = playerParty.getUnitByName(tokens[2]);
        if (unit == null) throw new IllegalArgumentException("Unit " + tokens[2] + " does not exist.");
        if (unit.isDead() && item != Items.ELIXIR)
            throw new IllegalArgumentException("Must revive this unit with an ELIXIR first!");

        //use the item on the unit
        playerParty.removeItem(item);
        unit.setHealth(unit.getHealth() + item.getHealthBoost());
        unit.setMana(unit.getMana() + item.getManaBoost());
        output.showMessage(String.format("%s consumed %s!", unit.getName(), item));
    }

    private void quit() {
        output.showMessage("Quitting PvE campaign.");
    }

    public static class BattleActionRequest {
        private String action;

        public BattleActionRequest() {}
        public BattleActionRequest(String action) { this.action = action; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }
}