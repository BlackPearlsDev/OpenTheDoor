package openthedoor.net.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import openthedoor.config.ServerConfig;
import openthedoor.log.SessionLogger;

public class TcpProxyInitializer extends ChannelInitializer<SocketChannel> {
    private final ServerConfig config;
    private final SessionLogger logger;
    private final EventLoopGroup workerGroup;

    public TcpProxyInitializer(ServerConfig config, SessionLogger logger, EventLoopGroup workerGroup) {
        this.config = config;
        this.logger = logger;
        this.workerGroup = workerGroup;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new TcpProxyFrontendHandler(config, logger, workerGroup));
    }
}
