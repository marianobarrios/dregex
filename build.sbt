organization := "com.github.marianobarrios"
name := "dregex"
version := "0.4.0-SNAPSHOT"
description := "Deterministic Regular Expressions Engine"
homepage := Some(url("https://github.com/marianobarrios/dregex"))
licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

scalaVersion := "2.12.2"
crossScalaVersions := Seq("2.11.8", "2.12.2")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

scalacOptions := Seq(
	"-feature",
	"-unchecked",
  "-deprecation",
	"-language:reflectiveCalls",
	"-language:implicitConversions", 
	"-Ywarn-dead-code",
	"-Ywarn-inaccessible",
	"-Ywarn-nullary-unit",
	"-Ywarn-nullary-override",
  "-Ywarn-unused-import",
  "-Xfuture",
  "-Xfatal-warnings")
	
libraryDependencies ++= 
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.1" ::
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6" ::
  "org.scalatest" %% "scalatest" % "3.0.2" % Test ::
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test ::
  Nil

// Do not include src/{main,test}/java in the configuration, to avoid having sbt-eclipse generate them empty
unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil
unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil

scalacOptions in (Compile, doc) ++= Seq("-doc-root-content", baseDirectory.value + "/root-doc.txt") 

testOptions in Test += Tests.Argument("-oF")
parallelExecution in Test := false 
fork in Test := true

publishMavenStyle := true

pomExtra := (
  <scm>
    <url>git@github.com:marianobarrios/dregex.git</url>
    <connection>scm:git:git@github.com:marianobarrios/dregex.git</connection>
    <developerConnection>scm:git:git@github.com:marianobarrios/dregex.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <name>Mariano Barrios</name>
      <url>https://github.com/marianobarrios/</url>
    </developer>
  </developers>)
