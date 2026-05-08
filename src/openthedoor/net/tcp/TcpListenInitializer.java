package openthedoor.net.tcp;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import openthedoor.config.ServerConfig;
import openthedoor.log.SessionLogger;

public class TcpListenInitializer extends ChannelInitializer<SocketChannel> {
    private final ServerConfig config;
    private final SessionLogger logger;

    public TcpListenInitializer(ServerConfig config, SessionLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new TcpListenHandler(config, logger));
    }
}
