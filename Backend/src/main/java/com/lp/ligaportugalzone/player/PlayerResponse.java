package com.lp.ligaportugalzone.player;

/**
 * What the API returns for a player.
 *
 * <p>The component names deliberately reproduce the JSON the {@link Player} entity used to
 * serialise into, so introducing this type changes nothing on the wire. The value is not in
 * the names — it is in the indirection: from here on the database schema can change without
 * the frontend noticing. Renaming the contract is a separate change, made together with the
 * frontend.
 *
 * <p>A record rather than a Lombok class: a response is immutable by nature, and Jackson
 * serialises records without any annotations.
 */
public record PlayerResponse(
        Integer id,
        String name,
        String nation,
        String team,
        String position,
        Integer age,
        Integer mp,
        Integer starts,
        Integer min,
        Integer gls,
        Integer ast,
        Integer pk,
        Integer crdY,
        Integer crdR,
        Double goalsPer90,
        Double assistsPer90
) {

    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getName(),
                player.getNation(),
                player.getTeam(),
                player.getPosition(),
                player.getAge(),
                player.getMp(),
                player.getStarts(),
                player.getMin(),
                player.getGls(),
                player.getAst(),
                player.getPk(),
                player.getCrdY(),
                player.getCrdR(),
                player.getGoalsPer90(),
                player.getAssistsPer90()
        );
    }
}
