package openthedoor.net;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;

public class HttpListenHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("[HTTP] CLIENT CONNECTED: " + ctx.channel().remoteAddress());
        System.out.println("==================================================");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("[HTTP] CLIENT DISCONNECTED: " + ctx.channel().remoteAddress());
        System.out.println("==================================================");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String method = request.method().name();
        String uri = request.uri();
        String body = request.content().toString(StandardCharsets.UTF_8);

        System.out.println();
        System.out.println("==================================================");
        System.out.println("[<-- CLIENT] HTTP REQUEST RECEIVED");
        System.out.println("[HTTP] From: " + ctx.channel().remoteAddress());
        System.out.println("[HTTP] Method: " + method);
        System.out.println("[HTTP] URI: " + uri);
        System.out.println("--------------------------------------------------");
        System.out.println("[HEADERS]");
        System.out.println(request.headers());
        System.out.println("--------------------------------------------------");
        System.out.println("[BODY]");
        System.out.println(body);
        System.out.println("==================================================");

        String responseJson = "{"
                + "\"status\":\"ok\","
                + "\"message\":\"request received\""
                + "}";

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(responseJson, StandardCharsets.UTF_8)
        );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        boolean keepAlive = HttpUtil.isKeepAlive(request);

        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("[HTTP] EXCEPTION: " + cause.getClass().getSimpleName() + " -> " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}