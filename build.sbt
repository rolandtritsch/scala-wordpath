seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

name := "Word Path Solution"

version := "0.1"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
  "com.weiglewilczek.slf4s" % "slf4s_2.9.1" % "1.0.7",
  "org.slf4j" % "slf4j-log4j12" % "1.6.4",
  "default" % "rolands-utilities-lib_2.9.1" % "0.1"
)
