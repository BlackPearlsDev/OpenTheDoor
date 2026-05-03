package openthedoor.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class TcpListenHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("[TCP] CLIENT CONNECTED");
        System.out.println("[TCP] Remote: " + ctx.channel().remoteAddress());
        System.out.println("[TCP] Local: " + ctx.channel().localAddress());
        System.out.println("==================================================");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("[TCP] CLIENT DISCONNECTED");
        System.out.println("[TCP] Remote: " + ctx.channel().remoteAddress());
        System.out.println("==================================================");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        int length = msg.readableBytes();

        byte[] bytes = new byte[length];
        msg.getBytes(msg.readerIndex(), bytes);

        String type = PacketTypeDetector.detect(bytes);
        String ascii = toPrintableAscii(bytes);
        String hex = ByteBufUtil.hexDump(msg);

        System.out.println();
        System.out.println("==================================================");
        System.out.println("[<-- CLIENT] PACKET RECEIVED");
        System.out.println("[TCP] From: " + ctx.channel().remoteAddress());
        System.out.println("[TCP] Size: " + length + " bytes");
        System.out.println("[TCP] Detected type: " + type);
        System.out.println("--------------------------------------------------");
        System.out.println("[ASCII / TEXT VIEW]");
        System.out.println(ascii);
        System.out.println("--------------------------------------------------");
        System.out.println("[HEX VIEW]");
        printHexPretty(hex);
        System.out.println("==================================================");

        // et plus tard si tu veux repondre tu le fait là du coup
        // ctx.writeAndFlush(Unpooled.copiedBuffer("RESPONSE", StandardCharsets.UTF_8));
    }

    private String toPrintableAscii(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            int value = b & 0xFF;

            if (value >= 32 && value <= 126) {
                sb.append((char) value);
            } else if (value == '\n') {
                sb.append("\\n");
            } else if (value == '\r') {
                sb.append("\\r");
            } else if (value == '\t') {
                sb.append("\\t");
            } else if (value == 0) {
                sb.append("\\0");
            } else {
                sb.append('.');
            }
        }

        return sb.toString();
    }

    private void printHexPretty(String hex) {
        StringBuilder line = new StringBuilder();

        for (int i = 0; i < hex.length(); i += 2) {
            line.append(hex, i, i + 2).append(' ');

            if (((i / 2) + 1) % 16 == 0) {
                System.out.println(line);
                line.setLength(0);
            }
        }

        if (line.length() > 0) {
            System.out.println(line);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("[TCP] EXCEPTION: " + cause.getClass().getSimpleName() + " -> " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}