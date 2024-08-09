name := "mima"

scalaVersion := "2.13.14"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest-freespec" % "3.2.19" % Test,
  "com.typesafe" %% "mima-core" % "1.1.4",
  "io.get-coursier" %% "coursier" % "2.1.10"
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

publishTo := (
  if (isSnapshot.value)
    None
  else
    Some(Opts.resolver.sonatypeStaging)
)

enablePlugins(ConscriptPlugin)

val updateLaunchconfig = TaskKey[File]("updateLaunchconfig")
val launchconfigFile = file("src/main/conscript/mima/launchconfig")

TaskKey[Int]("testConscript") := Def
  .sequential(
    updateLaunchconfig,
    csRun.toTask(" mima org.scalaz scalaz-core_2.11 7.1.0 7.1.1"),
    Def.task {
      sys.process.Process(s"git checkout ${launchconfigFile}").!
    }
  )
  .value

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

(Compile / doc / scalacOptions) ++= {
  val tag = tagOrHash.value
  Seq(
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath,
    "-doc-source-url",
    s"https://github.com/xuwei-k/mima/tree/${tag}€{FILE_PATH}.scala"
  )
}

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions"
)

releaseTagName := tagName.value

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
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)

updateLaunchconfig := {
  val mainClassName = (Compile / discoveredMainClasses).value match {
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
  IO.write(launchconfigFile, launchconfig)
  val git = new sbtrelease.Git((LocalRootProject / baseDirectory).value)
  val s = streams.value.log
  git.add(launchconfigFile.getCanonicalPath) ! s
  git.commit(message = "update launchconfig", sign = false, signOff = false) ! s
  launchconfigFile
}
