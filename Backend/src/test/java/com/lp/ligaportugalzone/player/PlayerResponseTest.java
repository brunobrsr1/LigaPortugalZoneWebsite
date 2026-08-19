package com.lp.ligaportugalzone.player;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The mapping is sixteen assignments in a row, which is precisely the shape of code where one
 * line goes missing and nobody notices. Both tests exist to make that loud.
 */
class PlayerResponseTest {

    @Test
    void copiesEveryFieldFromTheEntity() {
        Player player = PlayerFixtures.player("Vangelis Pavlidis", "gr GRE", "Benfica", "FW");
        player.setId(7);

        PlayerResponse response = PlayerResponse.from(player);

        assertThat(response.id()).isEqualTo(7);
        assertThat(response.name()).isEqualTo("Vangelis Pavlidis");
        assertThat(response.nation()).isEqualTo("gr GRE");
        assertThat(response.team()).isEqualTo("Benfica");
        assertThat(response.position()).isEqualTo("FW");
        assertThat(response.age()).isEqualTo(25);
        assertThat(response.mp()).isEqualTo(10);
        assertThat(response.starts()).isEqualTo(8);
        assertThat(response.min()).isEqualTo(700);
        assertThat(response.gls()).isEqualTo(3);
        assertThat(response.ast()).isEqualTo(1);
        assertThat(response.pk()).isEqualTo(0);
        assertThat(response.crdY()).isEqualTo(2);
        assertThat(response.crdR()).isEqualTo(0);
        assertThat(response.goalsPer90()).isEqualTo(0.39);
        assertThat(response.assistsPer90()).isEqualTo(0.13);
    }

    /**
     * Guards the case the test above cannot catch: a field added to the entity later and never
     * mapped. When the response is deliberately allowed to drop a field, add it to the
     * exclusion list here — so that the decision is written down rather than forgotten.
     */
    @Test
    void exposesEveryFieldTheEntityHas() {
        List<String> deliberatelyNotExposed = List.of();

        List<String> entityFields = Arrays.stream(Player.class.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName)
                .filter(name -> !deliberatelyNotExposed.contains(name))
                .toList();

        List<String> responseComponents = Arrays.stream(PlayerResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(responseComponents).containsExactlyInAnyOrderElementsOf(entityFields);
    }
}
