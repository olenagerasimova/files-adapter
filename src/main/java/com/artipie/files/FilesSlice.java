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
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDelete;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.http.slice.SliceUpload;
import com.artipie.http.slice.SliceWithHeaders;

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
        this(storage, Permissions.FREE, Authentication.ANONYMOUS);
    }

    /**
     * Ctor used by Artipie server which knows `Authentication` implementation.
     * @param storage The storage. And default parameters for free access.
     * @param perms Access permissions.
     * @param auth Auth details.
     */
    public FilesSlice(final Storage storage, final Permissions perms, final Authentication auth) {
        super(
            new SliceRoute(
                new RtRulePath(
                    ByMethodsRule.Standard.GET,
                    new BasicAuthSlice(
                        new SliceWithHeaders(
                            new SliceDownload(storage),
                            new Headers.From(new ContentType("application/octet-stream"))
                        ),
                        auth,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    ByMethodsRule.Standard.PUT,
                    new BasicAuthSlice(
                        new SliceUpload(storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.WRITE)
                    )
                ),
                new RtRulePath(
                    ByMethodsRule.Standard.DELETE,
                    new BasicAuthSlice(
                        new SliceDelete(storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.DELETE)
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new SliceSimple(new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED))
                )
            )
        );
    }
}
