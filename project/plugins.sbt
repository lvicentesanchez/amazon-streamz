// Comment to get more information during initialization
//
logLevel := Level.Warn

/// Resolvers
//
resolvers ++= Seq(
  "bintray-sbt-plugin-releases" at "http://dl.bintray.com/content/sbt/sbt-plugin-releases",
  "sbt plugin releases" at "http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/",
  "maven2 repository" at "http://repo.maven.apache.org/maven2",
  "softprops Maven" at "http://dl.bintray.com/content/softprops/maven",
  "sonatype oss releases" at "http://oss.sonatype.org/content/repositories/releases/",
  "sonatype oss snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "typesafe releases" at "http://repo.typesafe.com/typesafe/releases/",
  "typesafe snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
)

// Assembly
//
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

// Build Info
//
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.1")

// Dependency graph
//
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

// Releases
//
addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.2")

// Scalariform
//
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// Scoverage
//
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.98.0")

// Update plugin
//
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.5")
