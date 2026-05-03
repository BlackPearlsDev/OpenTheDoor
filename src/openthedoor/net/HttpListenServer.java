package openthedoor.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class HttpListenServer {

    private final int port;

    public HttpListenServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpListenInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(port).sync();

            System.out.println("[BOOT] HTTP listener started on 0.0.0.0:" + port);
            System.out.println("[BOOT] Waiting for HTTP clients...");

            future.channel().closeFuture().sync();
        } finally {
            System.out.println("[BOOT] Shutting down HTTP listener...");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}