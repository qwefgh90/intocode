import Dependencies._

lazy val commonSettings = Seq(
  organization := "io.github.qwefgh90",
  scalaVersion := "2.11.8",
  libraryDependencies += scalaTest % Test,
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7",
  libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
)

lazy val extractor = (project in file("extractor")).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "Extractor",
    libraryDependencies += "io.github.qwefgh90" % "jsearch" % "0.3.0" exclude("org.slf4j", "slf4j-log4j12")
  )
