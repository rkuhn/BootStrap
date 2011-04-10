import sbt._

class BootStrapProject(info : ProjectInfo) extends DefaultProject(info) {
    
    override def managedStyle = ManagedStyle.Maven
    val publishTo = "RK Repo" at "http://www.rkuhn.info/repository/"
    Credentials(Path.userHome / ".rkrepo", log)

}

// vim: set ts=4 sw=4 et:
