import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import sbtscalaxb.Plugin.ScalaxbKeys._


object ApplicationBuild extends Build {

  lazy val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
  lazy val scalaParser = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
  lazy val dispatchV = "0.11.2"
  lazy val dispatch = "net.databinder.dispatch" %% "dispatch-core" % dispatchV

  val appName         = "payzen-module"
  val appVersion      = "1.4-SNAPSHOT"

  val appDependencies = Seq(
    ws,
    scalaXml,
    scalaParser,
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
  )

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    scalaVersion := "2.11.7",
    organization := "fr.valwin",
    version := appVersion,
    libraryDependencies ++= appDependencies,
    publishTo := Some("valwin-snapshots" at "http://nexus.valwin.fr/nexus/content/repositories/valwin-snapshots"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  ).settings(sbtscalaxb.Plugin.scalaxbSettings: _*).
    settings(
      sourceGenerators in Compile += (scalaxb in Compile).taskValue,
      dispatchVersion in (Compile, scalaxb) := dispatchV,
      async in (Compile, scalaxb)           := true,
      packageName in (Compile, scalaxb)     := "generated"
      // packageNames in (Compile, scalaxb)    := Map(uri("http://schemas.microsoft.com/2003/10/Serialization/") -> "microsoft.serialization"),
      // logLevel in (Compile, scalaxb) := Level.Debug
    )

}
