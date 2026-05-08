package openthedoor.scan;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class ScanResult {
    private static final int URL_REPORT_LIMIT = 40;
    private static final int DOMAIN_REPORT_LIMIT = 25;
    private static final int PATH_REPORT_LIMIT = 30;
    private static final int HOST_SUGGESTION_TARGET = 3;

    private final Set<String> urls = new TreeSet<>();
    private final Set<String> domains = new TreeSet<>();
    private final Set<String> paths = new TreeSet<>();
    private final Set<String> protocolHints = new TreeSet<>();
    private final Set<String> interestingFiles = new TreeSet<>();

    public void addUrls(Set<String> values) { urls.addAll(values); }
    public void addDomains(Set<String> values) { domains.addAll(values); }
    public void addPaths(Set<String> values) { paths.addAll(values); }
    public void addHint(String value) { protocolHints.add(value); }
    public void addInterestingFile(String value) { interestingFiles.add(value); }

    public String toMarkdown() {
        List<EndpointCandidate> candidates = endpointCandidates();
        List<EndpointCandidate> meaningfulUrls = meaningfulUrls(candidates);
        List<String> domainNames = domainNames(meaningfulUrls);
        List<String> possiblePaths = possiblePaths(meaningfulUrls);
        List<EndpointCandidate> hostSuggestions = hostSuggestions(meaningfulUrls);

        StringBuilder sb = new StringBuilder();
        sb.append("# OpenTheDoor APK scan report\n\n");
        appendUrlsFound(sb, meaningfulUrls);
        appendDomainNames(sb, domainNames);
        appendPossiblePaths(sb, possiblePaths);
        appendProtocolHints(sb, meaningfulUrls);
        appendSuggestedHosts(sb, hostSuggestions);
        appendSuggestedConfig(sb, hostSuggestions, meaningfulUrls);
        return sb.toString();
    }

    public void writeMarkdown(String outputPath) throws IOException {
        File out = new File(outputPath);
        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileWriter writer = new FileWriter(out)) {
            writer.write(toMarkdown());
        }
    }

    public String toConsoleString() {
        return toMarkdown();
    }

    private void appendUrlsFound(StringBuilder sb, List<EndpointCandidate> values) {
        sb.append("## URLs found\n\n");
        if (values.isEmpty()) {
            sb.append("No reliable URL found. Run the client with `tcp-listen` or inspect the config paths below.\n\n");
            return;
        }

        int shown = 0;
        for (EndpointCandidate candidate : values) {
            if (shown >= URL_REPORT_LIMIT) break;
            sb.append("- `").append(candidate.url).append("` - ").append(candidate.confidence())
                    .append(" - ").append(candidate.reason).append('\n');
            shown++;
        }
        appendOmittedCount(sb, values.size(), shown);
    }

    private void appendDomainNames(StringBuilder sb, List<String> values) {
        sb.append("## Domain names\n\n");
        if (values.isEmpty()) {
            sb.append("No reliable domain name found.\n\n");
            return;
        }

        int shown = 0;
        for (String value : values) {
            if (shown >= DOMAIN_REPORT_LIMIT) break;
            sb.append("- `").append(value).append("`\n");
            shown++;
        }
        appendOmittedCount(sb, values.size(), shown);
    }

    private void appendPossiblePaths(StringBuilder sb, List<String> values) {
        sb.append("## Possible access and config paths\n\n");
        if (values.isEmpty()) {
            sb.append("No reliable path or config file found.\n\n");
            return;
        }

        int shown = 0;
        for (String value : values) {
            if (shown >= PATH_REPORT_LIMIT) break;
            sb.append("- `").append(value).append("`\n");
            shown++;
        }
        appendOmittedCount(sb, values.size(), shown);
    }

    private void appendProtocolHints(StringBuilder sb, List<EndpointCandidate> meaningfulUrls) {
        Set<String> hints = new TreeSet<>(protocolHints);
        for (EndpointCandidate candidate : meaningfulUrls) {
            if ("https".equals(candidate.scheme)) hints.add("https");
            if ("http".equals(candidate.scheme)) hints.add("http");
            if (candidate.host.contains("firebaseio.com")) hints.add("firebase");
            if (candidate.host.contains("cloudfront.net") || candidate.host.contains("amazonaws.com")
                    || candidate.host.contains("cdn") || candidate.host.contains("netdna-cdn.com")) {
                hints.add("cdn/resource download");
            }
            if (candidate.path.endsWith(".json")) hints.add("json config");
            if (candidate.path.endsWith(".xml")) hints.add("xml config");
            if (candidate.path.endsWith(".php") || candidate.path.endsWith(".jsp")
                    || candidate.path.endsWith(".aspx") || candidate.path.endsWith(".ashx")) {
                hints.add("dynamic http endpoint");
            }
        }

        sb.append("## Protocol hints\n\n");
        if (hints.isEmpty()) {
            sb.append("None found.\n\n");
            return;
        }
        for (String hint : hints) sb.append("- `").append(hint).append("`\n");
        sb.append('\n');
    }

    private void appendSuggestedHosts(StringBuilder sb, List<EndpointCandidate> suggestions) {
        sb.append("## Suggested hosts entries\n\n");
        if (suggestions.isEmpty()) {
            sb.append("No reliable host entry to suggest. Start with `tcp-listen` or patch the client toward `127.0.0.1`.\n\n");
            return;
        }

        sb.append("```txt\n");
        for (EndpointCandidate candidate : suggestions) {
            sb.append("127.0.0.1 ").append(candidate.host).append('\n');
        }
        sb.append("```\n\n");

        sb.append("Try these first:\n");
        for (EndpointCandidate candidate : suggestions) {
            sb.append("- `").append(candidate.host).append("` from `").append(candidate.url)
                    .append("` - ").append(candidate.confidence()).append(" - ").append(candidate.reason).append('\n');
        }
        sb.append('\n');
    }

    private void appendSuggestedConfig(StringBuilder sb, List<EndpointCandidate> hostSuggestions, List<EndpointCandidate> meaningfulUrls) {
        EndpointCandidate best = !hostSuggestions.isEmpty() ? hostSuggestions.get(0)
                : (!meaningfulUrls.isEmpty() ? meaningfulUrls.get(0) : null);

        sb.append("## Suggested OpenTheDoor config\n\n");
        sb.append("```properties\n");
        if (best != null && best.looksLikeTcp()) {
            sb.append("mode=tcp-listen\n");
        } else {
            sb.append("mode=http-listen\n");
        }
        sb.append("host=0.0.0.0\n");
        sb.append("port=").append(best != null ? best.suggestedPort() : 80).append('\n');
        sb.append("mockDir=mocks\n");
        sb.append("savePackets=true\n");
        sb.append("logDir=logs\n");
        sb.append("```\n\n");

        if (best != null) {
            sb.append("Reason: based on `").append(best.url).append("`.\n");
        } else {
            sb.append("Reason: no endpoint was reliable enough, so this starts a generic HTTP mock listener.\n");
        }
    }

    private List<EndpointCandidate> endpointCandidates() {
        List<EndpointCandidate> out = new ArrayList<>();
        for (String url : urls) {
            EndpointCandidate candidate = EndpointCandidate.from(url);
            if (candidate.kind != EndpointKind.INVALID) out.add(candidate);
        }
        Collections.sort(out, new Comparator<EndpointCandidate>() {
            @Override
            public int compare(EndpointCandidate left, EndpointCandidate right) {
                int scoreDiff = Integer.compare(right.score, left.score);
                return scoreDiff != 0 ? scoreDiff : left.url.compareTo(right.url);
            }
        });
        return out;
    }

    private List<EndpointCandidate> meaningfulUrls(List<EndpointCandidate> candidates) {
        List<EndpointCandidate> out = new ArrayList<>();
        for (EndpointCandidate candidate : candidates) {
            if (candidate.score >= 45 && candidate.kind != EndpointKind.THIRD_PARTY_SDK
                    && candidate.kind != EndpointKind.REFERENCE) {
                out.add(candidate);
            }
        }
        if (out.isEmpty()) {
            for (EndpointCandidate candidate : candidates) {
                if (candidate.score >= 35 && candidate.kind != EndpointKind.REFERENCE) out.add(candidate);
            }
        }
        return out;
    }

    private List<String> domainNames(List<EndpointCandidate> meaningfulUrls) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (EndpointCandidate candidate : meaningfulUrls) {
            if (candidate.isPublicDomain()) out.add(candidate.host);
        }
        for (String domain : domains) {
            String lower = domain.toLowerCase(Locale.ROOT);
            if (out.size() >= DOMAIN_REPORT_LIMIT) break;
            if (domainScore(lower) >= 70) out.add(lower);
        }
        return new ArrayList<>(out);
    }

    private List<String> possiblePaths(List<EndpointCandidate> meaningfulUrls) {
        List<PathCandidate> ranked = new ArrayList<>();
        for (EndpointCandidate candidate : meaningfulUrls) {
            if (candidate.path != null && candidate.path.length() > 1) {
                int score = pathScore(candidate.path) + 20;
                ranked.add(new PathCandidate(candidate.path, score));
            }
        }
        for (String path : paths) {
            int score = pathScore(path);
            if (score >= 20) ranked.add(new PathCandidate(path, score));
        }
        for (String file : interestingFiles) {
            int score = fileScore(file);
            if (score >= 65) ranked.add(new PathCandidate(file, score));
        }

        Collections.sort(ranked, new Comparator<PathCandidate>() {
            @Override
            public int compare(PathCandidate left, PathCandidate right) {
                int scoreDiff = Integer.compare(right.score, left.score);
                return scoreDiff != 0 ? scoreDiff : left.value.compareTo(right.value);
            }
        });

        LinkedHashSet<String> deduped = new LinkedHashSet<>();
        for (PathCandidate candidate : ranked) deduped.add(candidate.value);
        return new ArrayList<>(deduped);
    }

    private List<EndpointCandidate> hostSuggestions(List<EndpointCandidate> meaningfulUrls) {
        List<EndpointCandidate> out = new ArrayList<>();
        Set<String> usedHosts = new LinkedHashSet<>();
        for (EndpointCandidate candidate : meaningfulUrls) {
            if (!candidate.isRedirectableHost()) continue;
            if (candidate.score < 60) continue;
            if (usedHosts.add(candidate.host)) out.add(candidate);
            if (out.size() >= HOST_SUGGESTION_TARGET) break;
        }
        return out;
    }

    private void appendOmittedCount(StringBuilder sb, int total, int shown) {
        if (total > shown) {
            sb.append("\n_").append(total - shown).append(" lower-confidence item(s) omitted._\n");
        }
        sb.append('\n');
    }

    private int domainScore(String domain) {
        if (EndpointCandidate.isKnownReferenceHost(domain) || EndpointCandidate.isKnownSdkHost(domain)) return -50;
        int score = 25;
        if (containsAny(domain, "game", "server", "api", "gateway", "proxy", "cdn", "live", "patch", "resource", "connection")) score += 45;
        if (containsAny(domain, "ankama", "dofus", "touch")) score += 40;
        if (domain.contains("amazonaws.com") || domain.contains("cloudfront.net") || domain.contains("netdna-cdn.com")) score += 20;
        return score;
    }

    private int pathScore(String path) {
        String lower = path.toLowerCase(Locale.ROOT).replace('\\', '/');
        if (isAppResourcePath(lower) || isMediaFile(lower)) return -100;

        int score = 0;
        if (containsAny(lower, "api", "gateway", "server", "config", "login", "auth", "account", "session",
                "endpoint", "version", "manifest", "cdn", "patch", "update", "service", "live", "connection")) {
            score += 35;
        }
        if (lower.endsWith(".php") || lower.endsWith(".aspx") || lower.endsWith(".ashx") || lower.endsWith(".jsp")) score += 25;
        if (lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".properties") || lower.endsWith(".dat")) score += 15;
        if (lower.length() > 120) score -= 15;
        return score;
    }

    private int fileScore(String file) {
        String lower = file.toLowerCase(Locale.ROOT).replace('\\', '/');
        if (lower.equals("androidmanifest.xml")) return 75;
        if (isMediaFile(lower)) return -100;
        if (lower.contains("/docs/")) return -100;
        if (lower.contains("/extensions/") && (lower.contains("/fb-res/") || lower.contains("/res/"))) return -100;
        if (lower.contains("application.xml")) return 85;
        if (lower.contains("settings.xml") || lower.contains("config") || lower.contains("server")) return 85;
        if (lower.endsWith(".properties") || lower.endsWith(".dat")) return 70;
        if (lower.endsWith(".json") || lower.endsWith(".xml")) return 55;
        if (lower.endsWith(".swf") && !lower.contains("/extensions/")) return 65;
        return 0;
    }

    private boolean isAppResourcePath(String lower) {
        return lower.startsWith("/layout") || lower.startsWith("/drawable") || lower.startsWith("/color")
                || lower.startsWith("/anim") || lower.startsWith("/animator") || lower.startsWith("/mipmap")
                || lower.startsWith("/values") || lower.startsWith("/res/") || lower.startsWith("/docs/")
                || lower.startsWith("/meta-inf/") || lower.startsWith("/ane/") || lower.startsWith("/air/")
                || lower.startsWith("/codehaus/") || lower.startsWith("/org/") || lower.startsWith("/com/");
    }

    private boolean isMediaFile(String lower) {
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                || lower.endsWith(".webp") || lower.endsWith(".ogg") || lower.endsWith(".mp3") || lower.endsWith(".wav")
                || lower.endsWith(".mp4") || lower.endsWith(".3gp") || lower.endsWith(".fnt") || lower.endsWith(".css")
                || lower.endsWith(".js");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private enum EndpointKind {
        PRIMARY,
        CDN,
        PRIVATE_TEST,
        THIRD_PARTY_SDK,
        REFERENCE,
        INVALID
    }

    private static final class PathCandidate {
        final String value;
        final int score;

        PathCandidate(String value, int score) {
            this.value = value;
            this.score = score;
        }
    }

    private static final class EndpointCandidate {
        final String url;
        final String scheme;
        final String host;
        final String path;
        final int port;
        final int score;
        final String reason;
        final EndpointKind kind;

        private EndpointCandidate(String url, String scheme, String host, String path, int port, int score, String reason, EndpointKind kind) {
            this.url = url;
            this.scheme = scheme;
            this.host = host;
            this.path = path;
            this.port = port;
            this.score = score;
            this.reason = reason;
            this.kind = kind;
        }

        static EndpointCandidate from(String rawUrl) {
            if (rawUrl.matches("(?i)^https?://[^/]+:$")) return invalid(rawUrl);

            URI uri;
            try {
                uri = new URI(rawUrl);
            } catch (URISyntaxException e) {
                return invalid(rawUrl);
            }

            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) return invalid(rawUrl);
            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith(".") || host.contains("*")) return invalid(rawUrl);
            if (host.indexOf('.') < 0 && !isPrivateOrLocalHost(host)) return invalid(rawUrl);

            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String path = uri.getRawPath();
            if (path == null) path = "/";
            String signal = host + " " + path.toLowerCase(Locale.ROOT);

            if (isKnownReferenceHost(host) || isReferencePath(path)) {
                return new EndpointCandidate(rawUrl, scheme, host, path, uri.getPort(), 0, "reference/documentation URL", EndpointKind.REFERENCE);
            }

            boolean privateHost = isPrivateOrLocalHost(host);
            boolean sdkHost = isKnownSdkHost(host);
            boolean cdnHost = isCdnHost(host);
            int score = 35;
            String reason = "network endpoint";
            EndpointKind kind = EndpointKind.PRIMARY;

            if (privateHost) {
                score += 35;
                reason = "private/local endpoint found in client";
                kind = EndpointKind.PRIVATE_TEST;
            }
            if (containsAny(signal, "api", "gateway", "server", "login", "auth", "session", "config",
                    "endpoint", "proxy", "patch", "update", "live", "resource", "connection")) {
                score += 30;
                reason = "backend/config keyword";
            }
            if (containsAny(signal, "ankama", "dofus", "touch")) {
                score += 35;
                reason = "game/vendor domain";
            }
            if (path.endsWith(".php") || path.endsWith(".jsp") || path.endsWith(".aspx") || path.endsWith(".ashx")) {
                score += 20;
                reason = "dynamic server route";
            } else if (path.endsWith(".json") || path.endsWith(".xml") || path.endsWith(".txt") || path.endsWith(".properties")) {
                score += 15;
                reason = "structured config/resource file";
            }
            if (cdnHost) {
                score += 10;
                kind = EndpointKind.CDN;
                reason = "CDN/resource host";
            }
            if (sdkHost || looksLikeAdOrTracking(signal)) {
                score -= 60;
                reason = "known SDK/ad/social/payment endpoint";
                kind = EndpointKind.THIRD_PARTY_SDK;
            }
            if (looksLikeStaticAsset(path)) score -= 20;
            if (path.length() > 120) score -= 10;

            return new EndpointCandidate(rawUrl, scheme, host, path, uri.getPort(), score, reason, kind);
        }

        private static EndpointCandidate invalid(String rawUrl) {
            return new EndpointCandidate(rawUrl, "", "", "", -1, -100, "invalid URL", EndpointKind.INVALID);
        }

        boolean isRedirectableHost() {
            return isPublicDomain()
                    && kind != EndpointKind.THIRD_PARTY_SDK && kind != EndpointKind.REFERENCE;
        }

        boolean isIpAddress() {
            return isIpAddress(host);
        }

        boolean isPublicDomain() {
            return !isIpAddress() && !isPrivateOrLocalHost(host);
        }

        int suggestedPort() {
            if (port > 0) return port;
            return "https".equals(scheme) ? 443 : 80;
        }

        boolean looksLikeTcp() {
            return port > 0 && port != 80 && port != 443 && !pathEndsWithHttpConfig();
        }

        private boolean pathEndsWithHttpConfig() {
            return path.endsWith(".json") || path.endsWith(".xml") || path.endsWith(".txt")
                    || path.endsWith(".properties") || path.endsWith(".php") || path.endsWith(".jsp")
                    || path.endsWith(".aspx") || path.endsWith(".ashx") || path.endsWith("/");
        }

        String confidence() {
            if (score >= 85) return "HIGH";
            if (score >= 60) return "MEDIUM";
            return "LOW";
        }

        static boolean isIpAddress(String host) {
            return host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
        }

        private static boolean isPrivateOrLocalHost(String host) {
            return host.equals("localhost") || host.equals("127.0.0.1") || host.startsWith("10.")
                    || host.startsWith("192.168.") || host.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
        }

        static boolean isKnownReferenceHost(String host) {
            return host.equals("ns.adobe.com") || host.equals("schemas.android.com") || host.equals("schema.org")
                    || host.equals("purl.org") || host.endsWith(".w3.org") || host.equals("www.w3.org")
                    || host.contains("developer.android.com") || host.contains("developers.facebook.com")
                    || host.contains("documentation.") || host.contains("wikipedia.org") || host.contains("github.com")
                    || host.contains("apache.org") || host.contains("iana.org") || host.contains("ecma-international.org")
                    || host.contains("microsoft.com") || host.contains("monotype.com") || host.contains("verisign.com")
                    || host.equals("www.adobe.com") || host.endsWith(".adobe.com") || host.equals("goo.gl")
                    || host.startsWith("crl.") || host.startsWith("ocsp.");
        }

        static boolean isKnownSdkHost(String host) {
            return host.contains("facebook.com") || host.contains("fbcdn.net") || host.contains("doubleclick.net")
                    || host.contains("googleads") || host.contains("googlesyndication.com") || host.contains("admob")
                    || host.contains("tapjoy") || host.contains("playhaven") || host.contains("amazon-adsystem")
                    || host.contains("millennialmedia") || host.contains("mmedia.com") || host.contains("chartboost")
                    || host.contains("flurry") || host.contains("onesignal") || host.contains("app-measurement.com")
                    || host.contains("google-analytics.com") || host.contains("gcm.googleapis.com")
                    || host.contains("firebase.google.com") || host.contains("pushwoosh.com")
                    || host.contains("adjust.com") || host.contains("adjust.net") || host.contains("adjust.world")
                    || host.contains("rollbar.com") || host.contains("paypal.com") || host.contains("twitter.com")
                    || host.contains("googleapis.com") || host.equals("google.com") || host.equals("www.google.com")
                    || host.equals("plus.google.com") || host.equals("accounts.google.com") || host.equals("login.live.com")
                    || host.equals("login.yahoo.com") || host.equals("play.google.com") || host.equals("csi.gstatic.com")
                    || host.equals("ssl.gstatic.com") || host.equals("www.amazon.com") || host.equals("www.linkedin.com")
                    || host.contains("googletagmanager.com");
        }

        private static boolean isCdnHost(String host) {
            return host.contains("cloudfront.net") || host.contains("amazonaws.com")
                    || host.contains("netdna-cdn.com") || host.contains("cdn.");
        }

        private static boolean isReferencePath(String path) {
            String lower = path.toLowerCase(Locale.ROOT);
            return lower.contains("/docs/") || lower.contains("/policy/") || lower.contains("/reference/")
                    || lower.contains("/schema") || lower.endsWith(".dtd") || lower.endsWith(".crl")
                    || lower.endsWith(".crt") || lower.contains("/licenses/");
        }

        private static boolean looksLikeAdOrTracking(String value) {
            return containsAny(value, "ads", "adserver", "analytics", "tracking", "measurement", "offer", "billing",
                    "purchase", "wallet", "iap", "market.android.com", "play.google.com/store", "recaptcha");
        }

        private static boolean looksLikeStaticAsset(String path) {
            String lower = path.toLowerCase(Locale.ROOT);
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                    || lower.endsWith(".webp") || lower.endsWith(".css") || lower.endsWith(".js")
                    || lower.endsWith(".html") || lower.endsWith(".htm");
        }
    }
}
