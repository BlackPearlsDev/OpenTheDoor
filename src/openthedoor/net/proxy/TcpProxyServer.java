package openthedoor.net.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import openthedoor.config.ServerConfig;
import openthedoor.log.SessionLogger;

import java.net.InetSocketAddress;

public class TcpProxyServer {
    private final ServerConfig config;

    public TcpProxyServer(ServerConfig config) {
        this.config = config;
    }

    public void start() throws InterruptedException {
        if (config.getTargetHost() == null || config.getTargetHost().trim().isEmpty()) {
            throw new IllegalArgumentException("targetHost is required for tcp-proxy mode");
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        SessionLogger logger = new SessionLogger(config, "tcp-proxy");
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new TcpProxyInitializer(config, logger, workerGroup))
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.AUTO_READ, false)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(new InetSocketAddress(config.getHost(), config.getPort())).sync();
            System.out.println("[BOOT] TCP proxy listening on " + config.getHost() + ":" + config.getPort());
            System.out.println("[BOOT] Forwarding to " + config.getTargetHost() + ":" + config.getTargetPort());
            future.channel().closeFuture().sync();
        } finally {
            System.out.println("[BOOT] Shutting down TCP proxy...");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
