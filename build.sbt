import scalariform.formatter.preferences._

// Resolvers
resolvers ++= Seq(
  "scalaz bintray repo" at "http://dl.bintray.com/scalaz/releases"
)

// sbt-dependency-graph
////
net.virtualvoid.sbt.graph.Plugin.graphSettings

// sbt-scalariform
////
scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, false)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
  .setPreference(CompactControlReadability, false)
  .setPreference(CompactStringConcatenation, false)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(FormatXml, true)
  .setPreference(IndentLocalDefs, false)
  .setPreference(IndentPackageBlocks, true)
  .setPreference(IndentSpaces, 2)
  .setPreference(IndentWithTabs, false)
  .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, false)
  .setPreference(PreserveSpaceBeforeArguments, false)
  .setPreference(PreserveDanglingCloseParenthesis, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(SpaceBeforeColon, false)
  .setPreference(SpaceInsideBrackets, false)
  .setPreference(SpaceInsideParentheses, false)
  .setPreference(SpacesWithinPatternBinders, true)

// Test dependencies
////
lazy val testDependencies = Seq (
)

// Project dependencies
////
lazy val rootDependencies = Seq(
  "com.amazonaws"                        %  "aws-java-sdk" % "1.7.5",
  "io.argonaut"                          %% "argonaut"     % "6.0.3",
  "org.scalaz.stream" %% "scalaz-stream" %  "0.4"
)

// Compiler options
//
lazy val compileSettings = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:_",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-all",
  //"-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard"
)
//

// JVM options
//
lazy val forkedJvmOption = Seq(
  "-server",
  "-Dfile.encoding=UTF8",
  "-Duser.timezone=GMT",
  "-Xss1m",
  "-Xms1536m",
  "-Xmx1536m",
  "-XX:+CMSClassUnloadingEnabled",
  "-XX:MaxPermSize=384m",
  "-XX:ReservedCodeCacheSize=256m",
  "-XX:+DoEscapeAnalysis",
  "-XX:+UseConcMarkSweepGC",
  "-XX:+UseParNewGC",
  "-XX:+UseCodeCacheFlushing",
  "-XX:+UseCompressedOops"
)
//

lazy val main =
  project
    .in(file("."))
    .settings(
  	  name := "amazon-streamz",
  	  version := "0.1-SNAPSHOT",
  	  scalaVersion := "2.10.4",
      libraryDependencies ++= rootDependencies ++ testDependencies,
      fork in run := true,
      fork in Test := true,
      fork in testOnly := true,
      connectInput in run := true,
      javaOptions in run ++= forkedJvmOption,
      javaOptions in Test ++= forkedJvmOption,
      scalacOptions ++= compileSettings
    )
