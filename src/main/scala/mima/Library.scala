package mima

final case class Library(groupId: String, artifactId: String, version: String) {
  val name = s"$artifactId-$version.jar"
  val mavenCentralURL: String = {
    val g = groupId.replace('.', '/')
    s"${Library.MavenCentral}$g/$artifactId/$version/$name"
  }
  override def toString = s""""${groupId}" % "${artifactId}" % "${version}""""
}

object Library {
  final val MavenCentral = "https://repo1.maven.org/maven2/"
}
