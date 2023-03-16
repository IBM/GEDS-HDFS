//
// Copyright 2022- IBM Inc. All rights reserved
// SPDX-License-Identifier: Apache-2.0
//

scalaVersion := sys.env.getOrElse("SCALA_VERSION", "2.12.15")
organization := "com.ibm"
name := "geds-hdfs"
version := "SNAPSHOT"

val gedsApiVersion = "1.0"
val gedsInstallPath = sys.env.getOrElse("GEDS_INSTALL", "/home/psp/geds-install")
val hadoopVersion = sys.env.getOrElse("HADOOP_VERSION", "3.3.4")

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-common" % hadoopVersion % "provided",
  "com.ibm.geds" % "geds" % gedsApiVersion from "file://"+gedsInstallPath+"/java/geds-"+gedsApiVersion+".jar",
  "junit" % "junit" % "4.13.2" % Test, // TRAVIS_SCALA_WORKAROUND_REMOVE_LINE
)

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")
scalacOptions ++= Seq("-deprecation", "-unchecked")

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "_" +"hadoop" + hadoopVersion + "-geds" + gedsApiVersion + "_" + module.revision + "." + artifact.extension
}

assemblyMergeStrategy := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
assembly / assemblyJarName := s"${name.value}_geds${gedsApiVersion}_${version.value}-with-dependencies.jar"
assembly / assemblyOption ~= {
  _.withIncludeScala(false)
}

lazy val lib = (project in file("."))

