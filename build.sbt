name := """play-scala-starter-example"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.6"

crossScalaVersions := Seq("2.11.12", "2.12.6")

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += ws
// https://mvnrepository.com/artifact/com.typesafe.play/play-ahc-ws-standalone
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.10"
// https://mvnrepository.com/artifact/com.github.haifengl/smile-scala
libraryDependencies += "com.github.haifengl" %% "smile-scala" % "1.5.1"
