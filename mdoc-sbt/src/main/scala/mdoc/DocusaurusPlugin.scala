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
import scala.util.Try

object DocusaurusPlugin extends AutoPlugin with MdocPluginCompat {
  override def requires: Plugins = JvmPlugin && MdocPlugin
  object autoImport {
    val docusaurusProjectName =
      taskKey[String]("The siteConfig.js `projectName` setting value")
    val docusaurusVersion =
      settingKey[DocusaurusVersion](
        "The Docusaurus version used in your project. Docusaurus build and publish commands differ between versions"
      )
    @transient
    val docusaurusCreateSite =
      taskKey[File]("Create static build of docusaurus site")
    @transient
    val docusaurusPublishGhpages =
      taskKey[Unit]("Publish docusaurus site to GitHub pages")

    sealed trait DocusaurusVersion {
      def buildArgs: List[String]
      def publishArgs: List[String]
      def createRootRedirect: Boolean
    }
    object DocusaurusVersion {
      case object V1 extends DocusaurusVersion {
        val buildArgs: List[String] = List("run", "build")
        val publishArgs: List[String] = List("publish-gh-pages")
        val createRootRedirect: Boolean = true
      }
      case object V3 extends DocusaurusVersion {
        val buildArgs: List[String] = List("build")
        val publishArgs: List[String] = List("deploy")
        val createRootRedirect: Boolean = false
      }
    }

  }
  import autoImport._
  def website: Def.Initialize[File] =
    Def.setting {
      (ThisBuild / baseDirectory).value / "website"
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
    sys.env.getOrElse(
      "GIT_USER", {
        import scala.sys.process._
        Try("git config user.email".!!.trim)
          .getOrElse("docusaurus@scalameta.org")
      }
    )
  def installSsh(publishArgs: List[String]): String = {
    val publishCommand = ("yarn" :: publishArgs).mkString(" ")
    s"""|#!/usr/bin/env bash
        |
        |set -eu
        |DEPLOY_KEY=$${GIT_DEPLOY_KEY:-$${GITHUB_DEPLOY_KEY:-}}
        |set-up-ssh() {
        |  echo "Setting up ssh..."
        |  mkdir -p $$HOME/.ssh
        |  ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts
        |  git config --global user.name "Docusaurus bot"
        |  git config --global user.email "$${MDOC_EMAIL:-mdoc@docusaurus}"
        |  git config --global push.default simple
        |  DEPLOY_KEY_FILE=$$HOME/.ssh/id_rsa
        |  echo "$$DEPLOY_KEY" | base64 --decode > $${DEPLOY_KEY_FILE}
        |  chmod 600 $${DEPLOY_KEY_FILE}
        |  eval "$$(ssh-agent -s)"
        |  ssh-add $${DEPLOY_KEY_FILE}
        |}
        |
        |if [[ -n "$${DEPLOY_KEY:-}" ]]; then
        |  set-up-ssh
        |else
        |  echo "No deploy key found. Attempting to auth with ssh key saved in ssh-agent. To use a deploy key instead, set the GIT_DEPLOY_KEY environment variable."
        |fi
        |
        |yarn install
        |USE_SSH=true $publishCommand
     """.stripMargin
  }

  def installSshWindows(publishArgs: List[String]): String = {
    val publishCommand = ("yarn" :: publishArgs).mkString(" ")
    s"""|@echo off
        |call yarn install
        |set USE_SSH=true
        |call $publishCommand
     """.stripMargin
  }

  override def projectSettings: Seq[Def.Setting[_]] =
    List(
      (docusaurusPublishGhpages / aggregate) := false,
      (docusaurusCreateSite / aggregate) := false,
      docusaurusProjectName := moduleName.value.stripSuffix("-docs"),
      docusaurusVersion := DocusaurusVersion.V3,
      MdocPlugin.mdocInternalVariables ++= List(
        "js-out-prefix" -> "assets"
      ),
      docusaurusPublishGhpages := {
        m.mdoc.toTask(" ").value
        val version = docusaurusVersion.value

        val tmp =
          if (scala.util.Properties.isWin) {
            val tmp = Files.createTempFile("docusaurus", "install_ssh.bat")
            Files.write(tmp, installSshWindows(version.publishArgs).getBytes())
            tmp
          } else {
            val tmp = Files.createTempFile("docusaurus", "install_ssh.sh")
            Files.write(tmp, installSsh(version.publishArgs).getBytes())
            tmp
          }

        tmp.toFile.setExecutable(true)
        Process(
          tmp.toString,
          cwd = website.value,
          "GIT_USER" -> gitUser(),
          "USE_SSH" -> "true"
        ).execute()
      },
      docusaurusCreateSite := {
        (Compile / m.mdoc).toTask(" ").value
        val version = docusaurusVersion.value
        Process(List("yarn", "install"), cwd = website.value).execute()
        Process("yarn" :: version.buildArgs, cwd = website.value).execute()
        val out = website.value / "build"
        val redirectUrl = docusaurusProjectName.value + "/index.html"
        if (version.createRootRedirect) {
          val html = redirectHtml(redirectUrl)
          IO.write(out / "index.html", html)
        }
        out
      },
      cleanFiles := {
        val buildFolder = website.value / "build"
        val nodeModules = website.value / "node_modules"
        val currentCleanFiles = cleanFiles.value

        val docusaurusFolders = Seq(buildFolder, nodeModules).filter(_.exists())
        docusaurusFolders ++ currentCleanFiles
      },
      doc := {
        val out = docusaurusCreateSite.value
        Relativize.htmlSite(out.toPath)
        out
      },
      (Compile / packageDoc) := fileToFileRef(Def.task {
        val directory = doc.value
        val jar = target.value / "docusaurus.jar"
        val files = listJarFiles(directory.toPath)
        IO.jar(files, jar, new java.util.jar.Manifest())
        jar
      }).value
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
