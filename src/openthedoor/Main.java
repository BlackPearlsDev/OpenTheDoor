package openthedoor;

import openthedoor.config.ServerConfig;
import openthedoor.net.http.HttpListenServer;
import openthedoor.net.proxy.TcpProxyServer;
import openthedoor.net.tcp.TcpListenServer;
import openthedoor.scan.ApkScanner;
import openthedoor.scan.ScanResult;
import openthedoor.ui.DesktopUi;

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                DesktopUi.launch("config.properties");
                return;
            }

            boolean cli = "--cli".equalsIgnoreCase(args[0]);
            if ("--ui".equalsIgnoreCase(args[0])) {
                String uiConfigPath = args.length >= 2 ? args[1] : "config.properties";
                DesktopUi.launch(uiConfigPath);
                return;
            }

            String configPath = cli
                    ? (args.length >= 2 ? args[1] : "config.properties")
                    : args[0];
            ServerConfig config = ServerConfig.load(configPath);

            int overrideOffset = cli ? 2 : 1;
            if (args.length >= overrideOffset + 1) config.setHost(args[overrideOffset]);
            if (args.length >= overrideOffset + 2) config.setPort(Integer.parseInt(args[overrideOffset + 1]));

            printBoot(config);

            String mode = config.getMode().toLowerCase().trim();
            switch (mode) {
                case "ui":
                case "desktop":
                    DesktopUi.launch(configPath);
                    break;

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
