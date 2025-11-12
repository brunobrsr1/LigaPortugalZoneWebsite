package com.lp.ligaportugalzone.player;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public List<Player> findPlayers(String team, String nation, String position, String name) {
        return playerRepository.findAll().stream()
                .filter(player -> team == null || player.getTeam().equalsIgnoreCase(team))
                .filter(player -> nation == null || (player.getNation() != null && player.getNation().toUpperCase().endsWith(nation.toUpperCase())))
                .filter(player -> position == null || player.getPosition().equalsIgnoreCase(position))
                .filter(player -> name == null || player.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }
}
