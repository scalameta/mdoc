import { RequestType, NotificationType } from "vscode-jsonrpc";

"use strict";

export namespace Mdoc {
  export const status = new NotificationType<MdocStatusParams, void>(
    "mdoc/status"
  );
  export const preview = new NotificationType<string, void>("mdoc/preview");
  export const index = new RequestType<string, string, void, void>(
    "mdoc/index"
  );
}
export interface MdocStatusParams {
  text: string;
  show?: boolean;
  hide?: boolean;
  tooltip?: string;
  command?: string;
}
