package com.connecthub.common;

import org.owasp.html.Encoding;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.util.regex.Pattern;

/**
 * Normalises user-supplied plain text. All markup is stripped with the OWASP sanitizer (no
 * elements allowed), entities are decoded back to plain characters (the client escapes on
 * render), and control characters other than newlines/tabs are removed.
 */
public final class TextSanitizer {

    private static final PolicyFactory STRIP_ALL = new HtmlPolicyBuilder().toFactory();
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\n\\t]]");
    private static final Pattern EXCESS_NEWLINES = Pattern.compile("\\n{3,}");

    private TextSanitizer() {
    }

    /** Single or multi-line text. Returns null for null/blank input. */
    public static String plain(String input) {
        if (input == null) {
            return null;
        }
        String s = Encoding.decodeHtml(STRIP_ALL.sanitize(input.replace("\r\n", "\n")));
        s = CONTROL.matcher(s).replaceAll("");
        s = EXCESS_NEWLINES.matcher(s).replaceAll("\n\n").strip();
        return s.isEmpty() ? null : s;
    }

    /** Single-line text: newlines and tabs collapse to a single space. */
    public static String line(String input) {
        String s = plain(input);
        return s == null ? null : s.replaceAll("\\s+", " ");
    }
}
