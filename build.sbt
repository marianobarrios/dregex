import sbt.Keys.scalacOptions

organization := "com.github.marianobarrios"
name := "dregex"
version := "0.7.0-SNAPSHOT"
description := "Deterministic Regular Expression Engine"
homepage := Some(url("https://github.com/marianobarrios/dregex"))
licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

scalaVersion := "2.13.8"
crossScalaVersions := Seq("2.11.12", "2.12.15", "2.13.7")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

scalacOptions := Seq(
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
  "-language:higherKinds",             // Allow higher-kinded types
  "-language:implicitConversions",     // Allow definition of implicit functions called views
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
)

libraryDependencies ++= 
  "org.slf4j" % "slf4j-api" % "2.0.3" ::
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1" ::
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.6.0" ::
  "org.scalatest" %% "scalatest-funsuite" % "3.2.13" % Test ::
  "ch.qos.logback" % "logback-classic" % "1.2.11" % Test ::
  Nil

// Do not include src/{main,test}/java in the configuration, to avoid having sbt-eclipse generate them empty
Compile / unmanagedSourceDirectories := (Compile / scalaSource).value :: Nil
Test / unmanagedSourceDirectories := (Test / scalaSource).value :: Nil

Compile / doc / scalacOptions ++= Seq(
  "-doc-root-content", baseDirectory.value + "/root-doc.txt",
  "-skip-packages", "dregex.impl:dregex.extra",
)

Test / testOptions += Tests.Argument("-oF")
Test / parallelExecution := false
Test / fork := true

publishMavenStyle := true

pomExtra :=
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
  </developers>
