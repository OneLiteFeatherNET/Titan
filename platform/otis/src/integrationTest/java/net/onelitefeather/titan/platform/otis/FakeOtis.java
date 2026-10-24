/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.platform.otis;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * A stand-in for Otis on a loopback port the OS picks. Replies come from a queue, the last one
 * repeats, and every request is recorded, so a test can check path, header and body.
 */
final class FakeOtis implements AutoCloseable {

    /** How the fake answers one request. A non-null latch makes the request hang until it opens. */
    record Reply(int status, String body, CountDownLatch hang) {

        static Reply of(int status, String body) {
            return new Reply(status, body, null);
        }

        static Reply empty(int status) {
            return new Reply(status, "", null);
        }
    }

    /** One request the fake received. */
    record Received(String method, String path, String query, String authorization, String body) {
    }

    private final HttpServer server;
    private final Deque<Reply> replies = new ArrayDeque<>();
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private Reply fallback = Reply.empty(200);

    FakeOtis() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        this.server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            this.received.add(new Received(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), exchange.getRequestURI().getQuery(), exchange.getRequestHeaders().getFirst("Authorization"), body));
            Reply reply = nextReply();
            if (reply.hang() != null) {
                awaitQuietly(reply.hang());
            }
            byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(reply.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        this.server.start();
    }

    String url() {
        return "http://127.0.0.1:" + this.server.getAddress().getPort();
    }

    /** Queues one reply; queued replies are used in order before the fallback. */
    FakeOtis then(Reply reply) {
        this.replies.add(reply);
        return this;
    }

    /** The reply used once the queue is empty. */
    FakeOtis always(Reply reply) {
        this.fallback = reply;
        return this;
    }

    List<Received> received() {
        return List.copyOf(this.received);
    }

    @Override
    public void close() {
        this.server.stop(0);
    }

    private synchronized Reply nextReply() {
        return this.replies.isEmpty() ? this.fallback : this.replies.poll();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
