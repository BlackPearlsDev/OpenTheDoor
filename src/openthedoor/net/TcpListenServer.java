package openthedoor.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class TcpListenServer {

    private final String host;
    private final int port;

    public TcpListenServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new TcpListenInitializer())
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(new InetSocketAddress(host, port)).sync();

            System.out.println("[BOOT] Listener started on " + host + ":" + port);
            System.out.println("[BOOT] Accepted protocols: HTTP / JSON / XMLSocket / text / binary / custom packets");
            System.out.println("[BOOT] Waiting for clients...");

            future.channel().closeFuture().sync();
        } finally {
            System.out.println("[BOOT] Shutting down listener...");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}