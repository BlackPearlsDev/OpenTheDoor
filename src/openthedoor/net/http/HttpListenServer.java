package openthedoor.net.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import openthedoor.config.ServerConfig;
import openthedoor.log.SessionLogger;

import java.net.InetSocketAddress;

public class HttpListenServer {
    private final ServerConfig config;

    public HttpListenServer(ServerConfig config) {
        this.config = config;
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        SessionLogger logger = new SessionLogger(config, "http-listen");
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpListenInitializer(config, logger))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(new InetSocketAddress(config.getHost(), config.getPort())).sync();
            System.out.println("[BOOT] HTTP listener started on " + config.getHost() + ":" + config.getPort());
            System.out.println("[BOOT] Mock directory: " + config.getMockDir());
            System.out.println("[BOOT] Waiting for HTTP clients...");
            future.channel().closeFuture().sync();
        } finally {
            System.out.println("[BOOT] Shutting down HTTP listener...");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
