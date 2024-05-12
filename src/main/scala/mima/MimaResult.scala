package mima

import com.typesafe.tools.mima.core.Problem
import mima.MimaResult.Incompatibilities
import java.io.File

case class MimaResult(
  previous: Seq[(Library, File)],
  current: Seq[(Library, File)],
  removed: Seq[(Library, File)],
  added: Seq[(Library, File)],
  incompatibilities: Seq[Incompatibilities[List[Problem]]]
)

object MimaResult {
  case class Incompatibilities[A](
    module: coursier.core.Module,
    previousVersion: String,
    currentVersion: String,
    problems: A
  )
}
