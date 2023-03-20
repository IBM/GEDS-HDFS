#!/usr/bin/env bash
#
# Copyright 2022- IBM Inc. All rights reserved
# SPDX-License-Identifier: Apache-2.0
#

# Download completed logs.
while true; do
    date
    PODS=$(kubectl get pods | grep "Completed" | awk '{print $1}' | grep "\-driver")
    for pod in $PODS; do
        echo $pod
        kubectl logs $pod > $pod &
    done

    wait

    for pod in $PODS; do
        kubectl delete pod $pod &
    done

    wait

    sleep 100
done