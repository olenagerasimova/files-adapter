/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.files;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.vertx.VertxSliceServer;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for files adapter.
 * @since 0.1
 */
public class FileSliceTest {

    @Test
    void putWorks() throws IOException {
        final String hello = "Hello world!!!";
        final Path temp = Files.createTempDirectory("temp");
        final int port = rndPort();
        final Vertx vertx = Vertx.vertx();
        final Storage storage = new FileStorage(temp);
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new FilesSlice(storage),
            port
        );
        server.start();
        final WebClient webClient = WebClient.create(vertx);
        webClient.put(port, "localhost", "/hello.txt")
            .rxSendBuffer(Buffer.buffer(hello.getBytes()))
            .blockingGet();
        MatcherAssert.assertThat(
            new String(
                new BlockingStorage(storage).value(new Key.From("hello.txt"))
            ),
            Matchers.equalTo(hello)
        );
        server.stop();
        vertx.close();
    }

    @Test
    void getWorks() throws IOException {
        final String hello = "Hello world!!!";
        final Path temp = Files.createTempDirectory("temp");
        final int port = rndPort();
        System.out.println("port " + port);
        final Vertx vertx = Vertx.vertx();
        final Storage storage = new FileStorage(temp);
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new FilesSlice(storage),
            port
        );
        server.start();
        final WebClient webClient = WebClient.create(vertx);
        new BlockingStorage(storage).save(new Key.From("hello.txt"), hello.getBytes());
        MatcherAssert.assertThat(
            new String(
                Files.readAllBytes(
                    Path.of(temp.toString(), "hello.txt")
                )
            ), Matchers.equalTo(hello)
        );
        final HttpResponse<Buffer> response = webClient.get(port, "localhost", "/hello.txt")
            .rxSend()
            .blockingGet();
        MatcherAssert.assertThat(
            new String(
                response
                    .bodyAsBuffer()
                    .getBytes()
            ),
            Matchers.equalTo(hello)
        );
        server.stop();
        vertx.close();
    }



    /**
     * Find a random port.
     * @return The free port.
     * @throws IOException If fails.
     */
    private int rndPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

}
