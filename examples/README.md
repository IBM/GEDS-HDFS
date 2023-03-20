# Readme

This folder contains a set of examples for the HDFS connector.

## Prequisites

1. A Apache Spark installation for Scala 2.12 (3.3.1 or above)
2. A Kubernetes Cluster
3. A S3 Bucket / or a local S3 installation (see `config.sh` for the configuration). 
4. Configure `s3cmd` with the S3 credentials to enable cleanup.

## Steps

Modify `config.sh` based on your setup.

## Running
```
source config.sh
./BENCHMARK/run.sh
```

Use the following environment variables to configure the Spark:
- `USE_S3_SHUFFLE` Enable/Disable Shuffle on S3 (default: off)
- `USE_GEDS_SHUFFLE` Enable/Disable Shuffle on GEDS (default: off)
- `USE_GEDS_INPUT` Enable/Disable GEDS input caching (default: off)
