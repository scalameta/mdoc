"use strict";

import * as path from "path";
import {
  workspace,
  ExtensionContext,
  window,
  commands,
  StatusBarAlignment,
  ViewColumn,
  WebviewPanel
} from "vscode";
import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  RevealOutputChannelOn,
  ExecuteCommandRequest
} from "vscode-languageclient";
import { Options } from "markdown-it";
import { Mdoc } from "./protocol";

export async function activate(context: ExtensionContext) {
  const coursierPath = path.join(context.extensionPath, "./coursier");

  const serverVersion = workspace.getConfiguration("mdoc").get("serverVersion");

  const javaArgs = [
    `-Xss4m`,
    `-Xms1G`,
    `-Xmx4G`,
    `-XX:+UseG1GC`,
    `-XX:+UseStringDeduplication`,
    "-jar",
    coursierPath
  ];

  const coursierLaunchArgs = [
    "launch",
    "-r",
    "sonatype:releases",
    "-r",
    "sonatype:snapshots",
    `com.geirsson:mdoc-lsp_2.12:${serverVersion}`,
    "-M",
    "mdoc.LspMain"
  ];

  const launchArgs = javaArgs.concat(coursierLaunchArgs);

  const serverOptions: ServerOptions = {
    run: { command: "java", args: launchArgs },
    debug: { command: "java", args: launchArgs }
  };

  const clientOptions: LanguageClientOptions = {
    documentSelector: [{ scheme: "file", language: "markdown" }],
    synchronize: {
      configurationSection: "mdoc"
    },
    revealOutputChannelOn: RevealOutputChannelOn.Never
  };

  const client = new LanguageClient(
    "mdoc",
    "mdoc",
    serverOptions,
    clientOptions
  );

  context.subscriptions.push(client.start());

  client.onReady().then(_ => {
    context.subscriptions.push(
      commands.registerCommand("mdoc.preview", () => {
        const filename = path.basename(
          window.activeTextEditor.document.fileName
        );
        const panel = window.createWebviewPanel(
          "mdoc",
          `[preview] ${filename}`,
          ViewColumn.Beside,
          { enableScripts: true }
        );
        client.onNotification(Mdoc.preview, html => {
          panel.webview.html = html;
        });
        client
          .sendRequest(
            Mdoc.index,
            window.activeTextEditor.document.uri.toString()
          )
          .then(html => {
            panel.webview.html = html;
          });
      })
    );

    // The server updates the client with a brief text message about what
    // it is currently doing, for example "Compiling..".
    const item = window.createStatusBarItem(StatusBarAlignment.Right, 100);
    item.hide();
    client.onNotification(Mdoc.status, params => {
      item.text = params.text;
      if (params.show) {
        item.show();
      } else if (params.hide) {
        item.hide();
      }
      if (params.tooltip) {
        item.tooltip = params.tooltip;
      }
      if (params.command) {
        item.command = params.command;
        commands.getCommands().then(values => {
          if (values.indexOf(params.command) < 0) {
            commands.registerCommand(params.command, () => {
              client.sendRequest(ExecuteCommandRequest.type, {
                command: params.command
              });
            });
          }
        });
      } else {
        item.command = undefined;
      }
    });
  });
}
