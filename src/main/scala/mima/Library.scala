package mima

final case class Library(groupId: String, artifactId: String, version: String) {
  val name = s"$artifactId-$version.jar"
  private[this] val g = groupId.replace('.', '/')
  private[this] def buildURL(base: String): String = {
    s"${base}$g/$artifactId/$version/$name"
  }

  val mavenCentralURL: String = buildURL(Library.MavenCentral)
  val sonatypeReleasesURL: String = buildURL(Library.SonatypeReleases)

  val urls: Seq[String] = mavenCentralURL :: sonatypeReleasesURL :: Nil

  override def toString = s""""${groupId}" % "${artifactId}" % "${version}""""
}

object Library {
  final val MavenCentral = "https://repo1.maven.org/maven2/"
  final val SonatypeReleases = "https://oss.sonatype.org/content/repositories/releases/"
}
