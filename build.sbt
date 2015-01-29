organization := "dregex"

name := "dregex"

scalaVersion := "2.11.5"

version := "0.1-SNAPSHOT"

//publishTo := Some("bsas-snapshots" at "http://nexus.despegar.it:8080/nexus/content/repositories/snapshots/")
publishTo := Some("bsas-eleases" at "http://nexus.despegar.it:8080/nexus/content/repositories/releases/")
//publishTo := Some("miami-releases" at "http://nexus:8080/nexus/content/repositories/releases-miami/")
//publishTo := Some("miami-snapshots" at "http://nexus:8080/nexus/content/repositories/snapshots-miami/")

libraryDependencies ++= 
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2" ::
  "org.scalatest" %% "scalatest" % "2.2.1" % "test" ::
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2" ::
  "ch.qos.logback" % "logback-classic" % "1.1.2" % Test ::
  Nil

unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil

unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil

testOptions in Test += Tests.Argument("-oF")

parallelExecution in Test := false 