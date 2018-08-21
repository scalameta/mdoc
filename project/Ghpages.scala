package ghpages

import sbt.Keys._
import sbt._
import sys.process._

object Ghpages extends AutoPlugin {
  def installGithubToken(): Unit = {
    println("Setting up ssh...")
    val email = sys.env("USER_EMAIL")
    val userName = sys.env("USER_NAME")
    val githubDeployKey = sys.env("GITHUB_DEPLOY_KEY")
    val ssh = file(sys.props("user.home")) / ".ssh"
    ssh.mkdirs()
    "mkdir -p $HOME/.ssh".!
    "ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts".!
    s"git config --global user.email '$email'".!
    s"git config --global user.name '$userName'".!
    "git config --global push.default simple".!
    val deployKeyFile = ssh / "id_rsa"
    (s"echo $githubDeployKey" #| "base64 --decode" #> deployKeyFile).!
    s"chmod 600 $deployKeyFile".!
    "eval '$(ssh-agent -s)'".!
    s"ssh-add $deployKeyFile".!
  }

  override def globalSettings: Seq[Def.Setting[_]] = List(
    commands += Command.command("ci-release-docs") { s =>
      installGithubToken()
      "website/ghpagesPushSite" ::
        s
    }
  )
}
