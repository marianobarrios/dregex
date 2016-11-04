organization := "com.github.marianobarrios"
name := "dregex"
version := "0.4.0-SNAPSHOT"
description := "Deterministic Regular Expressions Engine"
homepage := Some(url("https://github.com/marianobarrios/dregex"))
licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

scalaVersion := "2.11.8"
crossScalaVersions := Seq("2.10.6", "2.11.8")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

scalacOptions := Seq(
	"-feature", 
	"-deprecation", 
	"-optimize",
	"-unchecked",
	"-language:postfixOps", 
	"-language:reflectiveCalls", 
	"-language:implicitConversions", 
	"-Ywarn-dead-code",
	"-Ywarn-inaccessible",
	"-Ywarn-nullary-unit",
	"-Ywarn-nullary-override")
	
libraryDependencies ++= 
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2" ::
  "org.scalatest" %% "scalatest" % "3.0.0" % "test" ::
  "ch.qos.logback" % "logback-classic" % "1.1.7" % Test ::
  Nil

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, major)) if major >= 11 =>
      libraryDependencies.value ++ Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4")
    case _ =>
      libraryDependencies.value
  }
}

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
