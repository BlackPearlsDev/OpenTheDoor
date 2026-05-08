package openthedoor.log;

import openthedoor.config.ServerConfig;
import openthedoor.detect.PacketTypeDetector;
import openthedoor.detect.PayloadFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionLogger {
    private final ServerConfig config;
    private final File sessionDir;
    private final AtomicInteger counter = new AtomicInteger();

    public SessionLogger(ServerConfig config, String name) {
        this.config = config;
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        this.sessionDir = new File(config.getLogDir(), name + "-" + timestamp);
        if (!sessionDir.exists() && !sessionDir.mkdirs()) {
            System.err.println("[LOG] Could not create log directory: " + sessionDir.getAbsolutePath());
        }
    }

    public synchronized void logPacket(TrafficDirection direction, Object remote, Object local, byte[] bytes) {
        int id = counter.incrementAndGet();
        String type = PacketTypeDetector.detectName(bytes);
        String baseName = String.format("%04d-%s", id, direction.name().toLowerCase());

        printConsole(direction, remote, local, bytes, type);

        if (!config.isSavePackets()) return;

        try {
            writeBinary(baseName + ".bin", bytes);
            writeText(baseName + ".txt", direction, remote, local, bytes, type);
            appendSummary(id, direction, remote, local, bytes, type);
        } catch (IOException e) {
            System.err.println("[LOG] Failed to write packet log: " + e.getMessage());
        }
    }

    private void printConsole(TrafficDirection direction, Object remote, Object local, byte[] bytes, String type) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("[" + direction + "] PACKET");
        System.out.println("Remote: " + remote);
        System.out.println("Local: " + local);
        System.out.println("Size: " + bytes.length + " bytes");
        System.out.println("Detected: " + type);
        System.out.println("--------------------------------------------------");
        System.out.println("[ASCII / TEXT]");
        System.out.println(PayloadFormatter.printableAscii(bytes, config.getMaxPrintableBytes()));
        System.out.println("--------------------------------------------------");
        System.out.println("[HEX]");
        System.out.print(PayloadFormatter.hex(bytes, config.getMaxPrintableBytes()));
        System.out.println("==================================================");
    }

    private void writeBinary(String fileName, byte[] bytes) throws IOException {
        try (FileOutputStream out = new FileOutputStream(new File(sessionDir, fileName))) {
            out.write(bytes);
        }
    }

    private void writeText(String fileName, TrafficDirection direction, Object remote, Object local, byte[] bytes, String type) throws IOException {
        try (FileWriter writer = new FileWriter(new File(sessionDir, fileName))) {
            writer.write("Timestamp: " + new Date() + "\n");
            writer.write("Direction: " + direction + "\n");
            writer.write("Remote: " + remote + "\n");
            writer.write("Local: " + local + "\n");
            writer.write("Size: " + bytes.length + " bytes\n");
            writer.write("Detected: " + type + "\n\n");
            writer.write("[ASCII / TEXT]\n");
            writer.write(PayloadFormatter.printableAscii(bytes, config.getMaxPrintableBytes()));
            writer.write("\n\n[HEX]\n");
            writer.write(PayloadFormatter.hex(bytes, config.getMaxPrintableBytes()));
        }
    }

    private void appendSummary(int id, TrafficDirection direction, Object remote, Object local, byte[] bytes, String type) throws IOException {
        try (FileWriter writer = new FileWriter(new File(sessionDir, "summary.md"), true)) {
            writer.write("## Packet " + id + "\n\n");
            writer.write("- Time: " + new Date() + "\n");
            writer.write("- Direction: " + direction + "\n");
            writer.write("- Remote: `" + remote + "`\n");
            writer.write("- Local: `" + local + "`\n");
            writer.write("- Size: " + bytes.length + " bytes\n");
            writer.write("- Detected: " + type + "\n\n");
        }
    }
}
