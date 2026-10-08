package com.connecthub.moderation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Soft profanity filter: masks listed words ("fuck" → "f***") rather than rejecting content.
 * Matches whole words with common suffixes and simple leetspeak (sh1t, a$$hole). The client ships
 * the same list to warn before posting; the server is the source of truth.
 */
@Component
public class ProfanityFilter {

    private static final Map<Character, String> LEET = Map.of(
            'a', "[a@4]", 'e', "[e3]", 'i', "[i1!|]", 'o', "[o0]", 's', "[s5$]", 't', "[t7+]", 'g', "[g9]", 'l', "[l1|]");

    private final Pattern pattern;

    public ProfanityFilter() {
        String alternatives;
        try {
            String raw = new ClassPathResource("profanity.txt").getContentAsString(StandardCharsets.UTF_8);
            alternatives = Arrays.stream(raw.split("\\R"))
                    .map(String::trim)
                    .filter(w -> !w.isEmpty() && !w.startsWith("#"))
                    .sorted((a, b) -> b.length() - a.length()) // longest first
                    .map(ProfanityFilter::toRegex)
                    .collect(Collectors.joining("|"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.pattern = Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + alternatives + ")(?:s|es|ed|er|ers|ing|in|y|head|heads)?(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** Returns the text with profane words masked, or the input unchanged (including null). */
    public String clean(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher m = pattern.matcher(text);
        if (!m.find()) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        do {
            String word = m.group();
            m.appendReplacement(out, Matcher.quoteReplacement(word.charAt(0) + "*".repeat(word.length() - 1)));
        } while (m.find());
        m.appendTail(out);
        return out.toString();
    }

    public boolean isProfane(String text) {
        return text != null && pattern.matcher(text).find();
    }

    private static String toRegex(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toLowerCase().toCharArray()) {
            sb.append(LEET.getOrDefault(c, Pattern.quote(String.valueOf(c))));
            sb.append("+"); // tolerate stretched letters: "fuuuck"
        }
        return sb.toString();
    }
}
