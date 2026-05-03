package openthedoor.net;

import java.nio.charset.StandardCharsets;

public class PacketTypeDetector {

    public static String detect(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "EMPTY";
        }

        String text = new String(bytes, StandardCharsets.UTF_8).trim();

        if (isHttp(text)) {
            return "HTTP";
        }

        if (isJson(text)) {
            return "JSON";
        }

        if (isXmlSocket(bytes, text)) {
            return "XMLSocket";
        }

        if (isXml(text)) {
            return "XML";
        }

        if (isMostlyText(bytes)) {
            return "TEXT / CUSTOM ASCII";
        }

        return "BINARY / PROPRIETARY";
    }

    private static boolean isHttp(String text) {
        return text.startsWith("GET ")
                || text.startsWith("POST ")
                || text.startsWith("PUT ")
                || text.startsWith("DELETE ")
                || text.startsWith("PATCH ")
                || text.startsWith("OPTIONS ")
                || text.startsWith("HEAD ")
                || text.contains("HTTP/1.1")
                || text.contains("HTTP/1.0");
    }

    private static boolean isJson(String text) {
        if (text.length() < 2) {
            return false;
        }

        return (text.startsWith("{") && text.endsWith("}"))
                || (text.startsWith("[") && text.endsWith("]"));
    }

    private static boolean isXml(String text) {
        return text.startsWith("<") && text.endsWith(">");
    }

    private static boolean isXmlSocket(byte[] bytes, String text) {
        // Les clients Flash XMLSocket terminent souvent les messages avec \0.
        boolean endsWithNullByte = bytes[bytes.length - 1] == 0;

        return endsWithNullByte && text.startsWith("<");
    }

    private static boolean isMostlyText(byte[] bytes) {
        int printable = 0;
        int total = bytes.length;

        for (byte b : bytes) {
            int value = b & 0xFF;

            if (value == 0) {
                continue;
            }

            if (value == '\n'
                    || value == '\r'
                    || value == '\t'
                    || value >= 32 && value <= 126) {
                printable++;
            }
        }

        double ratio = (double) printable / (double) total;

        return ratio >= 0.85;
    }
}