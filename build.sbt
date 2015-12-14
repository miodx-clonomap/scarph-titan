Nice.scalaProject

name          := "scarph-titan"
description   := "Scarph evaluators for TitanDB"
organization  := "ohnosequences"
bucketSuffix  := "era7.com"

scalaVersion        := "2.11.7"
// crossScalaVersions  := Seq("2.10.6")

val titanVersion = "0.5.4"
val scarphVersion = "0.3.0"

libraryDependencies ++= Seq(
  "ohnosequences"           %% "cosas"            % "0.8.0",
  "com.thinkaurelius.titan" %  "titan-core"       % titanVersion,
  "com.thinkaurelius.titan" %  "titan-berkeleyje" % titanVersion,
  "ohnosequences"           %% "scarph"           % scarphVersion,
  "ohnosequences"           %% "scarph"           % scarphVersion % Test classifier "tests",
  "org.scalatest"           %% "scalatest"        % "2.2.5"       % Test,
  "org.slf4j"               %  "slf4j-nop"        % "1.7.5"       % Test
  // ^ getting rid of the annoying warning about logging ^
)

// shows time for each test:
testOptions in Test += Tests.Argument("-oD")

// no name hashing, funny stuff happens
// incOptions := incOptions.value.withNameHashing(false)
// scalacOptions ++= Seq("-optimise", "-Yinline", "-Yinline-warnings")

// FIXME
wartremoverWarnings ++= Warts.all
wartremoverErrors in (Compile, compile) := Seq()
