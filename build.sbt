organization := "dregex"

name := "dregex"

crossScalaVersions := Seq("2.10.4", "2.11.5")

version := "0.1-RC2"

//publishTo := Some("bsas-snapshots" at "http://nexus.despegar.it:8080/nexus/content/repositories/snapshots/")
publishTo := Some("bsas-releases" at "http://nexus.despegar.it:8080/nexus/content/repositories/releases/")
//publishTo := Some("miami-releases" at "http://nexus:8080/nexus/content/repositories/releases-miami/")
//publishTo := Some("miami-snapshots" at "http://nexus:8080/nexus/content/repositories/snapshots-miami/")

libraryDependencies ++= 
  "org.scalatest" %% "scalatest" % "2.2.1" % "test" ::
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2" ::
  "ch.qos.logback" % "logback-classic" % "1.1.2" % Test ::
  Nil

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, major)) if major >= 11 =>
      libraryDependencies.value ++ Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2")
    case _ =>
      libraryDependencies.value
  }
}

unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil

unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil

testOptions in Test += Tests.Argument("-oF")

parallelExecution in Test := false 