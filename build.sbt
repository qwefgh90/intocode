import Dependencies._

lazy val commonSettings = Seq(
  organization := "io.github.qwefgh90",
  scalaVersion := "2.11.8",
  fork in Test := false,
  unmanagedBase :=  file(".") / "unmanaged_lib",
  libraryDependencies += scalaTest % Test,
  libraryDependencies += scalaTestPlus % Test,
  libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2",
  libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
  libraryDependencies += "io.reactivex" %% "rxscala" % "0.26.5",
  libraryDependencies += "com.typesafe" % "config" % "1.3.1"
)


lazy val extractor = (project in file("extractor")).dependsOn(bp).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "extractor",
    libraryDependencies += "io.github.qwefgh90" % "jsearch" % "0.3.0" exclude("org.slf4j", "slf4j-log4j12"),
    libraryDependencies += "org.languagetool" % "languagetool-core" % "3.8",
    libraryDependencies += "org.languagetool" % "language-en" % "3.8" exclude("org.slf4j", "slf4j-jdk14")
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
    libraryDependencies += ehcache,
    libraryDependencies += specs2 % Test,
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0",
    libraryDependencies += "com.typesafe.play" %% "play-iteratees" % "2.6.1",
    libraryDependencies += "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1",
    libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.0.1",
    libraryDependencies += "com.typesafe.play" %% "play-ws-standalone-json" % "1.0.1",
    libraryDependencies += "com.typesafe.play" %% "play-ws-standalone-xml" % "1.0.1",
    libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.0",
    libraryDependencies += "com.h2database" % "h2" % "1.4.196",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.5.4",
      "com.typesafe.akka" %% "akka-testkit" % "2.5.4" % Test,
      "com.typesafe.akka" %% "akka-cluster" % "2.5.4",
      "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.4"
    ),
    libraryDependencies += "io.backchat.hookup" %% "hookup" % "0.4.2" % Test
  )

lazy val victims_client = (project in file("victims-client")).dependsOn(bp).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "victims-client",
    libraryDependencies += "org.yaml" % "snakeyaml" % "1.18",
    libraryDependencies += "org.eclipse.aether" % "aether-connector-basic" % "1.1.0",
    libraryDependencies += "org.apache.maven" % "maven-aether-provider" % "3.3.9",
    libraryDependencies += "org.eclipse.aether" % "aether-transport-file" % "1.1.0",
    libraryDependencies += "org.eclipse.aether" % "aether-transport-http" % "1.1.0",
    libraryDependencies += "org.apache.maven" % "maven-model" % "3.5.0"
  )
lazy val bp = (project in file("bp")).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "bp",
    libraryDependencies += "io.github.qwefgh90" % "jsearch" % "0.3.0" exclude("org.slf4j", "slf4j-log4j12"),
    libraryDependencies += "com.googlecode.juniversalchardet" % "juniversalchardet" % "1.0.3",
    libraryDependencies += "org.eclipse.aether" % "aether-connector-basic" % "1.1.0",
    libraryDependencies += "org.apache.maven" % "maven-aether-provider" % "3.3.9",
    libraryDependencies += "org.eclipse.aether" % "aether-transport-file" % "1.1.0",
    libraryDependencies += "org.eclipse.aether" % "aether-transport-http" % "1.1.0",
    libraryDependencies += "org.apache.maven" % "maven-model" % "3.5.0"
  )

//
//
