import Dependencies._

lazy val commonSettings = Seq(
  organization := "io.github.qwefgh90",
  scalaVersion := "2.11.8",
  fork in Test := false,
  libraryDependencies += scalaTest % Test,
  libraryDependencies += scalaTestPlus % Test,
  libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2",
  libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  libraryDependencies += "org.eclipse.mylyn.github" % "org.eclipse.egit.github.core" % "2.1.5",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
)

lazy val extractor = (project in file("extractor")).dependsOn(bp).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "extractor",
    libraryDependencies += "io.github.qwefgh90" % "jsearch" % "0.3.0" exclude("org.slf4j", "slf4j-log4j12"),
    libraryDependencies += "org.languagetool" % "languagetool" % "2.0.1" exclude("org.slf4j", "slf4j-jdk14")
  )

lazy val web = (project in file("web")).enablePlugins(PlayScala).dependsOn(bp).dependsOn(extractor).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "web",
    libraryDependencies += filters,
  	libraryDependencies += "io.jsonwebtoken" % "jjwt" % "0.7.0",
  	libraryDependencies += ws,
    libraryDependencies += guice,
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0",
    libraryDependencies += "com.typesafe.play" %% "play-iteratees" % "2.6.1",
    libraryDependencies += "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1"

  )

lazy val victims_client = (project in file("victims-client")).dependsOn(bp).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "victims-client",
    libraryDependencies += "org.yaml" % "snakeyaml" % "1.18",
    libraryDependencies += "org.apache.maven" % "maven-model" % "3.3.9",
    libraryDependencies += "org.sonatype.aether" % "aether-api" % "1.13.1"
  )

lazy val bp = (project in file("bp")).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "bp",
    libraryDependencies += "com.googlecode.juniversalchardet" % "juniversalchardet" % "1.0.3"
  )

//
//
