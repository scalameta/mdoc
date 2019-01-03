(function(global) {
  function findDivs() {
    return Array.from(global.document.querySelectorAll("div[data-mdoc-js]"));
  }

  function loadAll(scope) {
    findDivs().forEach(function(el) {
      var id = el.getAttribute("id");
      scope[id](el);
    });
  }

  loadAll(global);
})(window);
