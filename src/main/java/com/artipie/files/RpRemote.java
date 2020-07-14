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
import com.artipie.asto.Key;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.reactive.client.ReactiveRequest;

/**
 * {@link Repository} getting artifacts from a remote {@link URI}.
 *
 * @since 0.4
 * @checkstyle ReturnCountCheck (500 lines)
 */
public final class RpRemote implements Repository {

    /**
     * Target URI.
     */
    private final URI remote;

    /**
     * HTTP client.
     */
    private final HttpClient http;

    /**
     * Remote proxy cache.
     */
    private final ProxyCache cache;

    /**
     * Proxy for URI.
     * @param http HTTP client
     * @param uri URI
     */
    public RpRemote(final HttpClient http, final URI uri) {
        this(http, uri, ProxyCache.NOP);
    }

    /**
     * Caching proxy for URI.
     * @param http Http client
     * @param uri URI
     * @param cache Proxy cache
     */
    public RpRemote(final HttpClient http, final URI uri, final ProxyCache cache) {
        this.remote = uri;
        this.http = http;
        this.cache = cache;
    }

    @Override
    public CompletionStage<? extends Content> artifact(final URI uri) {
        final URIBuilder builder = new URIBuilder(this.remote);
        builder.setPath(Paths.get(builder.getPath(), uri.getPath()).normalize().toString());
        final Request request = this.http.newRequest(builder.toString());
        return this.cache.load(
            new Key.From(relativePath(uri)),
            () -> Flowable.fromPublisher(
                ReactiveRequest.newBuilder(request).build().response(
                    (rsp, body) -> {
                        final Single<Content> res;
                        if (rsp.getStatus() == HttpURLConnection.HTTP_OK) {
                            res = Single.just(
                                new Content.From(
                                    Optional.ofNullable(
                                        rsp.getHeaders().get("Content-Size")
                                    ).map(Long::parseLong),
                                    Flowable.fromPublisher(body).map(chunk -> chunk.buffer)
                                )
                            );
                        } else if (rsp.getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
                            res = Single.error(
                                new ArtifactNotFoundException(builder.getPath())
                            );
                        } else {
                            res = Single.error(
                                new Exception(
                                    String.format(
                                        "Failed to fetch remote repo: %d", rsp.getStatus()
                                    )
                                )
                            );
                        }
                        return res.toFlowable();
                    }
                )
            ).singleOrError().to(SingleInterop.get())
        );
    }

    /**
     * Relative normalized path or provided URI.
     * @param uri URI with path to relativize
     * @return Relative path
     */
    private static String relativePath(final URI uri) {
        try {
            return new URI(
                uri.getScheme(), null, uri.getHost(), uri.getPort(),
                null, null, null
            ).relativize(uri).getPath();
        } catch (final URISyntaxException err) {
            throw new IllegalStateException("Invalid URI", err);
        }
    }
}
