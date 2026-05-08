package openthedoor.detect;

import openthedoor.util.HexDump;
import java.nio.charset.StandardCharsets;

public final class PayloadFormatter {
    private PayloadFormatter() {}

    public static String printableAscii(byte[] bytes, int maxBytes) {
        int len = Math.min(bytes.length, maxBytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int v = bytes[i] & 0xFF;
            if (v >= 32 && v <= 126) sb.append((char) v);
            else if (v == '\n') sb.append("\\n\n");
            else if (v == '\r') sb.append("\\r");
            else if (v == '\t') sb.append("\\t");
            else if (v == 0) sb.append("\\0");
            else sb.append('.');
        }
        if (bytes.length > maxBytes) sb.append("\n... truncated ").append(bytes.length - maxBytes).append(" bytes");
        return sb.toString();
    }

    public static String utf8(byte[] bytes, int maxBytes) {
        int len = Math.min(bytes.length, maxBytes);
        String s = new String(bytes, 0, len, StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) s += "\n... truncated " + (bytes.length - maxBytes) + " bytes";
        return s;
    }

    public static String hex(byte[] bytes, int maxBytes) {
        return HexDump.pretty(bytes, maxBytes);
    }
}
