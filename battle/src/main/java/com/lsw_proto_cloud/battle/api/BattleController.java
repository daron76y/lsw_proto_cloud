package com.lsw_proto_cloud.battle.api;

import com.lsw_proto_cloud.battle.*;
import com.lsw_proto_cloud.core.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/battle")
public class BattleController {

    private BattleEngine currentBattle;
    private InputService playerAInput;
    private InputService playerBInput;

    @PostMapping("/start")
    public Party[] startBattle(@RequestBody BattleStartRequest request) {
        //reconstruct player parties from the JSON in the request body
        Party partyA = request.getPartyA();
        Party partyB = request.getPartyB();

        //create input services for the parties. Use the same one if multiplayer for hot-seat gaming
        if (request.getInputA().equalsIgnoreCase(request.getInputB())) {
            playerAInput = playerBInput = getInputService(request.getInputA());
        }
        else {
            playerAInput = getInputService(request.getInputA());
            playerBInput = getInputService(request.getInputB());
        }

        //get the callback url to return the parties to either pve or pvp
        String callbackURL = request.getCallbackURL();

        //create the battle engine
        currentBattle = new BattleEngine(partyA, partyB, playerAInput, playerBInput, new ConsoleOutputService());

        // Run the battle in a separate thread so REST calls aren't blocked
        new Thread(() -> {
            Party winner;
            try {
                winner = currentBattle.runBattle();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Battle finished! Winner: " + winner.getName());

            //send the results back to the caller if they exist
            if (callbackURL != null && !callbackURL.isBlank()) {
                RestTemplate restTemplate = new RestTemplate();
                try {
                    Map<String, Party> result = new HashMap<>();
                    result.put("partyA", partyA);
                    result.put("partyB", partyB);
                    restTemplate.postForEntity(callbackURL, result, Void.class);
                } catch (Exception e) {
                    System.out.println("Failed to send battle result to callback" + e.getMessage());
                }
            }
        }).start();

        return new Party[]{partyA, partyB};
    }

    private InputService getInputService(String type) {
        return switch(type.toUpperCase().trim()) {
            case "HTTP" -> new HttpInputService();
            case "AI" -> new AIInputService();
            default -> throw new IllegalArgumentException("Unknown input type: " + type);
        };
    }

    @PostMapping("/action")
    public String submitAction(@RequestBody String action) {
        if (currentBattle == null) return "No battle in progress";

        if (playerAInput instanceof HttpInputService)
            ((HttpInputService)playerAInput).submitAction(action);
        else if (playerBInput instanceof HttpInputService)
            ((HttpInputService)playerBInput).submitAction(action);

        return "Action submitted!";
    }

    // DTOs for HTTP requests
    public static class BattleStartRequest {
        private Party partyA;
        private Party partyB;
        private String inputA;
        private String inputB;
        private String callbackURL;

        public BattleStartRequest() {} // Jackson

        public Party getPartyA() { return partyA; }
        public void setPartyA(Party partyA) { this.partyA = partyA; }
        public Party getPartyB() { return partyB; }
        public void setPartyB(Party partyB) { this.partyB = partyB; }

        public String getInputA() { return inputA; }
        public void setInputA(String inputA) { this.inputA = inputA; }
        public String getInputB() { return inputB; }
        public void setInputB(String inputB) { this.inputB = inputB; }

        public String getCallbackURL() { return callbackURL; }
        public void setCallbackURL(String callbackURL) { this.callbackURL = callbackURL; }
    }

    public static class BattleActionRequest {
        private String action; // "attack goblin" etc.

        public BattleActionRequest() {} // Jackson

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }
}