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
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Proxy cache with based on {@link Storage}.
 * @since 0.4
 */
public final class StorageCache implements ProxyCache {

    /**
     * Cache storage.
     */
    private final RxStorage storage;

    /**
     * New cache.
     * @param storage Cache storage
     */
    public StorageCache(final Storage storage) {
        this.storage = new RxStorageWrapper(storage);
    }

    @Override
    public CompletionStage<? extends Content> load(
        final Key key, final Supplier<? extends CompletionStage<? extends Content>> remote
    ) {
        return this.storage.exists(key).filter(exists -> exists)
            .flatMapSingleElement(ignore -> this.storage.value(key))
            .switchIfEmpty(
                SingleInterop.fromFuture(remote.get()).flatMapCompletable(
                    content -> this.storage.save(
                        key, new Content.From(content.size(), content)
                    )
                ).andThen(this.storage.value(key))
            ).to(SingleInterop.get());
    }
}
