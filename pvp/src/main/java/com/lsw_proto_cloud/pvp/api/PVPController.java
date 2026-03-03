package com.lsw_proto_cloud.pvp.api;

import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.ConsoleOutputService;
import com.lsw_proto_cloud.pvp.PVPMatchEngine;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pvp")
public class PVPController {

    private PVPMatchEngine engine;

    @PostMapping("/start")
    public String startMatch(
        @RequestBody PvPStartRequest request,
        @RequestParam("battleModuleURL") String battleModuleURL,
        @RequestParam("battleCallbackURL") String battleCallbackURL
    ) {
        engine = new PVPMatchEngine(
                request.getPlayerA(),
                request.getPlayerB(),
                battleModuleURL,
                battleCallbackURL,
                new ConsoleOutputService()
        );

        engine.startMatch();
        return "PvP match started between " + request.getPlayerA().getName() + " and " + request.getPlayerB().getName();
    }

    @PostMapping(value = "/action", consumes = "text/plain")
    public String submitAction(@RequestBody String action) {
        if (engine == null || !engine.isInBattle()) return "No PvP match in progress!";
        engine.handleAction(action.trim());
        return "Action submitted: " + action;
    }

    /** Callback endpoint called by Battle module when match finishes */
    @PostMapping("/battleCallback")
    public String receiveBattleResult(@RequestBody Map<String, Party> result) {
        Party partyA = result.get("partyA");
        Party partyB = result.get("partyB");

        // Decide winner
        Party winner;
        if (partyA.getUnits().stream().anyMatch(u -> u.getHealth() > 0)) {
            winner = partyA;
        } else {
            winner = partyB;
        }

        // Output winner
        new ConsoleOutputService().showMessage("PvP match finished! Winner: " + winner.getName());

        // Mark engine as finished
        if (engine != null) {
            engine.handleAction("finished"); // internally sets currentlyInBattle=false
        }

        return "Result received. Winner: " + winner.getName();
    }

    // DTO for starting PvP match
    public static class PvPStartRequest {
        private Party playerA;
        private Party playerB;

        public PvPStartRequest() {}

        public Party getPlayerA() { return playerA; }
        public void setPlayerA(Party playerA) { this.playerA = playerA; }

        public Party getPlayerB() { return playerB; }
        public void setPlayerB(Party playerB) { this.playerB = playerB; }
    }
}