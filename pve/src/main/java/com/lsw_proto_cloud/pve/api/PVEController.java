package com.lsw_proto_cloud.pve.api;

import com.lsw_proto_cloud.core.ConsoleOutputService;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.pve.PVECampaignEngine;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pve")
public class PVEController {
    private PVECampaignEngine engine;

    // start a new pve campaign, passing in a player party and URL to the battle module
    @PostMapping("/start")
    public String startCampaign(
        @RequestBody Party playerParty,
        @RequestParam("battleModuleURL") String battleModuleURL,
        @RequestParam("battleCallbackURL") String battleCallbackURL
    ) {
        //create the pve engine
        engine = new PVECampaignEngine(
                playerParty,
                new ConsoleOutputService(),
                battleModuleURL, //the url of the battle module
                battleCallbackURL); //callback url for battle results
        return "PvE campaign started with party: " + playerParty.getName();
    }

    // Accept plain text actions
    @PostMapping(value = "/action", consumes = "text/plain")
    public String pveAction(@RequestBody String action) {
        if (engine == null) return "PvE campaign not started!";
        engine.handleAction(action.trim());
        return "Action processed: " + action;
    }

    // Endpoint for battle module callback, defined by battleCallBackURL
    @PostMapping("/battleCallback")
    public void battleCallback(@RequestBody Map<String, Party> result) {
        if (engine == null) return;
        engine.battleFinished(result.get("partyA"), result.get("partyB"));
    }
}