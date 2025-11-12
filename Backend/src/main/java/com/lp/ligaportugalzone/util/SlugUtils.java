package com.lp.ligaportugalzone.util;

public class SlugUtils {
    private SlugUtils() {}

    public static String toSlug(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.contains(" ")) {
            String[] parts = trimmed.split("\\s+");
            String last = parts[parts.length - 1];
            if (last.length() == 3 && last.chars().allMatch(Character::isLetter)) {
                return last.toUpperCase();
            }
        }
        return trimmed.replaceAll("\\s+", "-");
    }

    public static String fromSlug(String slug) {
        if (slug == null) {
            return null;
        }
        return slug.replaceAll("-+", " ");
    }
}
