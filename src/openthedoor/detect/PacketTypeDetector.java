package openthedoor.detect;

import java.nio.charset.StandardCharsets;

public final class PacketTypeDetector {
    private PacketTypeDetector() {}

    public static ProtocolGuess detect(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return ProtocolGuess.EMPTY;
        String text = new String(bytes, StandardCharsets.UTF_8).trim();

        if (isTls(bytes)) return ProtocolGuess.TLS_HANDSHAKE;
        if (isHttp(text)) {
            if (text.toLowerCase().contains("upgrade: websocket")) return ProtocolGuess.WEBSOCKET_HANDSHAKE;
            return ProtocolGuess.HTTP;
        }
        if (isJson(text)) return ProtocolGuess.JSON;
        if (isXmlSocket(bytes, text)) return ProtocolGuess.XMLSOCKET;
        if (isXml(text)) return ProtocolGuess.XML;
        if (isMqtt(bytes, text)) return ProtocolGuess.MQTT;
        if (isAmf(bytes)) return ProtocolGuess.AMF;
        if (isGzip(bytes)) return ProtocolGuess.GZIP;
        if (isZlib(bytes)) return ProtocolGuess.ZLIB;
        if (looksLengthPrefixed(bytes)) return ProtocolGuess.LENGTH_PREFIXED;
        if (isNullTerminatedText(bytes)) return ProtocolGuess.NULL_TERMINATED_TEXT;
        if (isMostlyText(bytes)) return ProtocolGuess.TEXT;
        return ProtocolGuess.BINARY;
    }

    public static String detectName(byte[] bytes) {
        return detect(bytes).name();
    }

    private static boolean isTls(byte[] b) {
        return b.length >= 3 && (b[0] & 0xFF) == 0x16 && (b[1] & 0xFF) == 0x03;
    }

    private static boolean isHttp(String text) {
        return text.startsWith("GET ") || text.startsWith("POST ") || text.startsWith("PUT ")
                || text.startsWith("DELETE ") || text.startsWith("PATCH ") || text.startsWith("OPTIONS ")
                || text.startsWith("HEAD ") || text.contains("HTTP/1.1") || text.contains("HTTP/1.0");
    }

    private static boolean isJson(String text) {
        return text.length() >= 2 && ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]")));
    }

    private static boolean isXml(String text) {
        return text.startsWith("<") && text.endsWith(">");
    }

    private static boolean isXmlSocket(byte[] bytes, String text) {
        return bytes.length > 0 && bytes[bytes.length - 1] == 0 && text.startsWith("<");
    }

    private static boolean isMqtt(byte[] bytes, String text) {
        if (text.contains("MQTT") || text.contains("MQIsdp")) return true;
        return bytes.length >= 2 && (bytes[0] & 0xF0) == 0x10;
    }

    private static boolean isAmf(byte[] bytes) {
        if (bytes.length < 1) return false;
        int first = bytes[0] & 0xFF;
        return first == 0x00 || first == 0x03 || first == 0x11;
    }

    private static boolean isGzip(byte[] bytes) {
        return bytes.length >= 2 && (bytes[0] & 0xFF) == 0x1F && (bytes[1] & 0xFF) == 0x8B;
    }

    private static boolean isZlib(byte[] bytes) {
        if (bytes.length < 2) return false;
        int cmf = bytes[0] & 0xFF;
        int flg = bytes[1] & 0xFF;
        return cmf == 0x78 && ((cmf << 8) + flg) % 31 == 0;
    }

    private static boolean looksLengthPrefixed(byte[] bytes) {
        if (bytes.length < 5) return false;
        int len = ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        if (len == bytes.length - 4) return true;
        int shortLen = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
        return shortLen == bytes.length - 2;
    }

    private static boolean isNullTerminatedText(byte[] bytes) {
        return bytes.length > 1 && bytes[bytes.length - 1] == 0 && isMostlyText(bytes);
    }

    private static boolean isMostlyText(byte[] bytes) {
        int printable = 0;
        for (byte b : bytes) {
            int v = b & 0xFF;
            if (v == 0) continue;
            if (v == '\n' || v == '\r' || v == '\t' || (v >= 32 && v <= 126)) printable++;
        }
        return ((double) printable / Math.max(1, bytes.length)) >= 0.85;
    }
}
