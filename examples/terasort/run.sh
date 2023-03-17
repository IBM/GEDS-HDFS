#!/usr/bin/env bash
#
# Copyright 2022- IBM Inc. All rights reserved
# SPDX-License-Identifier: Apache-2.0
#
set -euo pipefail
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "${SCRIPT_DIR}"

ROOT="$(pwd)"
TIMESTAMP=$(date -u "+%FT%H%M%SZ")

IMAGE="${DOCKER_REGISTRY}/${DOCKER_IMAGE_PREFIX}spark-terasort:latest"

PROCESS_TAG="-${TIMESTAMP}"

DRIVER_CPU=${DRIVER_CPU:-4}
DRIVER_MEM=${DRIVER_MEM:-13000M}
DRIVER_MEMORY_OVERHEAD=${DRIVER_MEMORY_OVERHEAD:-3000M}
EXECUTOR_CPU=${EXECUTOR_CPU:-4}
EXECUTOR_MEM=${EXECUTOR_MEM:-13000M}
EXECUTOR_MEMORY_OVERHEAD=${EXECUTOR_MEMORY_OVERHEAD:-3000M}
INSTANCES=${INSTANCES:-4}
SIZE=${SIZE:-1g}

USE_GEDS=0

# Shuffle on S3
USE_S3_SHUFFLE=${USE_S3_SHUFFLE:-0}
# Shuffle on GEDS
USE_GEDS_SHUFFLE=${USE_GEDS_SHUFFLE:-0}

# Use GEDS input caching
USE_GEDS_INPUT=${USE_GEDS_INPUT:-0}

EXTRA_CLASSPATHS='/opt/spark/jars/*'
EXECUTOR_JAVA_OPTIONS="-Dsun.nio.PageAlignDirectMemory=true"
DRIVER_JAVA_OPTIONS="-Dsun.nio.PageAlignDirectMemory=true"

export SPARK_EXECUTOR_CORES=$EXECUTOR_CPU
export SPARK_DRIVER_MEMORY=$DRIVER_MEM
export SPARK_EXECUTOR_MEMORY=$EXECUTOR_MEM

SHUFFLE_DATA_LOCATION=${S3A_OUTPUT_BUCKET}/shuffle

export SPARK_HADOOP_S3A_CONFIG=(
    # Required
    --conf spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
    --conf spark.hadoop.fs.s3a.access.key=${S3A_ACCESS_KEY}
    --conf spark.hadoop.fs.s3a.secret.key=${S3A_SECRET_KEY}
    --conf spark.hadoop.fs.s3a.connection.ssl.enabled=false
    --conf spark.hadoop.fs.s3a.endpoint=${S3A_ENDPOINT}
    --conf spark.hadoop.fs.s3a.path.style.access=true
    --conf spark.hadoop.fs.s3a.fast.upload=true
    --conf spark.hadoop.fs.s3a.block.size=$((32*1024*1024))
)

SPARK_HADOOP_GEDS_CONFIG=(
  --conf spark.hadoop.fs.geds.impl="com.ibm.geds.hdfs.GEDSHadoopFileSystem"
  --conf spark.hadoop.fs.geds.metadataserver="geds-service:4381"
  --conf spark.hadoop.fs.geds.blocksize=$((32*1024*1024))
  --conf spark.hadoop.fs.geds.path=/geds
  --conf spark.hadoop.fs.geds.${TERASORT_BUCKET}.accessKey="${S3A_ACCESS_KEY}"
  --conf spark.hadoop.fs.geds.${TERASORT_BUCKET}.secretKey="${S3A_SECRET_KEY}"
  --conf spark.hadoop.fs.geds.${TERASORT_BUCKET}.endpoint="${S3A_ENDPOINT}"
)

HADOOP_PROTOCOL=s3a
HADOOP_PROTOCOL_SHUFFLE=s3a
if (( "${USE_GEDS_INPUT:-0}" )); then
    USE_GEDS=1
    HADOOP_PROTOCOL=geds
fi

if (( "$USE_GEDS_SHUFFLE" == 1 )); then
    USE_GEDS=1
    HADOOP_PROTOCOL_SHUFFLE=geds
fi

SPARK_S3_SHUFFLE_CONFIG=(
    --conf spark.hadoop.fs.s3a.access.key=${S3A_ACCESS_KEY}
    --conf spark.hadoop.fs.s3a.secret.key=${S3A_SECRET_KEY}
    --conf spark.hadoop.fs.s3a.endpoint=${S3A_ENDPOINT}
    --conf spark.shuffle.s3.useBlockManager=${USE_BLOCK_MANAGER:-false}
    --conf spark.shuffle.manager="org.apache.spark.shuffle.sort.S3ShuffleManager"
    --conf spark.shuffle.sort.io.plugin.class=org.apache.spark.shuffle.S3ShuffleDataIO
    --conf spark.shuffle.checksum.enabled=false
    --conf spark.shuffle.s3.rootDir=${HADOOP_PROTOCOL_SHUFFLE}://${SHUFFLE_DATA_LOCATION}
)

if (( "$USE_S3_SHUFFLE" == 0 )) && (( "$USE_GEDS_SHUFFLE" == 0 )); then
    SPARK_S3_SHUFFLE_CONFIG=(
            --conf spark.shuffle.s3.rootDir=s3a://${SHUFFLE_DATA_LOCATION}
    )
else
    PROCESS_TAG="${PROCESS_TAG}-s3shuffle"
fi

if (( "$USE_GEDS" == 0 )); then
    SPARK_HADOOP_GEDS_CONFIG=(
        --conf spark.hadoop.fs.geds.blocksize=$((32*1024*1024))
    )
else
    PROCESS_TAG="${PROCESS_TAG}-geds"
fi

${SPARK_HOME}/bin/spark-submit \
    --master k8s://$KUBERNETES_SERVER \
    --deploy-mode cluster \
    \
        --conf "spark.driver.extraJavaOptions=${DRIVER_JAVA_OPTIONS}" \
        --conf "spark.executor.extraJavaOptions=${EXECUTOR_JAVA_OPTIONS}" \
    \
    --name ce-terasort-${SIZE}${PROCESS_TAG}-${INSTANCES}x${EXECUTOR_CPU}--${EXECUTOR_MEM} \
    --conf spark.serializer="org.apache.spark.serializer.KryoSerializer" \
    --conf spark.kryoserializer.buffer=128mb \
    --conf spark.executor.instances=$INSTANCES \
    "${SPARK_HADOOP_S3A_CONFIG[@]}" \
    "${SPARK_HADOOP_GEDS_CONFIG[@]}" \
    "${SPARK_S3_SHUFFLE_CONFIG[@]}" \
    --conf spark.ui.prometheus.enabled=true \
    --conf spark.network.timeout=10000 \
    --conf spark.executor.heartbeatInterval=20000 \
    --conf spark.kubernetes.appKillPodDeletionGracePeriod=5 \
    --conf spark.kubernetes.container.image.pullSecrets=${KUBERNETES_PULL_SECRETS_NAME} \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=${KUBERNETES_SERVICE_ACCOUNT} \
    --conf spark.kubernetes.driver.podTemplateFile=${ROOT}/../templates/driver.yml \
    --conf spark.kubernetes.executor.podTemplateFile=${ROOT}/../templates/executor.yml \
    --conf spark.kubernetes.container.image.pullPolicy=Always \
    --conf spark.driver.memoryOverhead=$DRIVER_MEMORY_OVERHEAD \
    --conf spark.kubernetes.driver.request.cores=$DRIVER_CPU \
    --conf spark.kubernetes.driver.limit.cores=$DRIVER_CPU \
    --conf spark.executor.memoryOverhead=$EXECUTOR_MEMORY_OVERHEAD \
    --conf spark.kubernetes.executor.request.cores=$EXECUTOR_CPU \
    --conf spark.kubernetes.executor.limit.cores=$EXECUTOR_CPU \
    --conf spark.kubernetes.container.image=$IMAGE \
    --conf spark.kubernetes.namespace=$KUBERNETES_NAMESPACE \
    --class com.github.ehiggs.spark.terasort.TeraSort \
    local:///opt/spark/jars/spark-terasort-1.2-SNAPSHOT.jar \
    "${HADOOP_PROTOCOL}://${TERASORT_BUCKET}/${SIZE}" "s3a://${S3A_OUTPUT_BUCKET}/output/terasort/${SIZE}-${PROCESS_TAG}"

${SPARK_HOME}/bin/spark-submit \
    --master k8s://$KUBERNETES_SERVER \
    --deploy-mode cluster \
    \
        --conf "spark.driver.extraJavaOptions=${DRIVER_JAVA_OPTIONS}" \
        --conf "spark.executor.extraJavaOptions=${EXECUTOR_JAVA_OPTIONS}" \
    \
    --name ce-teravalidate-${SIZE}-${PROCESS_TAG}-${INSTANCES}x${EXECUTOR_CPU}--${EXECUTOR_MEM} \
    --conf spark.executor.instances=$INSTANCES \
    --conf spark.jars.ivy=/tmp/.ivy \
    --conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
    --conf spark.kryoserializer.buffer=128mb \
    "${SPARK_HADOOP_S3A_CONFIG[@]}" \
    --conf spark.ui.prometheus.enabled=true \
    --conf spark.network.timeout=10000 \
    --conf spark.executor.heartbeatInterval=20000 \
    --conf spark.kubernetes.appKillPodDeletionGracePeriod=5 \
    --conf spark.kubernetes.container.image.pullSecrets=${KUBERNETES_PULL_SECRETS_NAME} \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=${KUBERNETES_SERVICE_ACCOUNT} \
    --conf spark.kubernetes.driver.podTemplateFile=${ROOT}/../templates/driver.yml \
    --conf spark.kubernetes.executor.podTemplateFile=${ROOT}/../templates/executor.yml \
    --conf spark.kubernetes.container.image.pullPolicy=Always \
    --conf spark.driver.memoryOverhead=$DRIVER_MEMORY_OVERHEAD \
    --conf spark.kubernetes.driver.request.cores=$DRIVER_CPU \
    --conf spark.kubernetes.driver.limit.cores=$DRIVER_CPU \
    --conf spark.kubernetes.executor.request.cores=$EXECUTOR_CPU \
    --conf spark.kubernetes.executor.limit.cores=$EXECUTOR_CPU \
    --conf spark.executor.memoryOverhead=$EXECUTOR_MEMORY_OVERHEAD \
    --conf spark.kubernetes.container.image=$IMAGE \
    --conf spark.kubernetes.namespace=$KUBERNETES_NAMESPACE \
    --class com.github.ehiggs.spark.terasort.TeraValidate \
    local:///opt/spark/jars/spark-terasort-1.2-SNAPSHOT.jar \
       "s3a://${S3A_OUTPUT_BUCKET}/output/terasort/${SIZE}-${PROCESS_TAG}" "s3a://${S3A_OUTPUT_BUCKET}/output/terasort-validated/${SIZE}-${PROCESS_TAG}"

mc -r -force ${S3A_REGION}/${S3A_OUTPUT_BUCKET}/output/terasort/${SIZE}-${PROCESS_TAG} || true
mc -r -force ${S3A_REGION}/${SHUFFLE_DATA_LOCATION} || true
