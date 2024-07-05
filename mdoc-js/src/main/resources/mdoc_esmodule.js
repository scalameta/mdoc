(function (global) {
  function findDivs() {
    return Array.from(global.document.querySelectorAll("div[data-mdoc-js]"));
  }

  function loadAll(scope) {
    findDivs().forEach(function (el) {
      const id = el.getAttribute("id").replace("-html-", "_js_");
      const moduleName = el.getAttribute("data-mdoc-module-name");
      import(moduleName).then(function (module) {
        module[id](el);
      });
    });
  }

  loadAll(global);
})(window);
