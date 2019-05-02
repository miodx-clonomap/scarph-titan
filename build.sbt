name          := "scarph-titan"
description   := "Scarph evaluators for TitanDB"
organization  := "com.miodx.common"
version       := "0.3.0"
bucketSuffix  := "era7.com"

scalaVersion  := "2.12.8"
val scarphVersion = "0.5.0"
val titanVersion  = "1.0.0"

libraryDependencies ++= Seq(
  "com.miodx.common"        %% "cosas"            % "0.10.1",
  "com.thinkaurelius.titan" %  "titan-core"       % titanVersion,
  "com.miodx.common"        %% "scarph"           % scarphVersion,
  "com.miodx.common"        %% "scarph"           % scarphVersion % Test classifier "tests",
  "com.thinkaurelius.titan" %  "titan-berkeleyje" % titanVersion  % Test,
  "org.scalatest"           %% "scalatest"        % "3.0.1"       % Test,
  "org.slf4j"               %  "slf4j-nop"        % "1.7.5"       % Test
)

// shows time for each test:
testOptions in Test += Tests.Argument("-oD")
parallelExecution in Test := false

// no name hashing, funny stuff happens
// incOptions := incOptions.value.withNameHashing(false)
// scalacOptions ++= Seq("-optimise", "-Yinline", "-Yinline-warnings")

// FIXME
// wartremoverWarnings ++= Warts.all
wartremoverErrors in (Compile, compile) := Seq()
wartremoverErrors in (Test,    compile) := Seq()



// libraryDependencies += "com.tinkerpop.blueprints" % "blueprints-core" % "2.5.0"
// conflictManager := ConflictManager.default
// libraryDependencies += "com.lihaoyi" % "ammonite" % "0.8.2" cross CrossVersion.full
// initialCommands in (Compile, console) := """ammonite.Main().run()"""
