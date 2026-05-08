package openthedoor.net.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import openthedoor.log.SessionLogger;
import openthedoor.log.TrafficDirection;

public class TcpProxyBackendHandler extends ChannelInboundHandlerAdapter {
    private final Channel inboundChannel;
    private final SessionLogger logger;

    public TcpProxyBackendHandler(Channel inboundChannel, SessionLogger logger) {
        this.inboundChannel = inboundChannel;
        this.logger = logger;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        logger.logPacket(TrafficDirection.SERVER_TO_CLIENT, ctx.channel().remoteAddress(), inboundChannel.remoteAddress(), bytes);

        inboundChannel.writeAndFlush(buf).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) ctx.channel().read();
            else future.channel().close();
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        TcpProxyFrontendHandler.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("[PROXY] Backend exception: " + cause.getMessage());
        TcpProxyFrontendHandler.closeOnFlush(ctx.channel());
    }
}
