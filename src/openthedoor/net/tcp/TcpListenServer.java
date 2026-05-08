package openthedoor.net.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import openthedoor.config.ServerConfig;
import openthedoor.log.SessionLogger;

import java.net.InetSocketAddress;

public class TcpListenServer {
    private final ServerConfig config;

    public TcpListenServer(ServerConfig config) {
        this.config = config;
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        SessionLogger logger = new SessionLogger(config, "tcp-listen");
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new TcpListenInitializer(config, logger))
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(new InetSocketAddress(config.getHost(), config.getPort())).sync();
            System.out.println("[BOOT] TCP listener started on " + config.getHost() + ":" + config.getPort());
            System.out.println("[BOOT] Accepted data: HTTP / JSON / XMLSocket / text / binary / custom packets");
            System.out.println("[BOOT] Waiting for clients...");
            future.channel().closeFuture().sync();
        } finally {
            System.out.println("[BOOT] Shutting down TCP listener...");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
