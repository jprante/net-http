package org.xbib.net.http.server;

import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.buffer.DataBufferFactory;
import org.xbib.net.http.HttpHeaders;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.HttpVersion;
import org.xbib.net.http.cookie.Cookie;

import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public interface HttpResponseBuilder {

    HttpResponseBuilder setDataBufferFactory(DataBufferFactory dataBufferFactory);

    DataBufferFactory getDataBufferFactory();

    HttpResponseBuilder setVersion(HttpVersion version);

    HttpResponseBuilder setResponseStatus(HttpResponseStatus status);

    HttpResponseBuilder setContentType(String contentType);

    HttpResponseBuilder setCharset(Charset charset);

    HttpHeaders getHeaders();

    HttpResponseBuilder addHeader(CharSequence name, String value);

    HttpResponseBuilder setHeader(CharSequence name, String value);

    HttpResponseBuilder setTrailingHeader(CharSequence name, String value);

    HttpResponseBuilder addCookie(Cookie cookie);

    HttpResponseBuilder shouldFlush(boolean sholdFlush);

    boolean shouldFlush();

    HttpResponseBuilder shouldClose(boolean shouldClose);

    boolean shouldClose();

    HttpResponseBuilder setSequenceId(Integer sequenceId);

    HttpResponseBuilder setStreamId(Integer streamId);

    HttpResponseBuilder setResponseId(Long responseId);

    HttpResponseBuilder write(String body);

    HttpResponseBuilder write(CharBuffer charBuffer, Charset charset);

    HttpResponseBuilder write(DataBuffer dataBuffer);

    HttpResponseBuilder write(InputStream inputStream, int bufferSize);

    HttpResponseBuilder write(FileChannel fileChannel, int bufferSize);

    void reset();

    HttpResponse build();

}
