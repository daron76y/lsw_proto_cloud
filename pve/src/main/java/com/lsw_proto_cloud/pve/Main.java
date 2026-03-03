package com.lsw_proto_cloud.pve;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;
import com.lsw_proto_cloud.core.HeroClass;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws JsonProcessingException {
        // 1. Create a simple player party
        Party playerParty = new Party("Heroes");
        playerParty.addUnit(new Unit("Warrior", 50, 8, 5, 10, HeroClass.WARRIOR));

        // 2. Create a simple enemy party
        Party enemyParty = new Party("Monsters");
        enemyParty.addUnit(new Unit("Goblin", 30, 5, 2, 0, HeroClass.WARRIOR));

        // 3. Set input types for Battle module
        String inputA = "HTTP"; // player input
        String inputB = "HTTP";   // enemy input

        // 4. Set callback URL (can be a dummy URL for now)
        String callbackURL = "http://localhost:8081/pve/callback";

        // 5. Build the battle start request payload
        Map<String, Object> battleStartRequest = new HashMap<>();
        battleStartRequest.put("partyA", playerParty);
        battleStartRequest.put("partyB", enemyParty);
        battleStartRequest.put("inputA", inputA);
        battleStartRequest.put("inputB", inputB);
        //battleStartRequest.put("callbackURL", callbackURL);

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(battleStartRequest);
        System.out.println("=== BattleStartRequest JSON ===");
        System.out.println(jsonString);
        System.out.println("================================");

        // 6. Send request to Battle module
        String battleModuleURL = "http://localhost:8080/battle"; // change if needed
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://localhost:8080/battle/start",
                    battleStartRequest,
                    String.class
            );

            System.out.println("Battle module response: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Failed to start battle: " + e.getMessage());
            e.printStackTrace();
        }
    }
}