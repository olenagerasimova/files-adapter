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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Random port.
 * @since 0.5
 */
public final class RandomPort {

    /**
     * Expected property name.
     */
    private static final String PROP_NAME = "test.vertx.port";

    /**
     * Properties with port.
     */
    private final Properties props;

    /**
     * New random port from system properties.
     */
    public RandomPort() {
        this(System.getProperties());
    }

    /**
     * New random port from properties.
     * @param props Properties
     */
    public RandomPort(final Properties props) {
        this.props = props;
    }

    /**
     * Find port in properties or allocate new socket port if not found.
     * @return Port number
     */
    public int value() {
        final int port;
        if (this.props.contains(RandomPort.PROP_NAME)) {
            port = Integer.parseInt(this.props.getProperty(RandomPort.PROP_NAME));
        } else {
            port = newSocketPort();
        }
        return port;
    }

    /**
     * Allocate new socket port.
     * @return Port number
     */
    private static int newSocketPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }
}
