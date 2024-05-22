//
// Copyright 2022- IBM Inc. All rights reserved
// SPDX-License-Identifier: Apache-2.0
//

package com.ibm.geds.hdfs;

import com.ibm.geds.GEDS;
import com.ibm.geds.GEDSConfig;

import org.apache.hadoop.conf.Configuration;

public class GEDSInstance {
    private static boolean configureS3usingEnv;
    private static GEDS instance;
    private static GEDSConfig instanceConfig;

    private static String getEnv(String key) {
        return getEnv(key, "");
    }

    private static String getEnv(String key, String defaultValue) {
        try {
            String value = System.getenv(key);
            if (value == null) {
                return defaultValue;
            }
            return value;
        } catch (Exception e) {
            System.err.println("Unable to parse " + key + ": " + e.getMessage());
            return defaultValue;
        }
    }

    private static long getEnvLong(String key, long defaultValue) {
        try {
            String value = System.getenv(key);
            if (value == null || value == "") {
                return defaultValue;
            }
            return Long.parseLong(value);
        } catch (Exception e) {
            System.err.println("Unable to parse " + key + ": " + e.getMessage());
            return defaultValue;
        }
    }

    public synchronized static GEDS initialize(Configuration conf) {
        if (instance == null) {
            GEDSConfig gedsConfig = getConfig(conf);
            instance = new GEDS(gedsConfig);
        }
        return instance;
    }

    public synchronized static GEDSConfig getConfig(Configuration conf) {
        if (instanceConfig == null) {
            String metadataServer = conf.get(Constants.GEDS_PREFIX + Constants.METADATA_SERVER,
                    getEnv("GEDS_METADATASERVER", "localhost:4381"));
            instanceConfig = new GEDSConfig(metadataServer);

            // Use defaults from GEDS.
            String local_storage_path = conf.get(Constants.GEDS_PREFIX + Constants.LOCAL_STORAGE_PATH,
                    getEnv("GEDS_LOCAL_STORAGE_PATH", ""));
            if (local_storage_path != "") {
                instanceConfig.set(Constants.LOCAL_STORAGE_PATH, local_storage_path);
            }
            int port = conf.getInt(Constants.GEDS_PREFIX + Constants.PORT,
                    (int) getEnvLong("GEDS_PORT", 0));
            if (port != 0) {
                instanceConfig.set(Constants.PORT, port);
            }

            long cache_block_size = conf.getLong(Constants.GEDS_PREFIX + Constants.CACHE_BLOCK_SIZE,
                    getEnvLong("GEDS_CACHE_BLOCK_SIZE", 0));
            if (cache_block_size != 0) {
                instanceConfig.set(Constants.CACHE_BLOCK_SIZE, cache_block_size);
            }

            int http_server_port = conf.getInt(Constants.GEDS_PREFIX + Constants.HTTP_SERVER_PORT,
                    (int) getEnvLong("GEDS_HTTP_SERVER_PORT", 0));
            if (http_server_port != 0) {
                instanceConfig.set(Constants.HTTP_SERVER_PORT, http_server_port);
            }
            long available_local_memory = conf.getLong(Constants.GEDS_PREFIX + Constants.AVAILABLE_LOCAL_MEMORY,
                    getEnvLong("GEDS_AVAILABLE_LOCAL_MEMORY", 0));
            if (available_local_memory != 0) {
                instanceConfig.set(Constants.AVAILABLE_LOCAL_MEMORY, available_local_memory);
            }
            long available_local_storage = conf.getLong(Constants.GEDS_PREFIX + Constants.AVAILABLE_LOCAL_STORAGE,
                    getEnvLong("GEDS_AVAILABLE_LOCAL_STORAGE", 0));
            if (available_local_storage != 0) {
                instanceConfig.set(Constants.AVAILABLE_LOCAL_STORAGE, available_local_storage);
            }
            long io_thread_pool_size = conf.getLong(Constants.GEDS_PREFIX + Constants.IO_THREAD_POOL_SIZE,
                    getEnvLong("GEDS_IO_THREAD_POOL_SIZE", 0));
            if (io_thread_pool_size != 0) {
                instanceConfig.set(Constants.IO_THREAD_POOL_SIZE, io_thread_pool_size);
            }

            configureS3usingEnv = conf.getLong(Constants.GEDS_PREFIX + Constants.CONFIGURE_S3_USING_ENV,
                    getEnvLong("GEDS_CONFIGURE_S3_USING_ENV", 0)) == 1;
        }
        return instanceConfig;
    }

    public synchronized static GEDS initialize(String bucket, Configuration conf) {
        GEDS geds = initialize(conf);

        try {
            geds.createBucket(bucket);
        } catch (Exception e) {
            // Always initialize bucket.
        }
        if (configureS3usingEnv) {
            String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
            String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
            String endpoint = System.getenv("AWS_ENDPOINT_URL");

            boolean hasAccessKey = accessKey != null;
            boolean hasSecretKey = secretKey != null;
            boolean hasEndpoint = endpoint != null;
            if (hasAccessKey && hasSecretKey && hasEndpoint) {
                geds.registerObjectStoreConfig(bucket, endpoint, accessKey, secretKey);
            } else if (hasAccessKey || hasSecretKey || hasEndpoint) {
                throw new RuntimeException(Constants.CONFIGURE_S3_USING_ENV
                        + "is enabled and either AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY or AWS_ENDPOINT_URL is set.");
            }
        } else {
            String bucketAccessKey = conf.get(Constants.GEDS_PREFIX + bucket + ".accessKey",
                    getEnv("AWS_ACCESS_KEY_ID"));
            String bucketSecretKey = conf.get(Constants.GEDS_PREFIX + bucket + ".secretKey",
                    getEnv("AWS_ACCESS_KEY_ID"));
            String bucketEndpoint = conf.get(Constants.GEDS_PREFIX + bucket + ".endpoint", getEnv("AWS_ENDPOINT_URL"));

            boolean hasAccessKey = bucketAccessKey != null;
            boolean hasSecretKey = bucketSecretKey != null;
            boolean hasBucketEndpoint = bucketEndpoint != null;
            if (hasAccessKey && hasSecretKey && hasBucketEndpoint) {
                geds.registerObjectStoreConfig(bucket, bucketEndpoint, bucketAccessKey, bucketSecretKey);
            } else if (hasAccessKey || hasSecretKey || hasBucketEndpoint) {
                throw new RuntimeException("Bucket " + bucket
                        + " has either an accessKey, secretKey or an endpoint registered. To map the bucket to S3 all variables need to be configured.");
            }
        }
        return geds;
    }
}
