package org.xbib.net.http.nio.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbib.net.NetworkClass;
import org.xbib.net.http.HttpAddress;
import org.xbib.net.http.HttpHeaderNames;
import org.xbib.net.http.HttpHeaderValues;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.BaseApplication;
import org.xbib.net.http.server.BaseHttpDomain;
import org.xbib.net.http.server.BaseHttpService;
import org.xbib.net.http.server.route.BaseHttpRouter;
import org.xbib.net.http.server.HttpServerConfig;
import org.xbib.net.http.server.nio.NioHttpServer;

import java.net.BindException;
import java.nio.charset.StandardCharsets;

public class NioHttpServerTest {

    @Disabled
    @Test
    public void nioServerTest() throws Exception {
        HttpAddress httpAddress1 = HttpAddress.http1("localhost", 8008);
        HttpAddress httpAddress2 = HttpAddress.http1("localhost", 8009);
        NioHttpServer server = NioHttpServer.builder()
                .setHttpServerConfig(new HttpServerConfig()
                        .setServerName("NioHttpServer", NioHttpServer.class.getPackage().getImplementationVendor())
                        .setNetworkClass(NetworkClass.SITE)
                )
                .setApplication(BaseApplication.builder()
                    .setRouter(BaseHttpRouter.builder()
                        .addDomain(BaseHttpDomain.builder()
                                .setHttpAddress(httpAddress1)
                                .addService(BaseHttpService.builder()
                                        .setPath("/domain1")
                                        .setHandler(ctx -> {
                                                ctx.response()
                                                        .setResponseStatus(HttpResponseStatus.OK)
                                                        .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                                        .setCharset(StandardCharsets.UTF_8);
                                                ctx.write("domain1 " +
                                                        ctx.httpRequest().getParameter().toString() + " " +
                                                        ctx.httpRequest().getLocalAddress() +  " " +
                                                        ctx.httpRequest().getRemoteAddress());
                                        })
                                        .build())
                                .build())
                        .addDomain(BaseHttpDomain.builder()
                                .setHttpAddress(httpAddress2)
                                .addService(BaseHttpService.builder()
                                        .setPath("/domain2")
                                        .setHandler(ctx -> {
                                                ctx.response()
                                                        .setResponseStatus(HttpResponseStatus.OK)
                                                        .setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                                                        .setCharset(StandardCharsets.UTF_8);
                                                ctx.write("domain2 " +
                                                        ctx.httpRequest().getParameter().toString() + " " +
                                                        ctx.httpRequest().getLocalAddress() +  " " +
                                                        ctx.httpRequest().getRemoteAddress());
                                        })
                                        .build())
                                .build())
                        .build())
                    .build())
                .build();
        try {
            server.bind();
        } catch (BindException e) {
            throw new RuntimeException(e);
        }
    }
}
