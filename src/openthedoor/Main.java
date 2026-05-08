package openthedoor;

import openthedoor.config.ServerConfig;
import openthedoor.net.http.HttpListenServer;
import openthedoor.net.proxy.TcpProxyServer;
import openthedoor.net.tcp.TcpListenServer;
import openthedoor.scan.ApkScanner;
import openthedoor.scan.ScanResult;

public class Main {
    public static void main(String[] args) {
        try {
            String configPath = args.length >= 1 ? args[0] : "config.properties";
            ServerConfig config = ServerConfig.load(configPath);

            if (args.length >= 2) config.setHost(args[1]);
            if (args.length >= 3) config.setPort(Integer.parseInt(args[2]));

            printBoot(config);

            String mode = config.getMode().toLowerCase().trim();
            switch (mode) {
                case "tcp":
                case "listen":
                case "tcp-listen":
                case "auto":
                    new TcpListenServer(config).start();
                    break;

                case "http":
                case "http-listen":
                    new HttpListenServer(config).start();
                    break;

                case "proxy":
                case "tcp-proxy":
                    new TcpProxyServer(config).start();
                    break;

                case "apk":
                case "apk-scan":
                    ScanResult result = new ApkScanner(config.getApkPath()).scan();
                    result.writeMarkdown(config.getScanOutput());
                    System.out.println(result.toConsoleString());
                    System.out.println("[SCAN] Report written to: " + config.getScanOutput());
                    break;

                default:
                    throw new IllegalArgumentException("Unknown mode: " + config.getMode());
            }
        } catch (Exception e) {
            System.err.println("[BOOT] Fatal error:");
            e.printStackTrace();
        }
    }

    private static void printBoot(ServerConfig config) {
        System.out.println("==================================================");
        System.out.println("[BOOT] OpenTheDoor");
        System.out.println("[BOOT] Host: " + config.getHost());
        System.out.println("[BOOT] Port: " + config.getPort());
        System.out.println("[BOOT] Mode: " + config.getMode());
        if (config.getTargetHost() != null && !config.getTargetHost().isEmpty()) {
            System.out.println("[BOOT] Target: " + config.getTargetHost() + ":" + config.getTargetPort());
        }
        System.out.println("==================================================");
    }
}
