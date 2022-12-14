package org.xbib.net.http.server.nio.demo;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface SocketHandler {
    default void onRegistered() throws IOException {
    }

    default void onRead() throws IOException {
    }

    default void onWrite() throws IOException {
    }

    default void close() throws IOException {
    }

    default void write(ByteBuffer buffer) throws IOException {
    }
}
