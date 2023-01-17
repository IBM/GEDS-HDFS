//
// Copyright 2022- IBM Inc. All rights reserved
// SPDX-License-Identifier: Apache-2.0
//

package com.ibm.geds.hdfs;

import com.ibm.geds.GEDS;
import org.apache.hadoop.conf.Configuration;

public class GEDSInstance {
    private static GEDS instance;

    public synchronized static GEDS initialize(Configuration conf) {
        if (instance == null) {
            String metadataServer = conf.get(Constants.METADATA_SERVER, "localhost:" + GEDS.getDefaultMetdataServerPort());
            String path = conf.get(Constants.PATH, "/tmp/GEDSHadoop");
            int port = conf.getInt(Constants.PORT, 0); // 0 indicates use GEDS default.
            long blockSize = conf.getLong(Constants.BLOCKSIZE, Constants.DEFAULT_BLOCKSIZE); // 0 indicates use GEDS default.
            instance = new GEDS(metadataServer, path, port, blockSize);
        }
        return instance;
    }

    public synchronized static GEDS initialize(String bucket, Configuration conf) {
        GEDS geds = initialize(conf);

        String bucketAccessKey = conf.get(Constants.GEDS_PREFIX + bucket + ".accessKey");
        String bucketSecretKey = conf.get(Constants.GEDS_PREFIX + bucket + ".secretKey");
        String bucketEndpoint = conf.get(Constants.GEDS_PREFIX + bucket + ".endpoint");

        boolean hasAccessKey = bucketAccessKey != null;
        boolean hasSecretKey = bucketSecretKey != null;
        boolean hasBucketEndpoint = bucketEndpoint != null;
        if (hasAccessKey && hasSecretKey && hasBucketEndpoint) {
            geds.registerObjectStoreConfig(bucket, bucketEndpoint, bucketAccessKey, bucketSecretKey);
        } else if (hasAccessKey || hasSecretKey || hasBucketEndpoint) {
            throw new RuntimeException("Bucket " + bucket + " has either an accessKey, secretKey or an endpoint registered. To map the bucket to S3 all variables need to be configured.");
        }
        return geds;
    }
}
