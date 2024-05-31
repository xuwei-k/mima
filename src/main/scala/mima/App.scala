package mima

import com.typesafe.tools.mima.core
import com.typesafe.tools.mima.lib
import com.typesafe.tools.mima.core.util.log.Logging

object App {

  def main(args: Array[String]): Unit = {
    val m = makeMima()

    Seq(
      "https/repo.scala-sbt.org/scalasbt/sbt-plugin-releases/com.lightbend.sbt/sbt-javaagent/scala_2.12/sbt_1.0/0.1.6/jars/sbt-javaagent.jar" -> "https/repo1.maven.org/maven2/com/github/sbt/sbt-javaagent_2.12_1.0/0.1.8/sbt-javaagent-0.1.8.jar",
      "https/repo.scala-sbt.org/scalasbt/sbt-plugin-releases/com.typesafe.sbt/sbt-js-engine/scala_2.12/sbt_1.0/1.2.3/jars/sbt-js-engine.jar" -> "https/repo1.maven.org/maven2/com/github/sbt/sbt-js-engine_2.12_1.0/1.3.0/sbt-js-engine_2.12_1.0-1.3.0.jar",
      "https/repo.scala-sbt.org/scalasbt/sbt-plugin-releases/com.typesafe.sbt/sbt-web/scala_2.12/sbt_1.0/1.4.4/jars/sbt-web.jar" -> "https/repo1.maven.org/maven2/com/github/sbt/sbt-web_2.12_1.0/1.5.5/sbt-web_2.12_1.0-1.5.5.jar"
    ).foreach { case (oldJar, newJar) =>
      println(
        m.collectProblems(
          oldJarOrDir = new java.io.File(
            scala.util.Properties.userHome,
            s"Library/Caches/Coursier/v1/$oldJar"
          ),
          newJarOrDir = {
            val f = new java.io.File(
              scala.util.Properties.userHome,
              s"Library/Caches/Coursier/v1/$newJar"
            )
            assert(f.isFile)
            f
          },
          excludeAnnots = Nil
        )
      )
    }

  }

  def run(args: Array[String]): Int = {
    args match {
      case Array(groupId, artifactId, previous, current) =>
        val p = Library(groupId, artifactId, previous)
        val c = Library(groupId, artifactId, current)
        val results = runMima(previous = p, current = c)
        if (results.incompatibilities.forall(_.problems.isEmpty)) {
          0
        } else {
          sys.error("Binary compatibility check failed!")
        }
      case other =>
        Console.err.println(
          """invalid args
          |usage:
          |mima <groupId> <artifactId> <previousVersion> <currentVersion>""".stripMargin
        )
        -1
    }
  }

  private[this] val logger = new Logging {
    override def verbose(str: String): Unit = {}
    override def debug(str: String): Unit = {}
    override def error(str: String): Unit = Console.err.println(str)
    override def warn(str: String): Unit = Console.err.println(str)
  }

  private def makeMima(): lib.MiMaLib = {
    new lib.MiMaLib(Nil, logger)
  }

  def reportModuleErrors(
    backErrors: List[core.Problem],
    log: Logging,
    projectName: String
  ): Unit = {
    def prettyPrint(p: core.Problem, affected: String): String = {
      " * " + p.description(affected) + p.howToFilter.map("\n   filter with: " + _).getOrElse("")
    }

    println(s"$projectName: found ${backErrors.size} potential binary incompatibilities while checking against")
    backErrors.map { p: core.Problem => prettyPrint(p, "current") }.foreach { p => log.error(p) }
    println()
  }

  def runMima(previous: Library, current: Library): MimaResult = {
    val previousFiles = previous.download().map { case (lib, f) => lib -> f }
    println(previousFiles.map("  " + _._2).mkString("previous files:\n", "\n", "\n"))
    val currentFiles = current.download()
    println(currentFiles.map("  " + _._2).mkString("current files:\n", "\n", "\n"))
    val removed = previousFiles.filterNot(x => currentFiles.map(_._1.module).toSet.contains(x._1.module))
    if (removed.nonEmpty) {
      println(removed.map("  " + _._2).mkString("removed files:\n", "\n", "\n"))
    }
    val added = currentFiles.filterNot(x => previousFiles.map(_._1.module).toSet.contains(x._1.module))
    if (added.nonEmpty) {
      println(added.map("  " + _._2).mkString("new files:\n", "\n", "\n"))
    }
    val oldMap = previousFiles.map { case (k, v) => k.module -> (k.version -> v) }.toMap
    val incompatibilities = currentFiles.flatMap { x =>
      oldMap.get(x._1.module).map(x -> _)
    }.filter { case ((newLib, _), (oldVersion, _)) =>
      newLib.version != oldVersion
    }.map { case ((newLib, newJar), (oldVersion, oldJar)) =>
      val problems = makeMima().collectProblems(
        oldJarOrDir = oldJar,
        newJarOrDir = newJar,
        excludeAnnots = Nil
      )
      reportModuleErrors(
        backErrors = problems,
        log = logger,
        projectName = s"${newLib.groupId} % ${newLib.artifactId} % ${oldVersion} => ${newLib.version}"
      )
      MimaResult.Incompatibilities(
        module = newLib.module,
        previousVersion = oldVersion,
        currentVersion = newLib.version,
        problems = problems
      )
    }

    MimaResult(
      previous = previousFiles,
      current = currentFiles,
      removed = removed,
      added = added,
      incompatibilities = incompatibilities
    )
  }

}

class App extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) =
    Exit(App.run(config.arguments))
}
