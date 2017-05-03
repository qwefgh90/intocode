import Dependencies._

lazy val commonSettings = Seq(
  organization := "io.github.qwefgh90",
  scalaVersion := "2.11.8",
  libraryDependencies += scalaTest % Test,
  libraryDependencies += scalaTestPlus % Test,
  libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7",
  libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  libraryDependencies += "org.eclipse.mylyn.github" % "org.eclipse.egit.github.core" % "2.1.5"
)

lazy val extractor = (project in file("extractor")).dependsOn(bp).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "extractor",
    libraryDependencies += "io.github.qwefgh90" % "jsearch" % "0.3.0" exclude("org.slf4j", "slf4j-log4j12")
  )

lazy val web = (project in file("web")).enablePlugins(PlayScala).dependsOn(extractor).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "web",
    libraryDependencies += filters
  )

lazy val victims_client = (project in file("victims-client")).dependsOn(bp).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "victims-client",
    libraryDependencies += "org.apache.maven" % "maven-model" % "3.3.9",
    libraryDependencies += "com.esotericsoftware.yamlbeans" % "yamlbeans" % "1.08"
  )

lazy val bp = (project in file("bp")).
  settings(
    commonSettings,
    version := "0.1.0-SNAPSHOT",
    name := "bp",
    libraryDependencies += "com.googlecode.juniversalchardet" % "juniversalchardet" % "1.0.3"
  )
