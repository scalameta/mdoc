package mdoc

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbtdocusaurus.internal.Relativize
import scala.sys.process.Process
import scala.sys.process.ProcessBuilder
import mdoc.MdocPlugin.{autoImport => m}

object DocusaurusPlugin extends AutoPlugin {
  override def requires: Plugins = JvmPlugin && MdocPlugin
  object autoImport {
    val docusaurusProjectName =
      taskKey[String]("The siteConfig.js `projectName` setting value")
    val docusaurusCreateSite =
      taskKey[File]("Create static build of docusaurus site")
    val docusaurusPublishGhpages =
      taskKey[Unit]("Publish docusaurus site to GitHub pages")
  }
  import autoImport._
  def website: Def.Initialize[File] = Def.setting {
    baseDirectory.in(ThisBuild).value / "website"
  }

  def listJarFiles(root: Path): List[(File, String)] = {
    val files = List.newBuilder[(File, String)]
    Files.walkFileTree(
      root,
      new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          val relpath = root.relativize(file)
          files += (file.toFile -> relpath.toString)
          super.visitFile(file, attrs)
        }
      }
    )
    files.result()
  }

  def gitUser(): String =
    sys.env.getOrElse("GIT_USER", {
      import sys.process._
      "git config user.email".!!.trim
    })
  def installSsh: String =
    """|#!/usr/bin/env bash
       |
       |set -eu
       |
       |set-up-ssh() {
       |  echo "Setting up ssh..."
       |  mkdir -p $HOME/.ssh
       |  ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts
       |  git config --global user.name "Docusaurus bot"
       |  git config --global user.email "$TRAVIS_BUILD_NUMBER@$TRAVIS_COMMIT"
       |  git config --global push.default simple
       |  DEPLOY_KEY_FILE=$HOME/.ssh/id_rsa
       |  echo "$GITHUB_DEPLOY_KEY" | base64 --decode > ${DEPLOY_KEY_FILE}
       |  chmod 600 ${DEPLOY_KEY_FILE}
       |  eval "$(ssh-agent -s)"
       |  ssh-add ${DEPLOY_KEY_FILE}
       |}
       |DEPLOY_KEY=${GITHUB_DEPLOY_KEY:-}
       |
       |if [[ -n "$DEPLOY_KEY" ]]; then
       |  set-up-ssh
       |fi
       |
       |yarn install
       |USE_SSH=true yarn publish-gh-pages
    """.stripMargin

  override def projectSettings: Seq[Def.Setting[_]] = List(
    libraryDependencies ++= List(
      "com.geirsson" %% "mdoc" % "0.7.1"
    ),
    aggregate.in(docusaurusPublishGhpages) := false,
    aggregate.in(docusaurusCreateSite) := false,
    docusaurusProjectName := moduleName.value.stripSuffix("-docs"),
    docusaurusPublishGhpages := {
      m.mdoc.toTask(" ").value
      val tmp = Files.createTempFile("docusaurus", "install_ssh.sh")
      Files.write(tmp, installSsh.getBytes())
      tmp.toFile.setExecutable(true)
      Process(
        tmp.toString,
        cwd = website.value,
        "GIT_USER" -> gitUser(),
        "USE_SSH" -> "true"
      ).execute()
    },
    docusaurusCreateSite := {
      m.mdoc.in(Compile).toTask(" ").value
      Process(List("yarn", "install"), cwd = website.value).execute()
      Process(List("yarn", "run", "build"), cwd = website.value).execute()
      val redirectUrl = docusaurusProjectName.value + "/index.html"
      val html = redirectHtml(redirectUrl)
      val out = website.value / "build"
      IO.write(out / "index.html", html)
      out
    },
    doc := {
      val out = docusaurusCreateSite.value
      Relativize.htmlSite(out.toPath)
      out
    },
    packageDoc.in(Compile) := {
      val directory = doc.value
      val jar = target.value / "docusaurus.jar"
      val files = listJarFiles(directory.toPath)
      IO.jar(files, jar, new java.util.jar.Manifest())
      jar
    }
  )

  private implicit class XtensionListStringProcess(command: List[String]) {
    def execute(): Unit = {
      Process(command).execute()
    }
  }
  private implicit class XtensionStringProcess(command: String) {
    def execute(): Unit = {
      Process(command).execute()
    }
  }
  private implicit class XtensionProcess(command: ProcessBuilder) {
    def execute(): Unit = {
      val exit = command.!
      assert(exit == 0, s"command returned $exit: $command")
    }
  }
  private def redirectHtml(url: String): String = {
    s"""
       |<!DOCTYPE HTML>
       |<html lang="en-US">
       |    <head>
       |        <meta charset="UTF-8">
       |        <meta http-equiv="refresh" content="0; url=$url">
       |        <script type="text/javascript">
       |            window.location.href = "$url"
       |        </script>
       |        <title>Page Redirection</title>
       |    </head>
       |    <body>
       |        If you are not redirected automatically, follow this <a href='$url'>link</a>.
       |    </body>
       |</html>
      """.stripMargin
  }

}
