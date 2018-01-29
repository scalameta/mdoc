'use strict';

import { ExtensionContext } from 'vscode';
import { Options as MdOptions } from 'markdown-it';

export function activate(context: ExtensionContext) {
  return {
    extendMarkdownIt(md: { options: MdOptions }) {
      const defaultHighlight = md.options.highlight;
      md.options.highlight = (code: string, lang: string) => {
        if (lang && lang.match(/\bvork\b/i)) {
          const codeToHighlight = [
            code,
            'ACTUAL VORK WILL BE HAPPENING HERE'
          ].join('\n');
          return defaultHighlight(codeToHighlight, 'scala');
        } else {
          return defaultHighlight(code, lang);
        }
      };
      return md;
    }
  };
}
