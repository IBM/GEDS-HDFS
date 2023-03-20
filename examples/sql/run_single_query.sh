#!/usr/bin/env bash
#
# Copyright 2022- IBM Inc. All rights reserved
# SPDX-License-Identifier: Apache-2.0
#
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "${SCRIPT_DIR}"
ROOT="$(pwd)"

QUERY=$1
SIZE=${SIZE:-1000}
TIMESTAMP=$(date -u "+%FT%H%M%SZ")
PROCESS_TAG=${PROCESS_TAG:-"sql"}
PROCESS_TAG="${PROCESS_TAG}-${SIZE}-${TIMESTAMP}"

# Shuffle on S3
export USE_S3_SHUFFLE=${USE_S3_SHUFFLE:-0}
# Shuffle on GEDS
export USE_GEDS_SHUFFLE=${USE_GEDS_SHUFFLE:-0}
# Use GEDS input caching
export USE_GEDS_INPUT=${USE_GEDS_INPUT:-0}

USE_GEDS=0
HADOOP_PROTOCOL=s3a
SHUFFLE_PREFIX=s3a://${S3A_OUTPUT_BUCKET}/shuffle

if (( "${USE_GEDS_INPUT:-0}" )); then
    HADOOP_PROTOCOL=geds
    USE_GEDS=1
fi
if (( "${USE_GEDS_SHUFFLE}" == 1 )); then
    USE_GEDS=1
    SHUFFLE_PREFIX=geds://${S3A_OUTPUT_BUCKET}/shuffle
fi

if (( "${USE_S3_SHUFFLE}" == 1 )); then
    PROCESS_TAG="${PROCESS_TAG}-s3shuffle"
fi
if (( "${USE_GEDS}" == 1 )); then
    PROCESS_TAG="${PROCESS_TAG}-geds"
fi

export PROCESS_TAG=${PROCESS_TAG}
export SHUFFLE_PREFIX=${SHUFFLE_PREFIX}

INPUT_DATA_PREFIX=${HADOOP_PROTOCOL}://${TPCDS_BUCKET}
OUTPUT_DATA_PREFIX=s3a://${S3A_OUTPUT_BUCKET}/output/sql-benchmarks/

./run_benchmark.sh \
    -t $QUERY \
    -i $INPUT_DATA_PREFIX/sf${SIZE}_parquet/ \
    -a save,${OUTPUT_DATA_PREFIX}/${QUERY}/${PROCESS_TAG}/${SIZE}
