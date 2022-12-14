package org.xbib.net.http.netty.client.secure;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2SettingsFrame;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2ConnectionPrefaceAndSettingsFrameWrittenEvent;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AttributeKey;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.Test;

class WebtideTest {

    private static final Logger logger = Logger.getLogger(WebtideTest.class.getName());

    /**
     * Netty standalone demo to connect to <a href="https://webtide.com">https://webtide.com</a>
     * and negotiate HTTP/2 and receive responses as HTTP objects.
     */
    @Test
    void testWebtideHttps() throws Exception {
        try (Client client = new Client()) {
            InetSocketAddress address = new InetSocketAddress("google.com", 443);
            Http2Transport transport = new Http2Transport(client.bootstrap, address);
            transport.onResponse(string -> logger.log(Level.INFO, "got response for request = " + string));
            logger.log(Level.FINE, "connected");
            transport.connect();
            logger.log(Level.FINE, "waiting for settings");
            transport.awaitSettings();
            sendRequest(transport);
            logger.log(Level.FINE, "waiting for responses");
            transport.awaitResponses();
            logger.log(Level.FINE, "close");
            transport.close();
        }
    }

    private void sendRequest(Http2Transport transport) {
        Channel channel = transport.channel();
        if (channel == null) {
            return;
        }
        Integer streamId = transport.nextStream();
        String host = transport.inetSocketAddress().getHostString();
        int port = transport.inetSocketAddress().getPort();
        String uri = "https://" + host + ":" + port;
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        request.headers().add(HttpHeaderNames.HOST, host + ":" + port);
        request.headers().add(HttpHeaderNames.USER_AGENT, "Java");
        if (streamId != null) {
            request.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), Integer.toString(streamId));
        }
        logger.log(Level.FINE, "request prepared and ready for sending " + request);
        channel.writeAndFlush(request);
    }

    private final AttributeKey<Http2Transport> TRANSPORT_ATTRIBUTE_KEY = AttributeKey.valueOf("transport");

    interface ResponseWriter {
        void write(String string);
    }

    class Client implements Closeable {

        private final EventLoopGroup eventLoopGroup;

        private final Bootstrap bootstrap;

        Client() {
            eventLoopGroup = new NioEventLoopGroup();
            Initializer initializer = new Initializer();
            bootstrap = new Bootstrap()
                    .group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(initializer);
        }

        @Override
        public void close() throws IOException {
            eventLoopGroup.shutdownGracefully();
        }
    }

    class Http2Transport {

        private final Bootstrap bootstrap;

        private final InetSocketAddress inetSocketAddress;

        private final SortedMap<Integer, CompletableFuture<Boolean>> streamidPromiseMap;

        private final AtomicInteger streamIdCounter;

        private final CompletableFuture<Boolean> settingsPromise;

        private Channel channel;

        private ResponseWriter responseWriter;

        Http2Transport(Bootstrap bootstrap, InetSocketAddress inetSocketAddress) {
            this.bootstrap = bootstrap;
            this.inetSocketAddress = inetSocketAddress;
            this.streamidPromiseMap = new TreeMap<>();
            this.streamIdCounter = new AtomicInteger(3);
            this.settingsPromise = new CompletableFuture<>();
        }

        InetSocketAddress inetSocketAddress() {
            return inetSocketAddress;
        }

        void connect() throws InterruptedException {
            channel = bootstrap.connect(inetSocketAddress).sync().await().channel();
            channel.attr(TRANSPORT_ATTRIBUTE_KEY).set(this);
        }

        Channel channel() {
            return channel;
        }

        Integer nextStream() {
            Integer streamId = streamIdCounter.getAndAdd(2);
            streamidPromiseMap.put(streamId, new CompletableFuture<>());
            return streamId;
        }

        void onResponse(ResponseWriter responseWriter) {
            this.responseWriter = responseWriter;
        }

        void settingsReceived(Channel channel, Http2Settings http2Settings) {
            settingsPromise.complete(true);
        }

        void awaitSettings() {
            try {
                settingsPromise.get(5, TimeUnit.SECONDS);
                logger.log(Level.INFO, "settings received");
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                settingsPromise.completeExceptionally(e);
            }
        }

        void responseReceived(Integer streamId, String message) {
            if (streamId == null) {
                logger.log(Level.WARNING, "unexpected message received: " + message);
                return;
            }
            CompletableFuture<Boolean> promise = streamidPromiseMap.get(streamId);
            if (promise == null) {
                logger.log(Level.WARNING, "message received for unknown stream id " + streamId);
            } else {
                if (responseWriter != null) {
                    responseWriter.write(message);
                }
                promise.complete(true);
            }
        }

        void awaitResponse(Integer streamId) {
            if (streamId == null) {
                return;
            }
            CompletableFuture<Boolean> promise = streamidPromiseMap.get(streamId);
            if (promise != null) {
                try {
                    logger.log(Level.INFO, "waiting for response for stream id=" + streamId);
                    promise.get(5, TimeUnit.SECONDS);
                    logger.log(Level.INFO, "response for stream id=" + streamId + " received");
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    logger.log(Level.WARNING, "streamId=" + streamId + " " + e.getMessage(), e);
                } finally {
                    streamidPromiseMap.remove(streamId);
                }
            }
        }

        void awaitResponses() {
            logger.log(Level.INFO, "waiting for all stream ids " + streamidPromiseMap.keySet());
            for (int streamId : streamidPromiseMap.keySet()) {
                awaitResponse(streamId);
            }
        }

        void fail(Throwable throwable) {
            for (CompletableFuture<Boolean> promise : streamidPromiseMap.values()) {
                promise.completeExceptionally(throwable);
            }
        }

        void close() {
            if (channel != null) {
                channel.close();
            }
        }
    }

    class Initializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) {

            try {
                SslContext sslContext = SslContextBuilder.forClient()
                        .sslProvider(SslProvider.JDK)
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                        .applicationProtocolConfig(new ApplicationProtocolConfig(
                                ApplicationProtocolConfig.Protocol.ALPN,
                                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                ApplicationProtocolNames.HTTP_2))
                        .build();
                ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
                ApplicationProtocolNegotiationHandler negotiationHandler = new ApplicationProtocolNegotiationHandler("") {
                    @Override
                    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
                        logger.log(Level.INFO, "ALPN negotiated protocol = " + protocol);
                        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                            DefaultHttp2Connection http2Connection = new DefaultHttp2Connection(false);
                            Http2FrameLogger frameLogger = new Http2FrameLogger(LogLevel.INFO, "client");
                            Http2ConnectionHandler http2ConnectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
                                    .connection(http2Connection)
                                    .frameLogger(frameLogger)
                                    .frameListener(new DelegatingDecompressorFrameListener(http2Connection,
                                            new InboundHttp2ToHttpAdapterBuilder(http2Connection)
                                                    .maxContentLength(10 * 1024 * 1024)
                                                    .propagateSettings(true)
                                                    .build()))
                                    .build();
                            Http2SettingsHandler http2SettingsHandler = new Http2SettingsHandler();
                            Http2ResponseHandler http2ResponseHandler = new Http2ResponseHandler();
                            Http2ResponseMessages http2ResponseMessages = new Http2ResponseMessages();
                            ctx.pipeline().addLast(http2ConnectionHandler, http2SettingsHandler,
                                    http2ResponseHandler, http2ResponseMessages);
                            logger.log(Level.INFO, "HTTP/2 pipeline set up = " + ctx.channel().pipeline().names());
                            return;
                        }
                        ctx.close();
                        throw new IllegalStateException("unknown protocol: " + protocol);
                    }
                };
                ch.pipeline().addLast(negotiationHandler);
            } catch (SSLException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    class Http2SettingsHandler extends SimpleChannelInboundHandler<Http2Settings> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http2Settings http2Settings) {
            logger.log(Level.INFO, "got settings = " + http2Settings);
            Http2Transport transport = ctx.channel().attr(TRANSPORT_ATTRIBUTE_KEY).get();
            transport.settingsReceived(ctx.channel(), http2Settings);
            ctx.pipeline().remove(this);
        }
    }

    class Http2ResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
            logger.log(Level.INFO, "got full http response = " + msg);
            Http2Transport transport = ctx.channel().attr(TRANSPORT_ATTRIBUTE_KEY).get();
            Integer streamId = msg.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
            if (msg.content().isReadable()) {
                transport.responseReceived(streamId, msg.content().toString(StandardCharsets.UTF_8));
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx)  {
            logger.log(Level.INFO, "channel read complete");
            // do nothing
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx)  {
            logger.log(Level.INFO, "channel inactive");
            ctx.fireChannelInactive();
            Http2Transport transport = ctx.channel().attr(TRANSPORT_ATTRIBUTE_KEY).get();
            transport.fail(new IOException("channel closed"));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.log(Level.SEVERE, cause.getMessage(), cause);
            Http2Transport transport = ctx.channel().attr(TRANSPORT_ATTRIBUTE_KEY).get();
            transport.fail(cause);
            ctx.channel().close();
        }
    }


    class Http2ResponseMessages extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            logger.log(Level.FINEST, "received msg = " + msg.getClass().getName());
            if (msg instanceof DefaultHttp2SettingsFrame) {
                DefaultHttp2SettingsFrame settingsFrame = (DefaultHttp2SettingsFrame) msg;
                logger.log(Level.FINEST, "received settings ");
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            logger.log(Level.FINEST, "received event = " + evt.getClass().getName());
            if (evt instanceof Http2ConnectionPrefaceAndSettingsFrameWrittenEvent) {
                Http2ConnectionPrefaceAndSettingsFrameWrittenEvent event =
                        (Http2ConnectionPrefaceAndSettingsFrameWrittenEvent) evt;
                logger.log(Level.FINEST, "received preface and setting written event " + event);
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.log(Level.FINEST, "received exception " + cause);
        }
    }

}
