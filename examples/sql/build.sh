#!/usr/bin/env bash
#
# Copyright 2022- IBM Inc. All rights reserved
# SPDX-License-Identifier: Apache-2.0
#
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "${SCRIPT_DIR}"
ROOT="$(pwd)"

REGISTRY="${REGISTRY:-zac32.zurich.ibm.com}"
PREFIX="${PREFIX:-${USER}}"

IMAGE="${REGISTRY}/${PREFIX}/spark-sql:latest"
docker build -t "${IMAGE}" .
docker push "${IMAGE}"
