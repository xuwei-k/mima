package mima

import sbt.io.IO
import scalaj.http._
import java.io.File

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

  def runMima(previous: Library, current: Library): Int = {
    IO.withTemporaryDirectory { dir =>
      for {
        p <- download(previous).right
        c <- download(current).right
      } yield {
        val p0 = new File(dir, previous.name)
        val c0 = new File(dir, current.name)
        IO.write(p0, p)
        IO.write(c0, c)
        val args = "--prev" :: p0.getAbsolutePath :: "--curr" :: c0.getAbsolutePath :: Nil
        val m = new com.typesafe.tools.mima.cli.Main(args)
        m.run
      }
    }.left.map { error =>
      println(error)
      -1
    }.merge
  }

  def download(lib: Library): Either[String, Array[Byte]] = {
    val req = scalaj.http.Http(lib.mavenCentralURL).options(defaultOptions)
    println(s"downloading from ${lib.mavenCentralURL}")
    val res = req.asBytes
    println("status = " + res.code + " " + lib.mavenCentralURL)
    if (res.code == 200) {
      Right(res.body)
    } else {
      Left(s"status = ${res.code}. error while downloading ${lib.mavenCentralURL}")
    }
  }

}

class App extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) =
    Exit(App.run(config.arguments))
}
