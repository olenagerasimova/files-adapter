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

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.jcabi.log.Logger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.concurrent.CompletionException;
import org.reactivestreams.Publisher;

/**
 * Binary files proxy {@link Slice} implementation.
 * @since 0.4
 */
public final class FileProxySlice implements Slice {

    /**
     * Maven Repository.
     */
    private final Repository repo;

    /**
     * Ctor.
     *
     * @param repo Maven Repository.
     */
    public FileProxySlice(final Repository repo) {
        this.repo = repo;
    }

    @Override
    public Response response(
        final String line, final Iterable<Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            this.repo.artifact(new RequestLineFrom(line).uri()).<Response>thenApply(
                content -> new RsWithStatus(
                    new RsWithHeaders(
                        new RsWithBody(content),
                        content.size()
                            .map(size -> new Header("Content-Length", Long.toString(size)))
                            .<Headers>map(Headers.From::new)
                            .orElse(Headers.EMPTY)
                    ),
                    RsStatus.OK
                )
            ).<Response>thenApply(
                resp -> new RsWithHeaders(resp, new ContentType("application/octet-stream"))
            ).exceptionally(
                err -> {
                    final Throwable source;
                    if (err instanceof CompletionException) {
                        source = CompletionException.class.cast(err).getCause();
                    } else {
                        source = err;
                    }
                    final Response rsp;
                    if (source instanceof ArtifactNotFoundException) {
                        rsp = StandardRs.NOT_FOUND;
                    } else {
                        Logger.error(this, "Failed to download artifact: %[exception]s", source);
                        rsp = new RsWithStatus(
                            new RsWithBody(err.getMessage(), StandardCharsets.UTF_8),
                            RsStatus.INTERNAL_ERROR
                        );
                    }
                    return rsp;
                }
            )
        );
    }
}
