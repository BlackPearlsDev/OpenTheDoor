package openthedoor.scan;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlExtractor {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("\\b([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\\b");
    private static final Pattern PATH_PATTERN = Pattern.compile("/[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]+\\.(json|xml|php|aspx|ashx|jsp|txt|swf|air)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_VALUE_LENGTH = 240;
    private static final String[] KNOWN_TLDS = {
            "com", "net", "org", "mobi", "info", "biz", "io", "co", "me", "tv", "cc",
            "app", "dev", "cloud", "jp", "uk", "de", "fr", "us", "ca", "cn", "hk",
            "kr", "ru", "br", "in", "au", "nl", "se", "no", "fi", "dk", "pl", "it",
            "es", "eu"
    };
    private static final String[] PACKAGE_PREFIXES = {
            "android.", "java.", "javax.", "kotlin.", "kotlinx.", "dalvik.", "flash.",
            "vnd.", "window.", "document.", "navigator.", "resources.", "properties.",
            "androidx.", "cordova.", "regex.", "stringbuilder.", "com.android.",
            "com.google.android.", "com.google.gms.", "com.google.firebase.", "com.facebook.",
            "com.unity3d.", "com.amazon.android.", "org.apache.", "org.json."
    };

    private UrlExtractor() {}

    public static Set<String> urls(String text) {
        return find(URL_PATTERN, text, MatchKind.URL);
    }

    public static Set<String> domains(String text) {
        return find(DOMAIN_PATTERN, text, MatchKind.DOMAIN);
    }

    public static Set<String> paths(String text) {
        return find(PATH_PATTERN, text, MatchKind.PATH);
    }

    private static Set<String> find(Pattern pattern, String text, MatchKind kind) {
        Set<String> out = new TreeSet<>();
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            String value = trimNoise(m.group());
            if (isUseful(value, kind)) out.add(value);
        }
        return out;
    }

    private static String trimNoise(String s) {
        while (s.endsWith(".") || s.endsWith(",") || s.endsWith(";") || s.endsWith(")") || s.endsWith("]")
                || s.endsWith("}") || s.endsWith(">") || s.endsWith("\"") || s.endsWith("'")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static boolean isUseful(String value, MatchKind kind) {
        if (value == null || value.isEmpty() || value.length() > MAX_VALUE_LENGTH) return false;
        if (value.indexOf('\\') >= 0) return false;
        if (value.contains("://") && !value.toLowerCase().startsWith("http")) return false;

        if (kind == MatchKind.DOMAIN) return isUsefulDomain(value);
        if (kind == MatchKind.PATH) return isUsefulPath(value);
        return true;
    }

    private static boolean isUsefulDomain(String value) {
        String lower = value.toLowerCase();
        if (!value.equals(lower)) return false;
        if (lower.indexOf('_') >= 0) return false;
        if (lower.startsWith(".") || lower.startsWith("-") || lower.endsWith("-")) return false;
        if (hasPrefix(lower, PACKAGE_PREFIXES)) return false;

        int lastDot = lower.lastIndexOf('.');
        if (lastDot < 1 || lastDot == lower.length() - 1) return false;
        String lastPart = lower.substring(lastDot + 1);
        if (isFileExtension(lastPart)) return false;
        if (!isKnownTld(lastPart)) return false;

        String firstPart = lower.substring(0, lower.indexOf('.'));
        if (lower.indexOf('.') == lastDot && firstPart.length() <= 1) return false;
        if (looksLikeCodeSymbol(lower)) return false;

        return true;
    }

    private static boolean isUsefulPath(String value) {
        if (!value.startsWith("/") || value.startsWith("//")) return false;
        if (value.contains("://")) return false;
        return true;
    }

    private static boolean isFileExtension(String value) {
        return value.equals("json") || value.equals("xml") || value.equals("php") || value.equals("txt")
                || value.equals("swf") || value.equals("air") || value.equals("png") || value.equals("jpg")
                || value.equals("jpeg") || value.equals("gif") || value.equals("webp") || value.equals("css")
                || value.equals("js") || value.equals("dex") || value.equals("jar") || value.equals("java")
                || value.equals("class") || value.equals("cpp") || value.equals("cc") || value.equals("so")
                || value.equals("dat") || value.equals("properties") || value.equals("fnt") || value.equals("ogg")
                || value.equals("mp3") || value.equals("mp4") || value.equals("ttf") || value.equals("otf")
                || value.equals("dtd") || value.equals("crl") || value.equals("crt") || value.equals("prof")
                || value.equals("profm");
    }

    private static boolean isKnownTld(String value) {
        for (String tld : KNOWN_TLDS) {
            if (tld.equals(value)) return true;
        }
        return false;
    }

    private static boolean hasPrefix(String value, String[] prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) return true;
        }
        return false;
    }

    private static boolean looksLikeCodeSymbol(String value) {
        return value.contains(".get") || value.contains(".set") || value.contains(".on")
                || value.contains(".prototype") || value.contains(".length") || value.contains(".size")
                || value.contains(".push") || value.contains(".run") || value.contains(".start")
                || value.contains(".stop") || value.contains(".create");
    }

    private enum MatchKind {
        URL,
        DOMAIN,
        PATH
    }
}
