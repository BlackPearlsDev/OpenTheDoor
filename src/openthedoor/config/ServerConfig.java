package openthedoor.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfig {
    private String host = "0.0.0.0";
    private int port = 8080;
    private String mode = "auto";

    private String targetHost = "";
    private int targetPort = 80;

    private String mockDir = "mocks";
    private int defaultHttpStatus = 200;
    private String defaultHttpContentType = "application/json; charset=UTF-8";

    private String logDir = "logs";
    private boolean savePackets = true;
    private int maxPrintableBytes = 4096;

    private String apkPath = "game.apk";
    private String scanOutput = "reports/apk-scan-report.md";

    public static ServerConfig load(String path) {
        ServerConfig config = new ServerConfig();
        Properties p = new Properties();
        try (FileInputStream input = new FileInputStream(path)) {
            p.load(input);
            config.host = readString(p, "host", config.host);
            config.port = readInt(p, "port", config.port);
            config.mode = readString(p, "mode", config.mode);
            config.targetHost = readString(p, "targetHost", config.targetHost);
            config.targetPort = readInt(p, "targetPort", config.targetPort);
            config.mockDir = readString(p, "mockDir", config.mockDir);
            config.defaultHttpStatus = readInt(p, "defaultHttpStatus", config.defaultHttpStatus);
            config.defaultHttpContentType = readString(p, "defaultHttpContentType", config.defaultHttpContentType);
            config.logDir = readString(p, "logDir", config.logDir);
            config.savePackets = readBoolean(p, "savePackets", config.savePackets);
            config.maxPrintableBytes = readInt(p, "maxPrintableBytes", config.maxPrintableBytes);
            config.apkPath = readString(p, "apkPath", config.apkPath);
            config.scanOutput = readString(p, "scanOutput", config.scanOutput);
            System.out.println("[CONFIG] Loaded: " + path);
        } catch (IOException e) {
            System.out.println("[CONFIG] Config file not found, using defaults: " + path);
        } catch (Exception e) {
            System.out.println("[CONFIG] Invalid config, using defaults where needed.");
            e.printStackTrace();
        }
        return config;
    }

    private static String readString(Properties p, String key, String def) {
        return p.getProperty(key, def).trim();
    }

    private static int readInt(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim()); }
        catch (Exception e) { return def; }
    }

    private static boolean readBoolean(Properties p, String key, boolean def) {
        return Boolean.parseBoolean(p.getProperty(key, String.valueOf(def)).trim());
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getMode() { return mode; }
    public String getTargetHost() { return targetHost; }
    public int getTargetPort() { return targetPort; }
    public String getMockDir() { return mockDir; }
    public int getDefaultHttpStatus() { return defaultHttpStatus; }
    public String getDefaultHttpContentType() { return defaultHttpContentType; }
    public String getLogDir() { return logDir; }
    public boolean isSavePackets() { return savePackets; }
    public int getMaxPrintableBytes() { return maxPrintableBytes; }
    public String getApkPath() { return apkPath; }
    public String getScanOutput() { return scanOutput; }

    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }
    public void setMode(String mode) { this.mode = mode; }
}
