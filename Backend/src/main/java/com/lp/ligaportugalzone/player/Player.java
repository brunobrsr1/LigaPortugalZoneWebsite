package com.lp.ligaportugalzone.player;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="player_data")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "player_name")
    private String name;

    private String nation;

    @Column(name = "team_name")
    private String team;

    private String position;

    private Integer age;

    @Column(name = "matches_played")
    private Integer mp;

    private Integer starts;

    @Column(name = "minutes_played")
    private Integer min;

    @Column(name = "goals")
    private Integer gls;

    @Column(name = "assists")
    private Integer ast;

    @Column(name = "penalties_scored")
    private Integer pk;

    @Column(name = "yellow_cards")
    private Integer crdY;

    @Column(name = "red_cards")
    private Integer crdR;

    @Column(name = "expected_goals")
    private Double xg;

    @Column(name = "expected_assists")
    private Double xag;
}
