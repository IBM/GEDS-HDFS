//
// Copyright 2022- IBM Inc. All rights reserved
// SPDX-License-Identifier: Apache-2.0
//

package com.ibm.geds.hdfs;

public class Constants {
    public static final String GEDS_IMPLEMENTATION = "com.ibm.geds.hdfs.GEDSHadoopFileSystem";

    public static final String GEDS_PREFIX = "fs.geds.";
    public static final String METADATA_SERVER = GEDS_PREFIX + "metadataserver";
    public static final String PORT = GEDS_PREFIX + "port";
    public static final String PATH = GEDS_PREFIX + "path";

    public static final String BLOCKSIZE = GEDS_PREFIX + "blocksize";
    public static final long DEFAULT_BLOCKSIZE = 64 * 1024 * 1024;
}
