package com.lp.ligaportugalzone.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test: no Spring, no database, no mocks. It runs in milliseconds because
 * {@link SlugUtils} has no collaborators.
 */
class SlugUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "Benfica,           Benfica",
            "Vitória Guimarães, Vitória-Guimarães",
            "Santa Clara,       Santa-Clara",
            "Gil Vicente FC,    Gil-Vicente-FC",
            "Sporting CP,       Sporting-CP"
    })
    void toSlugReplacesSpacesWithHyphens(String teamName, String expectedSlug) {
        assertThat(SlugUtils.toSlug(teamName)).isEqualTo(expectedSlug);
    }

    @Test
    void toSlugKeepsOnlyTheCountryCodeWhenTheLastWordHasThreeLetters() {
        // Nations are stored as "uy URU"; the three-letter suffix is the country code.
        assertThat(SlugUtils.toSlug("uy URU")).isEqualTo("URU");
    }

    @ParameterizedTest
    @CsvSource({
            "Rio Ave,  AVE",
            "Casa Pia, PIA"
    })
    void toSlugMistakesTeamNamesEndingInAThreeLetterWordForCountryCodes(String teamName, String wrongSlug) {
        // Known defect, kept here so it is visible: the "last word has three letters" rule
        // cannot tell "uy URU" from "Rio Ave". Harmless today only because toSlug has no callers.
        assertThat(SlugUtils.toSlug(teamName)).isEqualTo(wrongSlug);
    }

    @Test
    void toSlugTrimsAndCollapsesWhitespace() {
        assertThat(SlugUtils.toSlug("  Vitória   Guimarães  ")).isEqualTo("Vitória-Guimarães");
    }

    @Test
    void toSlugReturnsNullForNullInput() {
        assertThat(SlugUtils.toSlug(null)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "Benfica,           Benfica",
            "Rio-Ave,           Rio Ave",
            "Vitória-Guimarães, Vitória Guimarães"
    })
    void fromSlugReplacesHyphensWithSpaces(String slug, String expectedTeamName) {
        assertThat(SlugUtils.fromSlug(slug)).isEqualTo(expectedTeamName);
    }

    @Test
    void fromSlugCollapsesRepeatedHyphens() {
        assertThat(SlugUtils.fromSlug("Casa--Pia")).isEqualTo("Casa Pia");
    }

    @Test
    void fromSlugReturnsNullForNullInput() {
        assertThat(SlugUtils.fromSlug(null)).isNull();
    }
}
