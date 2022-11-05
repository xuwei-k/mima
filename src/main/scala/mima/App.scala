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
  ): Unit = {
    def prettyPrint(p: core.Problem, affected: String): String = {
      " * " + p.description(affected) + p.howToFilter.map("\n   filter with: " + _).getOrElse("")
    }

    println(s"$projectName: found ${backErrors.size} potential binary incompatibilities while checking against")
    backErrors.map { p: core.Problem => prettyPrint(p, "current") }.foreach { p => log.error(p) }
    if (backErrors.nonEmpty) sys.error(projectName + ": Binary compatibility check failed!")
  }

  def runMima(previous: Library, current: Library): Int = {
    val previousFiles = previous.download()
    println(previousFiles.map("  " + _).mkString("previous files:\n", "\n", "\n"))
    val currentFiles = current.download()
    println(currentFiles.map("  " + _).mkString("current files:\n", "\n", "\n"))
    val problems = makeMima().collectProblems(
      oldJarOrDir = previousFiles.head,
      newJarOrDir = currentFiles.head,
      excludeAnnots = Nil
    )
    reportModuleErrors(
      backErrors = problems,
      log = logger,
      projectName = current.toString
    )
    0
  }

}

class App extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) =
    Exit(App.run(config.arguments))
}
