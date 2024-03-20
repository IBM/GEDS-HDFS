//
// Copyright 2022- IBM Inc. All rights reserved
// SPDX-License-Identifier: Apache-2.0
//

package com.ibm.geds.hdfs;

import com.ibm.geds.GEDS;
import com.ibm.geds.GEDSConfig;
import com.ibm.geds.GEDSFile;
import com.ibm.geds.GEDSFileStatus;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

public class GEDSHadoopFileSystem extends FileSystem {

    private GEDS geds = null;
    private GEDSConfig gedsConfig = null;
    private URI uri;
    private String bucket;
    private Path workingDirectory;
    private long blockSize;

    @Override
    public void initialize(URI name, Configuration conf) throws IOException {
        super.initialize(name, conf);
        uri = name;
        bucket = name.getHost();
        gedsConfig = GEDSInstance.getConfig(conf);
        geds = GEDSInstance.initialize(bucket, conf);

        blockSize = gedsConfig.getLong(Constants.CACHE_BLOCK_SIZE);
        workingDirectory = new Path("/");
    }

    @Override
    public void close() throws IOException {
        // TODO: FIXME - Spark calls the close function multiple times.
        // super.close();
        // geds.stopGEDS();
        // geds = null;
        geds.relocate();
        geds.printStatistics();
    }

    @Override
    public String getScheme() {
        return "geds";
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public FSDataInputStream open(Path f, int bufferSize) throws IOException {
        GEDSFile file = geds.open(bucket, computeGEDSPath(f));
        return new FSDataInputStream(new BufferedFSInputStream(new GEDSInputStream(file), bufferSize));
    }

    private String computeGEDSPath(Path f) {
        try {
            String s = Path.getPathWithoutSchemeAndAuthority(f).toString();
            if (s.startsWith("/")) {
                s = s.substring(1);
            }
            return s;
        } catch(Exception e) {
            return "";
        }
    }

    @Override
    public FSDataOutputStream create(Path f, FsPermission permission, boolean overwrite, int bufferSize,
            short replication, long blockSize, Progressable progress) throws IOException {
        GEDSFile file = geds.create(bucket, computeGEDSPath(f));
        return new FSDataOutputStream(new BufferedOutputStream(new GEDSOutputStream(file)), statistics);
    }

    @Override
    public FSDataOutputStream append(Path f, int bufferSize, Progressable progress) throws IOException {
        GEDSFile file = geds.open(bucket, computeGEDSPath(f));

        GEDSOutputStream stream = new GEDSOutputStream(file);
        stream.seekToEnd();
        return new FSDataOutputStream(new BufferedOutputStream(stream), statistics, stream.getPos());
    }

    @Override
    public boolean rename(Path src, Path dst) throws IOException {
        return geds.renamePrefix(bucket, computeGEDSPath(src), computeGEDSPath(dst));
    }

    @Override
    public boolean delete(Path f, boolean recursive) throws IOException {
        if (recursive) {
            return geds.deletePrefix(bucket, computeGEDSPath(f));
        }
        return geds.delete(bucket, computeGEDSPath(f));
    }

    @Override
    public FileStatus[] listStatus(Path f) throws FileNotFoundException, IOException {
        String path = computeGEDSPath(f);
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        GEDSFileStatus[] st = geds.listAsFolder(bucket, path);
        FileStatus[] response = new FileStatus[st.length];
        for (int i = 0; i < st.length; i++) {
            GEDSFileStatus s = st[i];
            response[i] = new FileStatus(s.size, s.isDirectory, 1, blockSize, 0,
                    new Path(s.key).makeQualified(getUri(), workingDirectory));
        }
        return response;
    }

    @Override
    public void setWorkingDirectory(Path new_dir) {
        workingDirectory = new_dir;
    }

    @Override
    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public boolean mkdirs(Path f, FsPermission permission) throws IOException {
        return geds.mkdirs(bucket, computeGEDSPath(f));
    }

    @Override
    public FileStatus getFileStatus(Path f) throws IOException {
        GEDSFileStatus st = geds.status(bucket, computeGEDSPath(f));
        return new FileStatus(st.size, st.isDirectory, 1, blockSize, 0,
                new Path(st.key).makeQualified(getUri(), workingDirectory));
    }
}
