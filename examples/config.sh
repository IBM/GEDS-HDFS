#!/usr/bin/env bash
#
# Copyright 2022- IBM Inc. All rights reserved
# SPDX-License-Identifier: Apache-2.0
#

# Spark installation
export SPARK_HOME="/home/${USER}/software/spark-3.3.1-bin-hadoop3"

# Kubernetes Server
export KUBERNETES_SERVER="https://9.4.244.122:6443"

# Image configuration
export DOCKER_REGISTRY="zac32.zurich.ibm.com"
export DOCKER_IMAGE_PREFIX="${PREFIX:-"${USER}/"}"

# S3 Config
export S3A_ENDPOINT="http://10.40.0.29:9000"
export S3A_ACCESS_KEY=${S3A_ACCESS_KEY:-$AWS_ACCESS_KEY_ID}
export S3A_SECRET_KEY=${S3A_SECRET_KEY:-$AWS_SECRET_ACCESS_KEY}
export S3A_OUTPUT_BUCKET=${S3A_BUCKET:-$S3A_ACCESS_KEY}

# Kubernetes Config
export KUBERNETES_PULL_SECRETS_NAME="zac-registry"
export KUBERNETES_NAMESPACE=$(kubectl config view --minify -o jsonpath='{..namespace}')
export KUBERNETES_SERVICE_ACCOUNT="${KUBERNETES_NAMESPACE}-manager"

# Datasets
## Terasort
export TERASORT_BUCKET=zrlio-terasort
