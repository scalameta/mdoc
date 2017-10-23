$!
  Copyright (c) 2016-2017 Martin Donath <martin.donath@squidfunk.com>
  Copyright (c) 2017 Jonas Fonseca <jonas.fonseca@gmail.com>

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to
  deal in the Software without restriction, including without limitation the
  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
  sell copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
  IN THE SOFTWARE.
!$
<!DOCTYPE html>
<html lang="$page.properties.("material.language"); null="en"$" class="no-js">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta http-equiv="x-ua-compatible" content="ie=edge">
    <meta name="description" content="$page.properties.("project.description")$">
    <meta name="generator" content="Paradox, paradox-material-theme, mkdocs-material=1.11.0">

    $partials/language()$

    $ if (page.properties.("description")) $
      <meta name="description" content="$page.properties.("description")$">
    $ elseif (page.properties.("project.description")) $
      <meta name="description" content="$page.properties.("project.description")$">
    $ endif $
    $ if (page.properties.("material.canonical.url")) $
      <link rel="canonical" href="$page.properties.("material.canonical.url")$">
    $ endif $
    $ if (page.properties.("author")) $
      <meta name="author" content="$page.properties.("author")$">
    $ elseif (page.properties.("material.author")) $
      <meta name="author" content="$page.properties.("site.author")$">
    $ endif $
    $ if (page.properties.("material.favicon")) $
      <link rel="shortcut icon" href="$page.base$$page.properties.("material.favicon")$">
    $ else $
      <link rel="shortcut icon" href="$page.base$assets/images/favicon.png">
    $ endif $
    $ if (page.properties.("title")) $
      <title>$page.properties.("title")$</title>
    $ elseif (page.title) $
      <title>$page.title$$ if (!page.home.active) $ · $page.home.title$$ endif $</title>
    $ else $
      <title>$page.home.title$</title>
    $ endif $
    <script src="$page.base$lib/modernizr/modernizr.min.js"></script>
    <link rel="stylesheet" href="$page.base$assets/stylesheets/application-0741cbeb94.css">
    $ if (page.properties.("material.color.primary")) $
      <link rel="stylesheet" href="$page.base$assets/stylesheets/application-23f75ab9c7.palette.css">
    $ endif $
    <link rel="stylesheet" href="$page.base$lib/material__tabs/dist/mdc.tabs.min.css">
    <link rel="stylesheet" href="$page.base$lib/prettify/prettify.css">
    $ if (!page.properties.("material.font.disabled")) $
      <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=$page.properties.("material.font.text"); null="Roboto"$:300,400,400i,700|$page.properties.("material.font.code"); null="Roboto+Mono"$">
      <style>
        body,input{font-family:"$page.properties.("material.font.text"); null="Roboto"$","Helvetica Neue",Helvetica,Arial,sans-serif}
        code,kbd,pre{font-family:"$page.properties.("material.font.code"); null="Roboto Mono"$","Courier New",Courier,monospace}
      </style>
      <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
      <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">
    $ endif $
    <link rel="stylesheet" href="$page.base$assets/stylesheets/paradox-material-theme.css">
    $ if (page.properties.("material.custom.stylesheet")) $
      <link rel="stylesheet" href="$page.base$$page.properties.("material.custom.stylesheet")$">
    $ endif $
  </head>
  <body
      data-md-color-primary="$page.properties.("material.color.primary"); null="indigo"$"
      data-md-color-accent="$page.properties.("material.color.accent"); null="indigo"$">
    <input class="md-toggle" data-md-toggle="drawer" type="checkbox" id="drawer">
    <input class="md-toggle" data-md-toggle="search" type="checkbox" id="search">
    <label class="md-overlay" data-md-component="overlay" for="drawer"></label>
    $partials/header()$
    <div class="md-container">
      <main class="md-main">
        <div class="md-main__inner md-grid" data-md-component="container">
          <div class="md-sidebar md-sidebar--primary" data-md-component="navigation">
            <div class="md-sidebar__scrollwrap">
              <div class="md-sidebar__inner">
                $partials/nav()$
              </div>
            </div>
          </div>
          $ if (page.subheaders) $
            <div class="md-sidebar md-sidebar--secondary" data-md-component="toc">
              <div class="md-sidebar__scrollwrap">
                <div class="md-sidebar__inner">
                  $partials/toc()$
                </div>
              </div>
            </div>
          $ endif $
          <div class="md-content">
            <article class="md-content__inner md-typeset">
              <div class="md-content__searchable">
                $page.content$
              </div>
              $ if (page.source_url) $
                <div>
                  <a href="$page.source_url$" title="Edit this page" class="md-source-file md-edit">
                    Edit this page
                  </a>
                </div>
              $ endif $
              <div class="print-only">
                <span class="md-source-file md-version">
                  $page.properties.("project.version.short")$
                </span>
              </div>
            </article>
          </div>
        </div>
      </main>
      $partials/footer()$
    </div>
    <script src="$page.base$assets/javascripts/application-268d62d82d.js"></script>
    <script src="$page.base$assets/javascripts/paradox-material-theme.js"></script>
    <script>app.initialize({url:{base:"$page.base$./"}})</script>
    <script type="text/javascript" src="$page.base$lib/prettify/prettify.js"></script>
    <script type="text/javascript" src="$page.base$lib/prettify/lang-scala.js"></script>
    <script type="text/javascript">
      document.addEventListener("DOMContentLoaded", function(event) {
        window.prettyPrint && prettyPrint();
      });
    </script>
    $ if (page.properties.("material.custom.javascript")) $
      <script src="$page.base$$page.properties.("material.custom.javascript")$"></script>
    $ endif $
    $ if (page.properties.("material.google.analytics")) $
      <script>!function(e,t,a,n,o,c,i){e.GoogleAnalyticsObject=o,e[o]=e[o]||function(){(e[o].q=e[o].q||[]).push(arguments)},e[o].l=1*new Date,c=t.createElement(a),i=t.getElementsByTagName(a)[0],c.async=1,c.src=n,i.parentNode.insertBefore(c,i)}(window,document,"script","https://www.google-analytics.com/analytics.js","ga"),ga("create","$page.properties.("material.google.analytics")$","auto"),ga("set","anonymizeIp",!0),ga("send","pageview");var links=document.getElementsByTagName("a");Array.prototype.map.call(links,function(e){e.host!=document.location.host&&e.addEventListener("click",function(){var t=e.getAttribute("data-md-action")||"follow";ga("send","event","outbound",t,e.href)})});var query=document.forms.search.query;query.addEventListener("blur",function(){if(this.value){var e=document.location.pathname;ga("send","pageview",e+"?q="+this.value)}})</script>
    $ endif $
  </body>
</html>
