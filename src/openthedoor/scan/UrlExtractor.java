package openthedoor.scan;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlExtractor {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("\\b([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\\b");
    private static final Pattern PATH_PATTERN = Pattern.compile("/[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]+\\.(json|xml|php|aspx|ashx|jsp|txt|swf|air)", Pattern.CASE_INSENSITIVE);

    private UrlExtractor() {}

    public static Set<String> urls(String text) {
        return find(URL_PATTERN, text);
    }

    public static Set<String> domains(String text) {
        return find(DOMAIN_PATTERN, text);
    }

    public static Set<String> paths(String text) {
        return find(PATH_PATTERN, text);
    }

    private static Set<String> find(Pattern pattern, String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            String value = trimNoise(m.group());
            if (!value.isEmpty()) out.add(value);
        }
        return out;
    }

    private static String trimNoise(String s) {
        while (s.endsWith(".") || s.endsWith(",") || s.endsWith(";") || s.endsWith(")") || s.endsWith("\"") || s.endsWith("'")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
