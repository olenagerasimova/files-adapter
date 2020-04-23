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

import com.artipie.asto.Storage;
import com.artipie.http.Slice;
import com.artipie.http.auth.Identities;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.auth.SliceAuth;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.http.slice.SliceUpload;

/**
 * A {@link Slice} which servers binary files.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class FilesSlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param storage The storage. And default parameters for free access.
     */
    public FilesSlice(final Storage storage) {
        this(storage, Permissions.FREE, Identities.ANONYMOUS);
    }

    /**
     * Ctor.
     * @param storage The storage.
     * @param perms Access permissions.
     * @param users Concrete identities.
     */
    public FilesSlice(final Storage storage, final Permissions perms, final Identities users) {
        super(
            new SliceRoute(
                new SliceRoute.Path(
                    new RtRule.ByMethod(RqMethod.GET),
                        new SliceAuth(
                            new SliceDownload(storage),
                            new Permission.ByName("download", perms),
                            users
                        )
                ),
                new SliceRoute.Path(
                    new RtRule.ByMethod(RqMethod.PUT),
                        new SliceAuth(
                            new SliceUpload(storage),
                            new Permission.ByName("upload", perms),
                            users
                        )
                ),
                new SliceRoute.Path(
                    RtRule.FALLBACK,
                    new SliceSimple(new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED))
                )
            )
        );
    }
}
