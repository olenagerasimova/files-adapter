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
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.UriClientSlice;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.Authenticator;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Binary files proxy {@link Slice} implementation.
 * @since 0.4
 */
public final class FileProxySlice implements Slice {

    /**
     * Remote slice.
     */
    private final Slice remote;

    /**
     * New files proxy slice.
     * @param clients HTTP clients
     * @param remote Remote URI
     */
    public FileProxySlice(final ClientSlices clients, final URI remote) {
        this(new UriClientSlice(clients, remote));
    }

    /**
     * New files proxy slice.
     * @param clients HTTP clients
     * @param remote Remote URI
     * @param auth Authenticator
     */
    public FileProxySlice(final ClientSlices clients, final URI remote, final Authenticator auth) {
        this(new AuthClientSlice(new UriClientSlice(clients, remote), auth));
    }

    /**
     * Ctor.
     *
     * @param remote Remote slice
     */
    private FileProxySlice(final Slice remote) {
        this.remote = remote;
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return this.remote.response(line, Headers.EMPTY, Content.EMPTY);
    }
}
