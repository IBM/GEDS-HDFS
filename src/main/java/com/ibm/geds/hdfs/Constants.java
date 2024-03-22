//
// Copyright 2022- IBM Inc. All rights reserved
// SPDX-License-Identifier: Apache-2.0
//

package com.ibm.geds.hdfs;

public class Constants {
    public static final String GEDS_IMPLEMENTATION = "com.ibm.geds.hdfs.GEDSHadoopFileSystem";

    public static final String GEDS_PREFIX = "fs.geds.";
    public static final String METADATA_SERVER = "metadataserver";
    public static final String PORT = "port";
    public static final String LOCAL_STORAGE_PATH = "local_storage_path";
    public static final String CACHE_BLOCK_SIZE = "cache_block_size";
    public static final String HTTP_SERVER_PORT = "http_server_port";
    public static final String AVAILABLE_LOCAL_STORAGE = "available_local_storage";
    public static final String AVAILABLE_LOCAL_MEMORY = "available_local_memory";
    public static final String CACHE_OBJECTS_FROM_S3 = "cache_objects_from_s3";
    public static final String FORCE_RELOCATION_WHEN_STOPPING = "force_relocation_when_stopping";
    public static final String IO_THREAD_POOL_SIZE = "io_thread_pool_size";
    public static final String STORAGE_SPILLING_FACTION = "storage_spilling_fraction";
}
