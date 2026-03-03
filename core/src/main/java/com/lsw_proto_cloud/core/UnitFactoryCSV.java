package com.lsw_proto_cloud.core;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UnitFactoryCSV implements UnitFactory {
    private static final Random random = new Random();
    private static final List<String> ENEMY_NAMES = loadNamesFromFile("/com/lsw_proto_cloud/core/enemy_names.txt");
    private static final List<String> HERO_NAMES = loadNamesFromFile("/com/lsw_proto_cloud/core/hero_names.txt");
    private static final HeroClass[] HERO_CLASSES = HeroClass.values();

    private static List<String> loadNamesFromFile(String filename) {
        List<String> names = new ArrayList<>();
        try (InputStream is = UnitFactoryCSV.class.getResourceAsStream(filename)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + filename);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) names.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading " + filename);
            throw new RuntimeException(e);
        }
        return names;
    }

    private static String getRandomName(List<String> names) {
        if  (names.isEmpty()) return "Unknown";
        return names.get(random.nextInt(names.size()));
    }

    public Party generateEnemyParty(int playerCumulativeLevel) {
        Party enemyParty = new Party("Enemy Party");
        int numEnemies = 1 + random.nextInt(4); //1-5 enemies

        //get a random total party level based on the player total level
        int minCumulative = Math.max(1, playerCumulativeLevel - 10);
        int maxCumulative = Math.max(1, playerCumulativeLevel);
        int enemyCumulativeLevel = minCumulative + random.nextInt(maxCumulative - minCumulative + 1);

        //distribute the enemy levels across all its units
        int remainingLevels = enemyCumulativeLevel;
        for (int i = 0; i < numEnemies; i++) {
            int enemiesLeft = numEnemies - i;
            int maxLevelForEnemy = Math.max(1, remainingLevels - (enemiesLeft - 1));
            int level = 1 + random.nextInt(maxLevelForEnemy);

            //prevent overflowing the remaining level cap
            if (level > remainingLevels) level = remainingLevels;
            remainingLevels -= level;

            //get a random name thats not already in the party
            String name = getRandomName(ENEMY_NAMES);
            while (enemyParty.getUnitByName(name) != null) name = getRandomName(ENEMY_NAMES);

            //create the enemy unit, with stats related to its level
            Unit enemy = new Unit(
                name,
                3 * level + 4,
                level,
                10 + level,
                0,
                HeroClass.WARRIOR
            );

            //add unit to party
            enemyParty.addUnit(enemy);
        }
        return enemyParty;
    }

    public List<Unit> generateHeroRecruits(int numRecruits) {
        List<Unit> recruits = new ArrayList<>();
        for (int i = 0; i < numRecruits; i++) {
            String name = getRandomName(HERO_NAMES);
            int level = 1 + random.nextInt(3); //lvl 1-3
            HeroClass heroClass = HeroClass.values()[random.nextInt(HERO_CLASSES.length)];
            recruits.add(new Unit(name, heroClass));
        }
        return recruits;
    }
}
