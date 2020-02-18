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
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsWithStatus;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Flow;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;

/**
 * A {@link Slice} which servers binary files.
 *
 * @since 0.1
 */
public final class FilesSlice implements Slice {

    /**
     * The storage.
     */
    private final RxStorage storage;

    /**
     * Ctor.
     * @param storage The storage.
     */
    public FilesSlice(final Storage storage) {
        this.storage = new RxStorageWrapper(storage);
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Flow.Publisher<ByteBuffer> publisher) {
        final RequestLineFrom rline = new RequestLineFrom(line);
        final Key key = new Key.From(rline.uri().toString().substring(1).split("/"));
        final Response response;
        final String method = rline.method();
        final Publisher<ByteBuffer> reactive = FlowAdapters.toPublisher(publisher);
        final int okay = 200;
        final int zero = 0;
        if (method.equals("GET")) {
            response = connection -> {
                connection.accept(
                    okay,
                    new HashSet<>(zero),
                    FlowAdapters.toFlowPublisher(
                        Flowable.fromPublisher(reactive)
                            .flatMapCompletable(byteBuffer -> Completable.complete())
                            .andThen(this.storage.value(key).flatMapPublisher(flow -> flow))
                    )
                );
            };
        } else if (method.equals("POST") || method.equals("PUT")) {
            response = connection -> connection.accept(
                okay,
                new HashSet<>(zero),
                FlowAdapters.toFlowPublisher(
                    this.storage.save(
                        key,
                        Flowable.fromPublisher(reactive)
                    ).andThen(Flowable.empty())
                )
            );
        } else {
            final int nfound = 404;
            response = new RsWithStatus(nfound);
        }
        return response;
    }
}
