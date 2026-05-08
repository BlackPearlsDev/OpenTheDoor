package openthedoor.scan;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApkScanner {
    private final String apkPath;

    public ApkScanner(String apkPath) {
        this.apkPath = apkPath;
    }

    public ScanResult scan() throws IOException {
        File apk = new File(apkPath);
        if (!apk.exists() || !apk.isFile()) {
            throw new IOException("APK not found: " + apk.getAbsolutePath());
        }

        ScanResult result = new ScanResult();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(apk.toPath()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                String lower = name.toLowerCase(Locale.ROOT);

                if (isInterestingFile(lower)) result.addInterestingFile(name);

                byte[] data = readEntry(zip, 8 * 1024 * 1024);
                String text = strings(data);
                result.addUrls(UrlExtractor.urls(text));
                result.addDomains(UrlExtractor.domains(text));
                result.addPaths(UrlExtractor.paths(text));
                detectHints(text, lower, result);
            }
        }
        return result;
    }

    private boolean isInterestingFile(String lower) {
        return lower.endsWith(".swf") || lower.endsWith(".xml") || lower.endsWith(".json") || lower.endsWith(".air")
                || lower.contains("assets/") || lower.contains("raw/");
    }

    private byte[] readEntry(ZipInputStream zip, int maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int n;
        while ((n = zip.read(buffer)) != -1) {
            total += n;
            if (total > maxBytes) break;
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private String strings(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            int v = b & 0xFF;
            if (v >= 32 && v <= 126) sb.append((char) v);
            else sb.append(' ');
        }
        sb.append('\n').append(new String(data, StandardCharsets.ISO_8859_1));
        return sb.toString();
    }

    private void detectHints(String text, String fileName, ScanResult result) {
        String lower = (fileName + "\n" + text).toLowerCase(Locale.ROOT);
        String[] keywords = {
                "socket", "xmlsocket", "connect", "server", "gateway", "api", "cdn",
                "mqtt", "websocket", "amf", "remoting", "login", "auth", "endpoint", "host", "port"
        };
        for (String k : keywords) if (lower.contains(k)) result.addHint(k);
        if (lower.contains("fws") || lower.contains("cws") || lower.endsWith(".swf")) result.addHint("swf/flash");
        if (lower.contains("adobe air") || lower.contains("application.xml")) result.addHint("adobe-air");
    }
}
