package mima

import java.io.File

final case class Library(groupId: String, artifactId: String, version: String) {

  val coursierDependency = coursier.core.Dependency(
    coursier.core.Module(
      organization = coursier.core.Organization(groupId),
      name = coursier.core.ModuleName(artifactId),
      attributes = Map.empty
    ),
    version
  )
  def download(): Seq[File] = {
    coursier.Fetch().addDependencies(coursierDependency).run()
  }

  override def toString = s""""${groupId}" % "${artifactId}" % "${version}""""
}
