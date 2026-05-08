package openthedoor.net.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import openthedoor.config.ServerConfig;
import openthedoor.log.SessionLogger;

public class HttpListenInitializer extends ChannelInitializer<SocketChannel> {
    private final ServerConfig config;
    private final SessionLogger logger;

    public HttpListenInitializer(ServerConfig config, SessionLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(10 * 1024 * 1024));
        pipeline.addLast(new HttpListenHandler(config, logger));
    }
}
