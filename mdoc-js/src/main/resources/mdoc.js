(function(global) {
  var dom = global.document;

  function findDivs() {
    return Array.from(dom.querySelectorAll("div[data-mdoc-js]"));
  }

  findDivs().forEach(function(el) {
    var id = el.getAttribute("id");
    window[id](el);
  });
})(window);
