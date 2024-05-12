package mima

import java.io.File

final case class Library(groupId: String, artifactId: String, version: String) {

  val module: coursier.core.Module = coursier.core.Module(
    organization = coursier.core.Organization(groupId),
    name = coursier.core.ModuleName(artifactId),
    attributes = Map.empty
  )

  val coursierDependency = coursier.core.Dependency(
    module,
    version
  )
  def download(): Seq[(Library, File)] = {
    coursier
      .Fetch()
      .addDependencies(coursierDependency)
      .runResult()
      .detailedArtifacts
      .map(a =>
        Library(
          groupId = a._1.module.organization.value,
          artifactId = a._1.module.name.value,
          version = a._1.version
        ) -> a._4
      )
  }

  override def toString = s""""${groupId}" % "${artifactId}" % "${version}""""
}
