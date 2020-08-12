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

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.SliceSimple;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Map.Entry;
import org.apache.http.client.utils.URIBuilder;
import org.reactivestreams.Publisher;

/**
 * Binary files proxy {@link Slice} implementation.
 * @since 0.4
 */
public final class FileProxySlice implements Slice {

    /**
     * Client slice.
     */
    private final Slice client;

    /**
     * New files proxy slice.
     * @param clients HTTP clients
     * @param remote Remote URI
     */
    public FileProxySlice(final ClientSlices clients, final URI remote) {
        this(new ClientSlice(clients, remote));
    }

    /**
     * New files proxy slice.
     * @param client HTTP client slice
     */
    private FileProxySlice(final Slice client) {
        this.client = client;
    }

    @Override
    public Response response(
        final String line, final Iterable<Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return this.client.response(line, headers, body);
    }

    /**
     * Client slice.
     * @since 0.5
     */
    private static final class ClientSlice implements Slice {

        /**
         * Client HTTP slices.
         */
        private final ClientSlices clients;

        /**
         * Remote URI.
         */
        private final URI remote;

        /**
         * New client slice from remote URI.
         * @param clients Slice clients
         * @param remote Remote URI
         */
        ClientSlice(final ClientSlices clients, final URI remote) {
            this.clients = clients;
            this.remote = remote;
        }

        @Override
        public Response response(final String line, final Iterable<Entry<String, String>> headers,
            final Publisher<ByteBuffer> body) {
            final Slice slice;
            final String host = this.remote.getHost();
            final int port = this.remote.getPort();
            switch (this.remote.getScheme()) {
                case "https":
                    slice = this.clients.https(host, port);
                    break;
                case "http":
                    slice = this.clients.http(host, port);
                    break;
                default:
                    slice = new SliceSimple(new RsWithStatus(RsStatus.INTERNAL_ERROR));
                    break;
            }
            final RequestLineFrom rqline = new RequestLineFrom(line);
            final URI uri = rqline.uri();
            return slice.response(
                new RequestLine(
                    rqline.method().value(),
                    new URIBuilder(uri)
                        .setPath(
                            Paths.get(this.remote.getPath(), uri.getPath())
                                .normalize().toString()
                        ).toString(),
                    rqline.version()
                ).toString(),
                headers, body
            );
        }
    }
}
