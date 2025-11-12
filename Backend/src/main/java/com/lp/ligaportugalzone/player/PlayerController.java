package com.lp.ligaportugalzone.player;

import com.lp.ligaportugalzone.util.SlugUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
@AllArgsConstructor
public class PlayerController {
    private final PlayerService playerService;

    @GetMapping("/data")
    public ResponseEntity<List<Player>> getFilterPlayers(
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String nation,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String name) {

        if (team != null) {
            team = SlugUtils.fromSlug(team);
        }

        List<Player> players = playerService.findPlayers(team, nation, position, name);
        return ResponseEntity.ok(players);

    }
}
