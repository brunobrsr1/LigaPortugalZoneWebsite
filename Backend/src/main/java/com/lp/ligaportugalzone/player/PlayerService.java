package com.lp.ligaportugalzone.player;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;

    /**
     * Entities do not leave this class: callers get {@link PlayerResponse}. That keeps the
     * persistence model free to change without the web layer following it.
     */
    @Transactional(readOnly = true)
    public List<PlayerResponse> findPlayers(String team, String nation, String position, String name) {
        return playerRepository.findAll().stream()
                .filter(player -> team == null || player.getTeam().equalsIgnoreCase(team))
                .filter(player -> nation == null || (player.getNation() != null && player.getNation().toUpperCase().endsWith(nation.toUpperCase())))
                .filter(player -> position == null || player.getPosition().equalsIgnoreCase(position))
                .filter(player -> name == null || player.getName().toLowerCase().contains(name.toLowerCase()))
                .map(PlayerResponse::from)
                .toList();
    }
}
