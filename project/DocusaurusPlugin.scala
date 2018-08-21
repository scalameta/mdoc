package ghpages

import sbt.Keys._
import sbt._
import sys.process._
import sbt.plugins.JvmPlugin

object DocusaurusPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin
  def installGithubToken(): Unit = {
    println("Setting up ssh...")
    val env = sys.env
    val email = env("USER_EMAIL")
    val travisBuildNumber = env("TRAVIS_BUILD_NUMBER")
    val traviscommit = env("TRAVIS_COMMIT")
    val userName = s"$travisBuildNumber@$traviscommit"
    val githubDeployKey = env("GITHUB_DEPLOY_KEY")
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
    """bash -c 'eval "$(ssh-agent -s)"' """.!
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
