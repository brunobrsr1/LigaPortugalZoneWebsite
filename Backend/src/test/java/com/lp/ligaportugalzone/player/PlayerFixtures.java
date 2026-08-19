package com.lp.ligaportugalzone.player;

/**
 * Builds {@link Player} instances for tests. Keeps the tests readable: each test states only
 * the fields it actually cares about.
 */
final class PlayerFixtures {

    private PlayerFixtures() {
    }

    static Player player(String name, String nation, String team, String position) {
        Player player = new Player();
        player.setName(name);
        player.setNation(nation);
        player.setTeam(team);
        player.setPosition(position);
        player.setAge(25);
        player.setMp(10);
        player.setStarts(8);
        player.setMin(700);
        player.setGls(3);
        player.setAst(1);
        player.setPk(0);
        player.setCrdY(2);
        player.setCrdR(0);
        player.setGoalsPer90(0.39);
        player.setAssistsPer90(0.13);
        return player;
    }
}
