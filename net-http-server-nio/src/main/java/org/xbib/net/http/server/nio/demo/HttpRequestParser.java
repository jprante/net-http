package org.xbib.net.http.server.nio.demo;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class HttpRequestParser {

    private final ByteArray parseBytes = new ByteArray();
    private HttpMethod httpMethod;
    private String uri;
    private String version;
    private HttpHeaders headers;
    private byte[] body;
    private boolean ready = false;

    public HttpRequestParser() {
    }

    public void read(ByteBuffer buffer) {
        byte[] remaining = new byte[buffer.remaining()];
        buffer.get(remaining);
        parseBytes.add(remaining);
        if (httpMethod == null) {
            parseMethod();
        }
        if (httpMethod != null && uri == null) {
            parseUri();
        }
        if (httpMethod != null && uri != null && version == null) {
            parseVersion();
        }
        if (httpMethod != null && uri != null && version != null && headers == null) {
            parseHeaders();
        }
        if (httpMethod != null && uri != null && version != null && headers != null && headers.containsKey("Content-Length") && body == null) {
            parseBody();
            ready = true;
        } else if (httpMethod != null && uri != null && version != null && headers != null && !headers.containsKey("Content-Length")) {
            ready = true;
        }
    }

    private void parseBody() {
        int bodyLength = Integer.parseInt(headers.get("Content-Length"));
        int size = parseBytes.size();
        if (size < bodyLength) {
            return;
        }
        byte[] copyArray = parseBytes.getBytes();
        this.body = new byte[bodyLength];
        System.arraycopy(copyArray, 0, this.body, 0, bodyLength);
        parseBytes.removeFirst(bodyLength);
    }

    private void parseHeaders() {
        int size = parseBytes.size();
        StringBuilder sb = new StringBuilder();
        boolean ready = false;
        int i = 0;
        for (; i < size; i++) {
            if (i + 3 < size
                    && parseBytes.getChar(i) == '\r'
                    && parseBytes.getChar(i + 1) == '\n'
                    && parseBytes.getChar(i + 2) == '\r'
                    && parseBytes.getChar(i + 3) == '\n') {
                ready = true;
                break;
            }
            sb.append(parseBytes.getChar(i));
        }
        if (!ready) {
            return;
        }
        String headersStr = sb.toString().trim();
        this.headers = new HttpHeaders();
        Arrays.stream(headersStr.split("\r\n"))
                .forEach(headerStr -> {
                    String[] split = headerStr.split(":");
                    String key = split[0];
                    String value = split[1].trim();
                    this.headers.add(key, value);
                });
        parseBytes.removeFirst(i + 4);
    }

    private void parseVersion() {
        int size = parseBytes.size();
        if (size < 10) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            char c = parseBytes.getChar(i);
            sb.append(c);
        }
        if (!sb.toString().equals("HTTP/1.1\r\n")) {
            return;
        }
        version = "HTTP/1.1";
        parseBytes.removeFirst(8);
    }

    private void parseUri() {
        int size = parseBytes.size();
        StringBuilder sb = new StringBuilder();
        boolean ready = false;
        for (int i = 0; i < size; i++) {
            char c = parseBytes.getChar(i);
            if (c == ' ') {
                ready = true;
                break;
            }
            sb.append(c);
        }
        if (!ready) {
            return;
        }
        uri = sb.toString();
        parseBytes.removeFirst(uri.length() + 1);
    }

    public boolean isReady() {
        return ready;
    }

    public HttpRequest getHttpRequest() {
        return new HttpRequest(httpMethod, uri, version, headers, body);
    }

    private void parseMethod() {
        _parseMethod();
        if (httpMethod != null) {
            int l = httpMethod.name().length();
            if (parseBytes.getChar(l) == ' ') {
                parseBytes.removeFirst(l + 1);
            } else {
                httpMethod = null;
            }
        }
    }

    private void _parseMethod() {
        int size = parseBytes.size();
        if (size < 3) {
            return;
        }

        char c0 = parseBytes.getChar(0);
        char c1 = parseBytes.getChar(1);
        char c2 = parseBytes.getChar(2);

        if ((c0 == 'G') && (c1 == 'E') && (c2 == 'T')) {
            httpMethod = HttpMethod.GET;
            return;
        }

        if (size < 4) {
            return;
        }

        char c3 = parseBytes.getChar(3);
        if (c0 == 'P' && c1 == 'O' && c2 == 'S' && c3 == 'T') {
            httpMethod = HttpMethod.POST;
            return;
        }

        if (c0 == 'P' && c1 == 'U' && c2 == 'T') {
            httpMethod = HttpMethod.PUT;
            return;
        }

        if (size < 6) {
            return;
        }

        char c4 = parseBytes.getChar(4);
        char c5 = parseBytes.getChar(5);
        if (c0 == 'D' && c1 == 'E' && c2 == 'L' && c3 == 'E' && c4 == 'T' && c5 == 'E') {
            httpMethod = HttpMethod.DELETE;
            return;
        }

        if (c0 == 'H' && c1 == 'E' && c2 == 'A' && c3 == 'D') {
            httpMethod = HttpMethod.HEAD;
            return;
        }

        if (c0 == 'P' && c1 == 'A' && c2 == 'T' && c3 == 'C' && c4 == 'H') {
            httpMethod = HttpMethod.PATCH;
        }

        if (c0 == 'T' && c1 == 'R' && c2 == 'A' && c3 == 'C' && c4 == 'E') {
            httpMethod = HttpMethod.TRACE;
        }

        if (size < 7) {
            return;
        }

        char c6 = parseBytes.getChar(6);
        if (c0 == 'O' && c1 == 'P' && c2 == 'T' && c3 == 'I' && c4 == 'O' && c5 == 'N' && c6 == 'S') {
            httpMethod = HttpMethod.OPTIONS;
            return;
        }

        if (c0 == 'C' && c1 == 'O' && c2 == 'N' && c3 == 'N' && c4 == 'E' && c5 == 'C' && c6 == 'T') {
            httpMethod = HttpMethod.CONNECT;
        }
    }
}
