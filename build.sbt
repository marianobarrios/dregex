name := "dregex"

scalaVersion := "2.11.4"

version := "0.1-SNAPSHOT"

libraryDependencies ++= 
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2" ::
  Nil

unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil

unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil

