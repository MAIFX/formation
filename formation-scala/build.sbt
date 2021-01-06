name := """formation"""
organization := "com.maifx"
version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  ws,
  guice,
  "org.reactivemongo" %% "play2-reactivemongo" % "1.0.2-play28",
  "org.reactivemongo" %% "reactivemongo-akkastream" % "1.0.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.maifx.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.maifx.binders._"
