package mima

import sbt.io.IO
import scalaj.http._
import java.io.File
import com.typesafe.tools.mima.core
import com.typesafe.tools.mima.lib
import com.typesafe.tools.mima.core.util.log.Logging

object App {

  val defaultOptions: Seq[HttpOptions.HttpOption] = Seq(
    _.setConnectTimeout(60000),
    _.setReadTimeout(60000)
  )

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
    override def debugLog(str: String): Unit = {}
    override def error(str: String): Unit = Console.err.println(str)
    override def info(str: String): Unit = {}
    override def warn(str: String): Unit = Console.err.println(str)
  }

  private def makeMima(): lib.MiMaLib = {
    core.Config.setup("conscript-mima", Array.empty)
    val classpath = com.typesafe.tools.mima.core.reporterClassPath("")
    new lib.MiMaLib(classpath, logger)
  }

  def reportModuleErrors(
    backErrors: List[core.Problem],
    log: Logging,
    projectName: String
  ): Unit = {

    // TODO - Line wrapping an other magikz
    def prettyPrint(p: core.Problem, affected: String): String = {
      " * " + p.description(affected) + p.howToFilter.map("\n   filter with: " + _).getOrElse("")
    }

    println(s"$projectName: found ${backErrors.size} potential binary incompatibilities while checking against")
    backErrors.map { p: core.Problem =>
      prettyPrint(p, "current")
    }.foreach { p =>
      log.error(p)
    }
    if (backErrors.nonEmpty) sys.error(projectName + ": Binary compatibility check failed!")
  }

  def runMima(previous: Library, current: Library): Int = {
    IO.withTemporaryDirectory { dir =>
      for {
        p <- download(previous)
        c <- download(current)
      } yield {
        val p0 = new File(dir, previous.name)
        val c0 = new File(dir, current.name)
        IO.write(p0, p)
        IO.write(c0, c)
        val problems = makeMima().collectProblems(p0.getAbsolutePath, c0.getAbsolutePath)
        reportModuleErrors(
          backErrors = problems,
          log = logger,
          projectName = current.toString
        )
        0
      }
    }.left.map { error =>
      println(error)
      -1
    }.merge
  }

  def download(lib: Library): Either[String, Array[Byte]] = {
    val x :: xs = lib.urls
    xs.foldLeft(download0(x)) {
      case (success @ Right(_), _) =>
        success
      case (_ @Left(_), nextURL) =>
        download0(nextURL)
    }
  }
  def download0(url: String): Either[String, Array[Byte]] = {
    val req = scalaj.http.Http(url).options(defaultOptions)
    println(s"downloading from ${url}")
    val res = req.asBytes
    println(s"status = ${res.code} ${url}")
    if (res.code == 200) {
      Right(res.body)
    } else {
      Left(s"status = ${res.code}. error while downloading ${url}")
    }
  }

}

class App extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) =
    Exit(App.run(config.arguments))
}
