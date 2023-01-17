//
// Copyright 2022- IBM Inc. All rights reserved
// SPDX-License-Identifier: Apache-2.0
//

package com.ibm.geds.hdfs;

import com.ibm.geds.GEDSFile;
import org.apache.hadoop.fs.Seekable;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class GEDSOutputStream extends OutputStream implements Seekable {
    private final GEDSFile file;

    public GEDSOutputStream(GEDSFile file) {
        this.file = file;
    }

    private void checkOpen() throws IOException {
        if (file.isClosed()) {
            throw new EOFException("The file is already closed!");
        }
    }

    private long position = 0;
    private byte[] singleByteBuffer = new byte[1];

    @Override
    public void write(int b) throws IOException {
        synchronized (this) {
            checkOpen();
            singleByteBuffer[0] =(byte) b;
            file.write(position, singleByteBuffer, 0, 1);
            position += 1;
        }
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        synchronized (this) {
            checkOpen();
            file.write(position, b, off, len);
            position += len;
        }
    }

    public final void write(ByteBuffer buffer) throws IOException {
        synchronized (this) {
            checkOpen();
            position += file.write(position, buffer);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            super.close();
            file.seal();
            file.close();
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        synchronized (this) {
            checkOpen();
            long size = file.size();
            if (pos > size) {
                throw new EOFException("Cannot seek out of bounds.");
            }
            position = pos;
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (this) {
            checkOpen();
            file.seal();
        }
    }

    public void seekToEnd() throws IOException {
        synchronized (this) {
            checkOpen();
            position = file.size();
        }
    }

    @Override
    public long getPos() throws IOException {
        checkOpen();
        return position;
    }

    @Override
    public boolean seekToNewSource(long targetPos) throws IOException {
        synchronized (this) {
            checkOpen();
            long size = file.size();
            if (targetPos > size) {
                return false;
            }
            position = targetPos;
        }
        return true;
    }
}
