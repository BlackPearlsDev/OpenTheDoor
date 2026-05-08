package openthedoor.net.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import openthedoor.config.ServerConfig;
import openthedoor.log.SessionLogger;
import openthedoor.log.TrafficDirection;

public class TcpListenHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final ServerConfig config;
    private final SessionLogger logger;

    public TcpListenHandler(ServerConfig config, SessionLogger logger) {
        this.config = config;
        this.logger = logger;
    }

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
        byte[] bytes = new byte[msg.readableBytes()];
        msg.getBytes(msg.readerIndex(), bytes);
        logger.logPacket(TrafficDirection.CLIENT_TO_LISTENER, ctx.channel().remoteAddress(), ctx.channel().localAddress(), bytes);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("[TCP] EXCEPTION: " + cause.getClass().getSimpleName() + " -> " + cause.getMessage());
        ctx.close();
    }
}
