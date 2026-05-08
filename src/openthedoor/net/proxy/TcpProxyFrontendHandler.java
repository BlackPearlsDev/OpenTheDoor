package openthedoor.net.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import openthedoor.config.ServerConfig;
import openthedoor.log.SessionLogger;
import openthedoor.log.TrafficDirection;

public class TcpProxyFrontendHandler extends ChannelInboundHandlerAdapter {
    private final ServerConfig config;
    private final SessionLogger logger;
    private final EventLoopGroup workerGroup;
    private Channel outboundChannel;

    public TcpProxyFrontendHandler(ServerConfig config, SessionLogger logger, EventLoopGroup workerGroup) {
        this.config = config;
        this.logger = logger;
        this.workerGroup = workerGroup;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        final Channel inboundChannel = ctx.channel();
        System.out.println("[PROXY] Client connected: " + inboundChannel.remoteAddress());

        Bootstrap b = new Bootstrap();
        b.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new TcpProxyBackendHandler(inboundChannel, logger));

        ChannelFuture f = b.connect(config.getTargetHost(), config.getTargetPort());
        outboundChannel = f.channel();
        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("[PROXY] Connected to target: " + config.getTargetHost() + ":" + config.getTargetPort());
                inboundChannel.read();
            } else {
                System.out.println("[PROXY] Target connection failed: " + future.cause().getMessage());
                inboundChannel.close();
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        if (outboundChannel == null || !outboundChannel.isActive()) {
            ((ByteBuf) msg).release();
            return;
        }

        ByteBuf buf = (ByteBuf) msg;
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        logger.logPacket(TrafficDirection.CLIENT_TO_SERVER, ctx.channel().remoteAddress(), outboundChannel.remoteAddress(), bytes);

        outboundChannel.writeAndFlush(buf).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) ctx.channel().read();
            else future.channel().close();
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("[PROXY] Client disconnected: " + ctx.channel().remoteAddress());
        closeOnFlush(outboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("[PROXY] Frontend exception: " + cause.getMessage());
        closeOnFlush(ctx.channel());
    }

    static void closeOnFlush(Channel ch) {
        if (ch != null && ch.isActive()) ch.writeAndFlush(io.netty.buffer.Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
