// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("public")

// Use the Play sbt plugin for Play projects

addSbtPlugin("org.scalaxb" % "sbt-scalaxb" % "1.4.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.9")

