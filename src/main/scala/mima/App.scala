package mima

import com.typesafe.tools.mima.core
import com.typesafe.tools.mima.lib
import com.typesafe.tools.mima.core.util.log.Logging

object App {

  def main(args: Array[String]): Unit = {
    run(args)
  }

  def run(args: Array[String]): Int = {
    args match {
      case Array(groupId, artifactId, previous, current) =>
        val p = Library(groupId, artifactId, previous)
        val c = Library(groupId, artifactId, current)
        runMima(previous = p, current = c)
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
  ): Boolean = {
    def prettyPrint(p: core.Problem, affected: String): String = {
      " * " + p.description(affected) + p.howToFilter.map("\n   filter with: " + _).getOrElse("")
    }

    println(s"$projectName: found ${backErrors.size} potential binary incompatibilities while checking against")
    backErrors.map { p: core.Problem => prettyPrint(p, "current") }.foreach { p => log.error(p) }
    println()
    backErrors.isEmpty
  }

  def runMima(previous: Library, current: Library): Int = {
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
    val results = currentFiles.flatMap { x =>
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
    }
    if (results.forall(identity)) {
      0
    } else {
      sys.error("Binary compatibility check failed!")
    }
  }

}

class App extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) =
    Exit(App.run(config.arguments))
}
