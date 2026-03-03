package com.lsw_proto_cloud.core;

import java.util.List;

public class ConsoleOutputService implements OutputService {
    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void showParty(List<Party> partyList) {
        System.out.println("=====================================================");
        for (Party party : partyList) {
            System.out.println(party.toString());
        }
        System.out.println("=====================================================");
    }

    @Override
    public void announceTurn(Unit unit) {
        System.out.println("It is " + unit.getName() + "'s turn!");
    }

    @Override
    public void showUnitBasic(Unit unit) {
        System.out.println(unit.toString());
    }

    @Override
    public void showUnitAdvanced(Unit unit) {
        //TODO:
    }

    @Override
    public void showInventory(Party playerParty) {
        System.out.println("===================== Inventory ======================");
        if (playerParty.getInventory().isEmpty()) {
            System.out.println("Empty inventory!");
        }
        else {
            for (Items item : playerParty.getInventory().keySet()) {
                System.out.println(item + " : " + playerParty.getInventory().get(item));
            }
        }
        System.out.println("=====================================================");
    }

    @Override
    public void showItemShop() {
        System.out.println("==================== Item Shop ======================");
        for (Items item : Items.values()) {
            System.out.printf("%s\tCost: %dg\tHP: %d\tMP: %d%n", item, item.getCost(), item.getHealthBoost(), item.getManaBoost());
        }
        System.out.println("=====================================================");
    }
}
