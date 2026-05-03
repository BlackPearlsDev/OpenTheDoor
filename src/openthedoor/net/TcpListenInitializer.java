package openthedoor.net;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class TcpListenInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new TcpListenHandler());
    }
}