package com.lsw_proto_cloud.pvp;

import com.lsw_proto_cloud.battle.api.RestTemplateProvider;
import com.lsw_proto_cloud.core.*;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class PVPMatchEngine implements PVPMatch {
    private final Party playerOne;
    private final Party playerTwo;
    private final OutputService output;
    private final RestTemplate restTemplate = RestTemplateProvider.get();

    private final String battleModuleURL;
    private final String battleCallbackURL;

    private boolean currentlyInBattle = false;

    public PVPMatchEngine(
        Party playerOne,
        Party playerTwo,
        String battleModuleURL,
        String battleCallbackURL,
        OutputService output
    ) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.battleModuleURL = battleModuleURL;
        this.battleCallbackURL = battleCallbackURL;
        this.output = output;
    }

    /** Handles all string-based battle actions */
    public void handleAction(String action) {
        if (!currentlyInBattle) {
            output.showMessage("No battle in progress.");
            return;
        }

        String url = battleModuleURL + "/action";

        try {
            String response = restTemplate.postForObject(url, action, String.class);
            output.showMessage("Battle module response: " + response);

            if (response != null && response.toLowerCase().contains("finished")) {
                currentlyInBattle = false;
                output.showMessage("PvP match finished!");
            }
        } catch (Exception e) {
            output.showMessage("Error sending battle action: " + e.getMessage());
        }
    }

    /** Start the PvP match by sending a request to the Battle module */
    public void startMatch() {
        currentlyInBattle = true;
        output.showMessage("Starting PvP match...");

        Map<String, Object> battleStartRequest = new HashMap<>();
        battleStartRequest.put("partyA", playerOne);
        battleStartRequest.put("partyB", playerTwo);
        battleStartRequest.put("inputA", "HTTP");
        battleStartRequest.put("inputB", "HTTP");
        battleStartRequest.put("callbackURL", battleCallbackURL);

        try {
            restTemplate.postForObject(
                    battleModuleURL + "/start",
                    battleStartRequest,
                    String.class
            );
            output.showMessage("Battle started! Please view battle module output");
        } catch (Exception e) {
            output.showMessage("Failed to start PvP match: " + e.getMessage());
            currentlyInBattle = false;
        }
    }

    public boolean isInBattle() {
        return currentlyInBattle;
    }
}