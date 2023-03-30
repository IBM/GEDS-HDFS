//
// Copyright 2022- IBM Inc. All rights reserved
// SPDX-License-Identifier: Apache-2.0
//

package com.ibm.geds.hdfs;

import com.ibm.geds.GEDSFile;
import org.apache.hadoop.fs.FSInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import static java.lang.Math.min;

public class GEDSInputStream extends FSInputStream {

    private final GEDSFile file;
    private final long fileSize;

    public GEDSInputStream(GEDSFile file) {
        this.file = file;
        try {
            this.fileSize = file.size();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        this.tmpbuffer = ByteBuffer.allocate(8);
    }

    public String metadata() throws IOException {
        return file.metadata();
    }

    public byte[] metadataAsByteArray() throws IOException {
        return file.metadataAsByteArray();
    }

    private long position = 0;
    private ByteBuffer tmpbuffer;

    @Override
    public int read() throws IOException {
        synchronized (this) {
            tmpbuffer.clear();
            tmpbuffer.limit(1);
            int length = file.read(position, tmpbuffer);
            if (length > 1) {
                throw new InternalError("Invalid ByteBuffer access");
            }
            if (length > 0) {
                position += length;
            }
            tmpbuffer.flip();
            return (length <= 0) ? -1 : tmpbuffer.get();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (this) {
            int length = file.read(position, b, off, len);
            if (length > 0) {
                position += length;
            }
            return length;
        }
    }

    public final int read(ByteBuffer buffer) throws IOException {
        if (buffer == null) {
            throw new NullPointerException();
        }
        if (buffer.remaining() == 0) {
            return 0;
        }
        synchronized (this) {
            int length = file.read(position, buffer);
            if (length > 0) {
                position += length;
            }
            return length;
        }
    }

    public final double readDouble() throws IOException {
        synchronized (this) {
            tmpbuffer.clear();
            tmpbuffer.limit(Double.BYTES);
            int length = file.read(position, tmpbuffer);
            if (length != Double.BYTES) {
                throw new EOFException("Not enough data.");
            }
            position += length;
            tmpbuffer.flip();
            return tmpbuffer.getDouble();
        }
    }

    public final float readFloat() throws IOException {
        synchronized (this) {
            tmpbuffer.clear();
            tmpbuffer.limit(Float.BYTES);
            int length = file.read(position, tmpbuffer);
            if (length != Float.BYTES) {
                throw new EOFException("Not enough data.");
            }
            position += length;
            tmpbuffer.flip();
            return tmpbuffer.getFloat();
        }
    }

    public final int readInt() throws IOException {
        synchronized (this) {
            tmpbuffer.clear();
            tmpbuffer.limit(Integer.BYTES);
            int length = file.read(position, tmpbuffer);
            if (length != Integer.BYTES) {
                throw new EOFException("Not enough data.");
            }
            position += length;
            tmpbuffer.flip();
            return tmpbuffer.getInt();
        }
    }

    public final long readLong() throws IOException {
        synchronized (this) {
            tmpbuffer.clear();
            tmpbuffer.limit(Long.BYTES);
            int length = file.read(position, tmpbuffer);
            if (length != Long.BYTES) {
                throw new EOFException("Not enough data.");
            }
            position += length;
            tmpbuffer.flip();
            return tmpbuffer.getLong();
        }
    }

    public final short readShort() throws IOException {
        synchronized (this) {
            tmpbuffer.clear();
            tmpbuffer.limit(Short.BYTES);
            int length = file.read(position, tmpbuffer);
            if (length != Short.BYTES) {
                throw new EOFException("Not enough data.");
            }
            position += length;
            tmpbuffer.flip();
            return tmpbuffer.getShort();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        synchronized (this) {
            long maxSkip = min(fileSize - position, n);
            if (maxSkip < 0) {
                throw new RuntimeException("position > size");
            }
            position += maxSkip;
            return maxSkip;
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        synchronized (this) {
            if (pos < 0) {
                throw new EOFException("Negative seek is not allowed.");
            }
            if (pos > fileSize) {
                throw new EOFException("Cannot seek out of bounds for '" + file.bucket + "/" + file.key
                        + "' - Requested: " + pos + " file size: " + fileSize);
            }
            position = pos;
        }
    }

    @Override
    public long getPos() throws IOException {
        return position;
    }

    @Override
    public boolean seekToNewSource(long targetPos) throws IOException {
        synchronized (this) {
            if (targetPos > fileSize) {
                return false;
            }
            position = targetPos;
        }
        return true;
    }

    @Override
    public int available() throws IOException {
        long remaining = fileSize - position;
        if (remaining > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remaining;
    }

    @Override
    public void close() throws IOException {
        super.close();
        file.close();
    }

    @Override
    public int read(long position, byte[] buffer, int offset, int length) throws IOException {
        return file.read(position, buffer, offset, length);
    }

    @Override
    public void readFully(long position, byte[] buffer, int offset, int length) throws IOException {
        int len = file.read(position, buffer, offset, length);
        if (len != length) {
            throw new EOFException("Unable to read " + file.bucket + "/" + file.key + ":  length " + length
                    + " at position " + position + ".");
        }
    }

    @Override
    public void readFully(long position, byte[] buffer) throws IOException {
        int len = file.read(position, buffer, 0, buffer.length);
        if (len != buffer.length) {
            throw new EOFException("Unable to read length " + buffer.length + " at position " + position + ".");
        }
    }

    public void readFully(long position, ByteBuffer buffer) throws IOException {
        int expected = buffer.remaining();
        readFully(position, buffer, expected);
    }

    public void readFully(long position, ByteBuffer buffer, int length) throws IOException {
        int len = file.read(position, buffer);
        if (len != length) {
            throw new EOFException("Unable to read length " + length + " at position " + position + ".");
        }
    }
}
