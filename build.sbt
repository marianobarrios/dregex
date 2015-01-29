name := "dregex"

scalaVersion := "2.11.4"

version := "0.1-SNAPSHOT"

libraryDependencies ++= 
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2" ::
  "org.scalatest" %% "scalatest" % "2.2.1" % "test" ::
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2" ::
  "ch.qos.logback" % "logback-classic" % "1.1.2" % Test ::
  Nil

unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil

unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil

 testOptions in Test += Tests.Argument("-oF")
 