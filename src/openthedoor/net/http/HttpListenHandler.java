package openthedoor.net.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import openthedoor.config.ServerConfig;
import openthedoor.log.SessionLogger;
import openthedoor.log.TrafficDirection;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class HttpListenHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final ServerConfig config;
    private final SessionLogger logger;

    public HttpListenHandler(ServerConfig config, SessionLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("[HTTP] CLIENT CONNECTED: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("[HTTP] CLIENT DISCONNECTED: " + ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        byte[] requestBytes = buildRequestDump(request).getBytes(StandardCharsets.UTF_8);
        logger.logPacket(TrafficDirection.HTTP_REQUEST, ctx.channel().remoteAddress(), ctx.channel().localAddress(), requestBytes);

        MockResponse mock = findMockResponse(request);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(mock.status),
                Unpooled.copiedBuffer(mock.body)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mock.contentType);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        byte[] responseDump = ("HTTP/1.1 " + mock.status + "\nContent-Type: " + mock.contentType + "\n\n")
                .getBytes(StandardCharsets.UTF_8);
        logger.logPacket(TrafficDirection.HTTP_RESPONSE, ctx.channel().localAddress(), ctx.channel().remoteAddress(), responseDump);

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private String buildRequestDump(FullHttpRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.method().name()).append(' ').append(request.uri()).append(" HTTP/1.1\n");
        sb.append(request.headers()).append('\n');
        sb.append(request.content().toString(StandardCharsets.UTF_8));
        return sb.toString();
    }

    private MockResponse findMockResponse(FullHttpRequest request) throws Exception {
        String path = new URI(request.uri()).getPath();
        if (path == null || path.isEmpty() || path.equals("/")) path = "/index.json";
        path = path.replace("..", "");

        File file = new File(config.getMockDir(), path.startsWith("/") ? path.substring(1) : path);
        if (!file.exists() || !file.isFile()) {
            file = new File(config.getMockDir(), "default.json");
        }

        if (file.exists() && file.isFile()) {
            byte[] body = Files.readAllBytes(file.toPath());
            return new MockResponse(config.getDefaultHttpStatus(), contentTypeFor(file.getName()), body);
        }

        String fallback = "{\"status\":\"ok\",\"message\":\"request received\"}";
        return new MockResponse(config.getDefaultHttpStatus(), config.getDefaultHttpContentType(), fallback.getBytes(StandardCharsets.UTF_8));
    }

    private String contentTypeFor(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".xml")) return "application/xml; charset=UTF-8";
        if (lower.endsWith(".txt")) return "text/plain; charset=UTF-8";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        return config.getDefaultHttpContentType();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("[HTTP] EXCEPTION: " + cause.getClass().getSimpleName() + " -> " + cause.getMessage());
        ctx.close();
    }

    private static class MockResponse {
        final int status;
        final String contentType;
        final byte[] body;

        MockResponse(int status, String contentType, byte[] body) {
            this.status = status;
            this.contentType = contentType;
            this.body = body;
        }
    }
}
