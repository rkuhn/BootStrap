import sbt._

class BootStrapProject(info : ProjectInfo) extends DefaultProject(info) {
    
  override def managedStyle = ManagedStyle.Maven
  val publishTo = "RK Repo" at "http://www.rkuhn.info/repository/"
  Credentials(Path.userHome / ".rkrepo", log)

  lazy val scalaTestModuleConfig = ModuleConfiguration("org.scalatest", ScalaToolsSnapshots)

  val scalaTest = "org.scalatest" % "scalatest" % "1.4-SNAPSHOT" % "test"
}

// vim: set ts=2 sw=2 et:
