//
// Copyright 2022- IBM Inc. All rights reserved
// SPDX-License-Identifier: Apache-2.0
//

scalaVersion := sys.env.getOrElse("SCALA_VERSION", "2.12.15")
organization := "com.ibm"
name := "geds-hdfs"
version := "SNAPSHOT"

val gedsApiVersion = "1.3"
val userHome = sys.env.getOrElse("HOME", "/home/psp")
val gedsInstallPath = sys.env.getOrElse("GEDS_INSTALL", userHome+"/geds-install")
val hadoopVersion = sys.env.getOrElse("HADOOP_VERSION", "3.3.4")

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-common" % hadoopVersion % "provided",
  "com.ibm.geds" % "geds" % gedsApiVersion from "file://"+gedsInstallPath+"/java/geds-"+gedsApiVersion+".jar"
)

libraryDependencies ++= (if (scalaBinaryVersion.value == "2.12") Seq(
  "org.scalatest" %% "scalatest" % "3.2.2" % Test,
  "org.junit.jupiter" % "junit-jupiter-engine" % "5.10.2" % Test,
  "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test
)
else Seq())

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")
scalacOptions ++= Seq("-deprecation", "-unchecked")

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "_" + sv.binary +  "-geds" + gedsApiVersion + "-hadoop" + hadoopVersion + "_" + module.revision + "." + artifact.extension
}

assemblyMergeStrategy := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
assembly / assemblyJarName := s"${name.value}_${scalaBinaryVersion.value}-geds${gedsApiVersion}-hadoop${hadoopVersion}_${version.value}-with-dependencies.jar"
assembly / assemblyOption ~= {
  _.withIncludeScala(false)
}

lazy val lib = (project in file("."))

