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

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.StandardRs;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FileProxySlice}.
 *
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class FileProxySliceTest {

    @Test
    void sendEmptyHeadersAndContent() throws Exception {
        final AtomicReference<Iterable<Map.Entry<String, String>>> headers;
        headers = new AtomicReference<>();
        final AtomicReference<byte[]> body = new AtomicReference<>();
        new FileProxySlice(
            new FakeClientSlices(
                (rqline, rqheaders, rqbody) -> {
                    headers.set(rqheaders);
                    return new AsyncResponse(
                        new PublisherAs(rqbody).bytes().thenApply(
                            bytes -> {
                                body.set(bytes);
                                return StandardRs.OK;
                            }
                        )
                    );
                }
            ),
            new URI("http://host/path")
        ).response(
            new RequestLine(RqMethod.GET, "/").toString(),
            new Headers.From("X-Name", "Value"),
            new Content.From("data".getBytes())
        ).send(
            (status, rsheaders, rsbody) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Headers are empty",
            headers.get(),
            new IsEmptyIterable<>()
        );
        MatcherAssert.assertThat(
            "Body is empty",
            body.get(),
            new IsEqual<>(new byte[0])
        );
    }

    /**
     * Fake {@link ClientSlices} implementation that returns specified result.
     *
     * @since 0.7
     */
    private static final class FakeClientSlices implements ClientSlices {

        /**
         * Slice returned by requests.
         */
        private final Slice result;

        /**
         * Ctor.
         *
         * @param result Slice returned by requests.
         */
        FakeClientSlices(final Slice result) {
            this.result = result;
        }

        @Override
        public Slice http(final String host) {
            return this.result;
        }

        @Override
        public Slice http(final String host, final int port) {
            return this.result;
        }

        @Override
        public Slice https(final String host) {
            return this.result;
        }

        @Override
        public Slice https(final String host, final int port) {
            return this.result;
        }
    }
}
