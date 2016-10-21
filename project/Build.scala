import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._


object ApplicationBuild extends Build {

  val appName         = "payzen-module"
  val appVersion      = "1.4-SNAPSHOT"

  val appDependencies = Seq(
    ws,
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
  )

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    scalaVersion := "2.11.7",
    organization := "fr.valwin",
    version := appVersion,
    libraryDependencies ++= appDependencies,
    publishTo := Some("valwin-snapshots" at "http://nexus.valwin.fr/nexus/content/repositories/valwin-snapshots"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )

}
