package org.xbib.net.http.server.nio.demo;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class HttpResponse {

    private final String version = "HTTP/1.1";
    private final HttpHeaders headers = new HttpHeaders();
    private String statusCode = "200";
    private String reasonPhrase = "OK";
    private ByteArray body;

    HttpResponse() {
    }

    public void addHeader(String key, String value) {
        headers.add(key, value);
    }

    public byte[] getBytes() {
        headers.remove("content-length");
        if (body != null) {
            headers.add("content-length", String.valueOf(body.size()));
        }
        String h = headers.getList().stream()
                .map(it -> it.getKey() + ": " + it.getValue())
                .collect(Collectors.joining("\r\n")) + "\r\n\r\n";
        ByteArray byteArray = new ByteArray();
        byteArray.add((version + " ").getBytes(StandardCharsets.US_ASCII));
        byteArray.add((statusCode + " ").getBytes(StandardCharsets.US_ASCII));
        byteArray.add((reasonPhrase + "\r\n").getBytes(StandardCharsets.US_ASCII));
        byteArray.add(h.getBytes(StandardCharsets.US_ASCII));
        if (body != null) {
            byteArray.add(body.getBytes());
        }
        byteArray.add("\r\n".getBytes(StandardCharsets.US_ASCII));
        return byteArray.getBytes();
    }

    public String getVersion() {
        return version;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public ByteArray getBody() {
        return body;
    }

    public void setBody(ByteArray body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "version='" + version + '\'' +
                ", statusCode='" + statusCode + '\'' +
                ", reasonPhrase='" + reasonPhrase + '\'' +
                ", headers=" + headers +
                ", body=" + body +
                '}';
    }
}
