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
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for files adapter.
 * @since 0.1
 */
public class FileSliceITCase {

    /**
     * The host to send requests to.
     */
    private static final String HOST = "localhost";

    /**
     * File put works.
     * @param temp The temp dir.
     * @throws IOException If fails.
     */
    @Test
    void putWorks(@TempDir final Path temp) throws IOException {
        final String hello = "Hello world!!!";
        final int port = this.rndPort();
        final Vertx vertx = Vertx.vertx();
        final Storage storage = new FileStorage(temp, vertx.fileSystem());
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new FilesSlice(storage),
            port
        );
        server.start();
        final WebClient web = WebClient.create(vertx);
        web.put(port, FileSliceITCase.HOST, "/hello.txt")
            .rxSendBuffer(Buffer.buffer(hello.getBytes()))
            .blockingGet();
        MatcherAssert.assertThat(
            new String(
                new BlockingStorage(storage).value(new Key.From("hello.txt"))
            ),
            new IsEqual<>(hello)
        );
        server.stop();
        vertx.close();
    }

    /**
     * Put on complex name works correctly.
     * @param temp The temp dir.
     * @throws IOException if failed
     */
    @Test
    void complexNamePutWorks(@TempDir final Path temp) throws IOException {
        final String hello = "Hello world!!!!";
        final int port = this.rndPort();
        final Vertx vertx = Vertx.vertx();
        final Storage storage = new FileStorage(temp, vertx.fileSystem());
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new FilesSlice(storage),
            port
        );
        server.start();
        final WebClient web = WebClient.create(vertx);
        web.put(port, FileSliceITCase.HOST, "/hello/world.txt")
            .rxSendBuffer(Buffer.buffer(hello.getBytes()))
            .blockingGet();
        MatcherAssert.assertThat(
            new String(
                new BlockingStorage(storage).value(new Key.From("hello/world.txt"))
            ),
            new IsEqual<>(hello)
        );
        server.stop();
        vertx.close();
    }

    /**
     * Get on complex file works.
     *
     * @param temp The temp dir.
     * @throws IOException If fails.
     */
    @Test
    @Disabled
    void getComplexNameWorks(@TempDir final Path temp) throws IOException {
        final String hello = "Hellooo world!!";
        final int port = this.rndPort();
        final Vertx vertx = Vertx.vertx();
        final Storage storage = new FileStorage(temp, vertx.fileSystem());
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new FilesSlice(storage),
            port
        );
        server.start();
        final WebClient web = WebClient.create(vertx);
        final String hellot = "hello/world1.txt";
        new BlockingStorage(storage).save(new Key.From(hellot), hello.getBytes());
        MatcherAssert.assertThat(
            new String(
                Files.readAllBytes(
                    Paths.get(temp.toString(), hellot)
                )
            ), new IsEqual<>(hello)
        );
        MatcherAssert.assertThat(
            new String(
                web.get(port, FileSliceITCase.HOST, String.format("/%s", hellot))
                    .rxSend()
                    .blockingGet()
                    .bodyAsBuffer()
                    .getBytes()
            ),
            new IsEqual<>(hello)
        );
        server.stop();
        vertx.close();
    }

    /**
     * Get file works.
     * @param temp The temp dir.
     * @throws IOException If fails.
     */
    @Test
    @Disabled
    void getWorks(@TempDir final Path temp) throws IOException {
        final String hello = "Hello world!!";
        final int port = this.rndPort();
        final Vertx vertx = Vertx.vertx();
        final Storage storage = new FileStorage(temp, vertx.fileSystem());
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new FilesSlice(storage),
            port
        );
        server.start();
        final WebClient web = WebClient.create(vertx);
        final String hellot = "hello1.txt";
        new BlockingStorage(storage).save(new Key.From(hellot), hello.getBytes());
        MatcherAssert.assertThat(
            new String(
                Files.readAllBytes(
                    Paths.get(temp.toString(), hellot)
                )
            ), new IsEqual<>(hello)
        );
        MatcherAssert.assertThat(
            new String(
                web.get(port, FileSliceITCase.HOST, "/hello1.txt")
                    .rxSend()
                    .blockingGet()
                    .bodyAsBuffer()
                    .getBytes()
            ),
            new IsEqual<>(hello)
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
