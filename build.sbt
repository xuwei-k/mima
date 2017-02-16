name := "mima"

libraryDependencies ++= Seq(
  "com.typesafe" %% "mima-reporter" % "0.1.14",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.scala-sbt" %% "io" % "1.0.0-M9"
)

organization := "com.github.xuwei-k"
licenses := Seq("MIT" -> url("https://opensource.org/licenses/mit-license"))
homepage := some(url("https://github.com/xuwei-k/mima"))
pomExtra :=
  <scm>
    <url>git@github.com:xuwei-k/mima.git</url>
    <connection>scm:git:git@github.com:xuwei-k/mima.git</connection>
  </scm>
  <developers>
    <developer>
      <id>xuwei-k</id>
      <name>Kenji Yoshida</name>
      <url>https://github.com/xuwei-k</url>
    </developer>
  </developers>

enablePlugins(ConscriptPlugin)

scalafmtConfig in ThisBuild := Some((baseDirectory in LocalRootProject).value / ".scalafmt.conf")

val updateLaunchconfig = TaskKey[File]("updateLaunchconfig")

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lines_!.head
  else tagName.value
}

scalacOptions in (Compile, doc) ++= {
  val tag = tagOrHash.value
  Seq(
    "-sourcepath",
    (baseDirectory in LocalRootProject).value.getAbsolutePath,
    "-doc-source-url",
    s"https://github.com/xuwei-k/mima/tree/${tag}€{FILE_PATH}.scala"
  )
}

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Xfuture",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Yno-adapted-args"
)

releaseTagName := tagName.value
resolvers += Opts.resolver.sonatypeReleases

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  releaseStepTask(updateLaunchconfig),
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)

updateLaunchconfig := {
  val mainClassName = (discoveredMainClasses in Compile).value match {
    case Seq(m) => m
    case zeroOrMulti => sys.error(s"could not found main class. $zeroOrMulti")
  }
  val launchconfig = s"""[app]
    |  version: ${version.value}
    |  org: ${organization.value}
    |  name: ${normalizedName.value}
    |  class: ${mainClassName}
    |[scala]
    |  version: ${scalaVersion.value}
    |[repositories]
    |  local
    |  maven-central
    |""".stripMargin
  val f = (baseDirectory in ThisBuild).value / "src/main/conscript/mima/launchconfig"
  IO.write(f, launchconfig)
  f
}
