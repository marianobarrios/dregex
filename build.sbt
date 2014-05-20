name := "dregex"

scalaVersion := "2.10.4"

version := "0.1-SNAPSHOT"

libraryDependencies ++= 
  Nil

unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil

unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil

