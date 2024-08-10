package mima

import mima.MimaResult.Incompatibilities
import org.scalatest.freespec.AnyFreeSpec
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class MimaTest extends AnyFreeSpec {
  private def run(previous: Library, current: Library): MimaResult = {
    val devnull = new PrintStream(new ByteArrayOutputStream())
    Console.withOut(devnull) {
      Console.withErr(devnull) {
        App.runMima(
          previous = previous,
          current = current
        )
      }
    }
  }

  "mima" - {
    val scalaLibrary = coursier.core.Module(
      coursier.core.Organization("org.scala-lang"),
      coursier.core.ModuleName("scala-library"),
      Map.empty
    )
    val hocon = coursier.core.Module(
      coursier.core.Organization("com.typesafe"),
      coursier.core.ModuleName("config"),
      Map.empty
    )

    "removed" in {
      val previous = Library(
        groupId = "com.typesafe",
        artifactId = "ssl-config-core_2.13",
        version = "0.4.3"
      )
      val current = previous.copy(version = "0.6.0")

      val result = run(
        previous = previous,
        current = current
      )
      assert(
        result.previous.map(_._1) == Seq(
          previous,
          Library("org.scala-lang", "scala-library", "2.13.1"),
          Library("org.scala-lang.modules", "scala-parser-combinators_2.13", "1.1.2"),
          Library("com.typesafe", "config", "1.4.0")
        )
      )
      assert(
        result.current.map(_._1) == Seq(
          current,
          Library("org.scala-lang", "scala-library", "2.13.6"),
          Library("com.typesafe", "config", "1.4.1")
        )
      )
      assert(
        result.removed.map(_._1) == Seq(
          Library("org.scala-lang.modules", "scala-parser-combinators_2.13", "1.1.2")
        )
      )
      assert(result.added == Nil)
      assert(
        result.incompatibilities.map(x => x.copy(problems = x.problems.size)) == Seq(
          Incompatibilities(
            module = current.module,
            previousVersion = previous.version,
            currentVersion = current.version,
            problems = 49
          ),
          Incompatibilities(
            module = scalaLibrary,
            previousVersion = "2.13.1",
            currentVersion = "2.13.6",
            problems = 187
          ),
          Incompatibilities(
            module = hocon,
            previousVersion = "1.4.0",
            currentVersion = "1.4.1",
            problems = 0
          )
        )
      )
    }

    "added" in {
      val previous = Library(
        groupId = "com.typesafe",
        artifactId = "ssl-config-core_2.13",
        version = "0.6.0"
      )
      val current = previous.copy(version = "0.4.3")

      val result = run(
        previous = previous,
        current = current
      )
      assert(
        result.previous.map(_._1) == Seq(
          previous,
          Library("org.scala-lang", "scala-library", "2.13.6"),
          Library("com.typesafe", "config", "1.4.1")
        )
      )
      assert(
        result.current.map(_._1) == Seq(
          current,
          Library("org.scala-lang", "scala-library", "2.13.1"),
          Library("org.scala-lang.modules", "scala-parser-combinators_2.13", "1.1.2"),
          Library("com.typesafe", "config", "1.4.0")
        )
      )
      assert(result.removed == Nil)
      assert(
        result.added.map(_._1) == Seq(
          Library("org.scala-lang.modules", "scala-parser-combinators_2.13", "1.1.2")
        )
      )
      assert(
        result.incompatibilities.map(x => x.copy(problems = x.problems.size)) == Seq(
          Incompatibilities(
            module = current.module,
            previousVersion = previous.version,
            currentVersion = current.version,
            problems = 6
          ),
          Incompatibilities(
            module = scalaLibrary,
            previousVersion = "2.13.6",
            currentVersion = "2.13.1",
            problems = 236
          ),
          Incompatibilities(
            module = hocon,
            previousVersion = "1.4.1",
            currentVersion = "1.4.0",
            problems = 5
          )
        )
      )
    }
  }
}
