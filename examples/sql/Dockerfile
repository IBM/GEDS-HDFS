#!/usr/bin/env bash
#
# Copyright 2022- IBM Inc. All rights reserved
# SPDX-License-Identifier: Apache-2.0
#

FROM debian:11.5 as java-prerequisites

ENV DEBIAN_FRONTEND noninteractive
ENV DEBCONF_NONINTERACTIVE_SEEN true

RUN apt-get clean && apt-get update \
    && apt-get install -y --no-install-recommends \
        openjdk-11-jdk-headless git maven wget \
    && apt-get clean

###
# SQL-Benchmarks
FROM java-prerequisites as sql-builder

RUN git clone https://github.com/zrlio/sql-benchmarks \
    && cd sql-benchmarks \
    && mvn package

# Result /sql-benchmarks/target/sql-benchmarks-1.0.jar

### 
# GEDS-HDFS
FROM java-prerequisites as geds-hdfs-builder

ARG GEDS_VERSION=0.8.2
RUN wget -nv "https://github.com/IBM/GEDS/releases/download/v${GEDS_VERSION}/geds-x86_64-debian11.5-${GEDS_VERSION}-Release.tar.gz" \
    && tar xf "geds-x86_64-debian11.5-${GEDS_VERSION}-Release.tar.gz" -C /usr/local/ \
    && rm "geds-x86_64-debian11.5-${GEDS_VERSION}-Release.tar.gz"

RUN wget -nv https://repo.scala-sbt.org/scalasbt/debian/sbt-1.8.2.deb \
    && dpkg -i sbt-1.8.2.deb \
    && rm sbt-1.8.2.deb

ENV GEDS_INSTALL=/usr/local/geds
RUN git clone https://github.com/IBM/GEDS-HDFS \
    && cd GEDS-HDFS \
    && sbt package
# Output /GEDS-HDFS/target/scala-2.12/geds-hdfs_hadoop3.3.4-geds1.0_SNAPSHOT.jar

###
# Spark Container
FROM docker.io/apache/spark-py:3.3.1

ARG GEDS_VERSION=0.8.2
ARG HADOOP_VERSION=3.3.2
ARG AWS_SDK_VERSION=1.11.1026
ARG STOCATOR_VERSION=1.1.5

USER root
RUN apt-get update \
    && apt-get install -y \
        openssl curl git wget \
    && apt-get clean autoclean \
    && apt-get autoremove --yes

RUN cd "${SPARK_HOME}/jars/" \
    && wget -nv https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_VERSION}/hadoop-aws-${HADOOP_VERSION}.jar  \
    && wget -nv https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-cloud-storage/${HADOOP_VERSION}/hadoop-cloud-storage-${HADOOP_VERSION}.jar  \
    && wget -nv https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/${AWS_SDK_VERSION}/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar \
    && wget -nv https://repo1.maven.org/maven2/com/ibm/stocator/stocator/${STOCATOR_VERSION}/stocator-${STOCATOR_VERSION}.jar \
    && wget -nv https://github.com/IBM/spark-s3-shuffle/releases/download/v0.6.2/spark-s3-shuffle_2.12-3.3.1_0.6.2.jar \
    && chmod +r *

# SQL
COPY --from=sql-builder /sql-benchmarks/target/sql-benchmarks-1.0.jar "${SPARK_HOME}/jars/"

# GEDS
COPY --from=geds-hdfs-builder /usr/local/geds "/usr/local/"
COPY --from=geds-hdfs-builder /usr/local/geds/java/geds.jar "${SPARK_HOME}/jars/"

# GEDS-HDFS
COPY --from=geds-hdfs-builder /GEDS-HDFS/target/scala-2.12/geds-hdfs_hadoop3.3.4-geds1.0_SNAPSHOT.jar "${SPARK_HOME}/jars/"

ENV LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/usr/local/lib:/usr/local/java

USER ${SPARK_USER}
