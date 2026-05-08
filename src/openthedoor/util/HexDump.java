package openthedoor.util;

public final class HexDump {
    private HexDump() {}

    public static String pretty(byte[] bytes, int maxBytes) {
        int len = Math.min(bytes.length, maxBytes);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < len; i += 16) {
            out.append(String.format("%04X  ", i));
            StringBuilder ascii = new StringBuilder();
            for (int j = 0; j < 16; j++) {
                if (i + j < len) {
                    int v = bytes[i + j] & 0xFF;
                    out.append(String.format("%02X ", v));
                    ascii.append(v >= 32 && v <= 126 ? (char) v : '.');
                } else {
                    out.append("   ");
                    ascii.append(' ');
                }
            }
            out.append(" | ").append(ascii).append('\n');
        }
        if (bytes.length > maxBytes) out.append("... truncated ").append(bytes.length - maxBytes).append(" bytes\n");
        return out.toString();
    }
}
