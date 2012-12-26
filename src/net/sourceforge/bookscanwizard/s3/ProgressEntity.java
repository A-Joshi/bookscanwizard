/*
 *
 * Copyright (c) 2013 by Steve Devore
 *                       http://bookscanwizard.sourceforge.net
 *
 * This file is part of the Book Scan Wizard.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package net.sourceforge.bookscanwizard.s3;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

/**
 * A wrapper that listens for upload progress.
 */
public class ProgressEntity extends HttpEntityWrapper {
    private double contentLength;
    private ProgressListener listener;

    public ProgressEntity(HttpEntity entity) {
        super(entity);
    }

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }
    
    @Override
    public long getContentLength() {
        long length = super.getContentLength();
        contentLength = length;
        return length;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        super.writeTo(new CountingOutputStream(outstream));
    }

    public class CountingOutputStream extends FilterOutputStream {
        private long transferred;

        public CountingOutputStream(final OutputStream out) {
            super(out);
            this.transferred = 0;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            this.transferred += len;
            transferred(this.transferred);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            this.transferred++;
            transferred(this.transferred);
        }

        private void transferred(long transferred) {
            if (listener != null) {
                listener.updateProgress(transferred / contentLength);
            }
        }
    }
}
