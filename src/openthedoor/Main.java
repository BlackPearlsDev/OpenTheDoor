package openthedoor;

import openthedoor.config.ServerConfig;
import openthedoor.net.TcpListenServer;

public class Main {

    public static void main(String[] args) {
        try {
            ServerConfig config = ServerConfig.load("config.properties");

            if (args.length >= 1) {
                config.setHost(args[0]);
            }

            if (args.length >= 2) {
                config.setPort(Integer.parseInt(args[1]));
            }

            System.out.println("==================================================");
            System.out.println("[BOOT] OpenTheDoor listener");
            System.out.println("[BOOT] Host: " + config.getHost());
            System.out.println("[BOOT] Port: " + config.getPort());
            System.out.println("[BOOT] Mode: " + config.getMode());
            System.out.println("==================================================");

            new TcpListenServer(config.getHost(), config.getPort()).start();

        } catch (Exception e) {
            System.err.println("[BOOT] Fatal error:");
            e.printStackTrace();
        }
    }
}