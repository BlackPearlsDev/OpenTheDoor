package openthedoor.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfig {

    private String host;
    private int port;
    private String mode;

    public ServerConfig(String host, int port, String mode) {
        this.host = host;
        this.port = port;
        this.mode = mode;
    }

    public static ServerConfig load(String path) {
        Properties properties = new Properties();

        String host = "0.0.0.0";
        int port = 5555;
        String mode = "auto";

        try (FileInputStream input = new FileInputStream(path)) {
            properties.load(input);

            host = properties.getProperty("host", host).trim();
            port = Integer.parseInt(properties.getProperty("port", String.valueOf(port)).trim());
            mode = properties.getProperty("mode", mode).trim();

            System.out.println("[CONFIG] Loaded: " + path);
        } catch (IOException e) {
            System.out.println("[CONFIG] Config file not found, using defaults.");
        } catch (Exception e) {
            System.out.println("[CONFIG] Invalid config, using defaults.");
            e.printStackTrace();
        }

        return new ServerConfig(host, port, mode);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getMode() {
        return mode;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}