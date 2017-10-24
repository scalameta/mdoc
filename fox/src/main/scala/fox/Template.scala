package fox

import scala.xml.Elem
import fox.Markdown.Doc
import fox.Markdown.Site

class Template(options: Options, logger: Logger) {
  val github = <svg class="md-svg">
    <defs>
      <svg xmlns="http://www.w3.org/2000/svg" width="416" height="448" viewBox="0 0 416 448" id="github">
        <path fill="currentColor" d="M160 304q0 10-3.125 20.5t-10.75 19T128 352t-18.125-8.5-10.75-19T96 304t3.125-20.5 10.75-19T128 256t18.125 8.5 10.75 19T160 304zm160 0q0 10-3.125 20.5t-10.75 19T288 352t-18.125-8.5-10.75-19T256 304t3.125-20.5 10.75-19T288 256t18.125 8.5 10.75 19T320 304zm40 0q0-30-17.25-51T296 232q-10.25 0-48.75 5.25Q229.5 240 208 240t-39.25-2.75Q130.75 232 120 232q-29.5 0-46.75 21T56 304q0 22 8 38.375t20.25 25.75 30.5 15 35 7.375 37.25 1.75h42q20.5 0 37.25-1.75t35-7.375 30.5-15 20.25-25.75T360 304zm56-44q0 51.75-15.25 82.75-9.5 19.25-26.375 33.25t-35.25 21.5-42.5 11.875-42.875 5.5T212 416q-19.5 0-35.5-.75t-36.875-3.125-38.125-7.5-34.25-12.875T37 371.5t-21.5-28.75Q0 312 0 260q0-59.25 34-99-6.75-20.5-6.75-42.5 0-29 12.75-54.5 27 0 47.5 9.875t47.25 30.875Q171.5 96 212 96q37 0 70 8 26.25-20.5 46.75-30.25T376 64q12.75 25.5 12.75 54.5 0 21.75-6.75 42 34 40 34 99.5z"/>
      </svg>

    </defs>
  </svg>

  val header =
    <header class="md-header" data-md-component="header" data-md-state="shadow">
      <nav class="md-header-nav md-grid">
        <div class="md-flex">
          <div class="md-flex__cell md-flex__cell--shrink">
            <a href={options.docs} title={options.title} class="md-header-nav__button md-logo">
              <i class="md-icon md-icon--home"></i>
            </a>
          </div>
          <div class="md-flex__cell md-flex__cell--shrink">
            <label class="md-icon md-icon--menu md-header-nav__button" for="drawer"></label>
          </div>
          <div class="md-flex__cell md-flex__cell--stretch">
            <span class="md-flex__ellipsis md-header-nav__title">
              {options.title}
            </span>
          </div>
          <div class="md-flex__cell md-flex__cell--shrink">
            <label class="md-icon md-icon--search md-header-nav__button" for="search"></label>
            <div class="md-search" data-md-component="search" role="dialog">
              <label class="md-search__overlay" for="search"></label>
              <div class="md-search__inner">
                <form class="md-search__form" name="search">
                  <input type="text" class="md-search__input" name="query" required="" placeholder="Search" autocapitalize="off" autocorrect="off" autocomplete="off" spellcheck="false" data-md-component="query"/>
                  <label class="md-icon md-search__icon" for="search"></label>
                  <button type="reset" class="md-icon md-search__icon" data-md-component="reset"></button>
                </form>
                <div class="md-search__output">
                  <div class="md-search__scrollwrap" data-md-scrollfix="">
                    <div class="md-search-result" data-md-component="result">
                      <div class="md-search-result__meta">
                        Type to start searching
                      </div>
                      <ol class="md-search-result__list"></ol>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="md-flex__cell md-flex__cell--shrink">
            <div class="md-header-nav__source">
              <a href={options.github} title="Go to repository" class="md-source" data-md-source="github" data-md-state="done">
                <div class="md-source__icon">
                  <svg viewBox="0 0 24 24" width="24" height="24">
                    <use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#github" width="24" height="24"></use>
                  </svg>
                </div>
                <div class="md-source__repository">
                  {options.org}/{options.repo}
                  <ul class="md-source__facts">
                    <li class="md-source__fact">{options.stars} stars</li> <li class="md-source__fact">{options.forks} forks</li>
                  </ul>
                </div>
              </a>
            </div>
          </div>
        </div>
      </nav>
    </header>

  def render(doc: Doc, site: Site): xml.Node = html {
    <body data-md-color-primary="light-blue" data-md-color-accent="orange" data-md-state="">
      <input class="md-toggle" data-md-toggle="drawer" type="checkbox" id="drawer"/>
      <input class="md-toggle" data-md-toggle="search" type="checkbox" id="search"/>
      <label class="md-overlay" data-md-component="overlay" for="drawer"></label>{header}<div class="md-container">
      <main class="md-main">
        <div class="md-main__inner md-grid" data-md-component="container">
          {sections(site)}{toc(doc)}<div class="md-content">
          <article class="md-content__inner md-typeset">
            {xml.Unparsed(doc.contents)}
          </article>
        </div>
        </div>
      </main>
    </div>
    </body>
  }

  def html(body: xml.Node) =
    <html lang="en" class="no-js">
      {head}{body}
    </html>

  def head: Elem = <head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width,initial-scale=1"/>
    <meta http-equiv="x-ua-compatible" content="ie=edge"/>
    <meta name="description" content={options.description}/>
    <link rel="canonical" href={options.docs}/>
    <meta name="author" content="Martin Donath"/>
    <meta name="lang:clipboard.copy" content="Copy to clipboard"/>
    <meta name="lang:clipboard.copied" content="Copied to clipboard"/>
    <meta name="lang:search.languages" content=""/>
    <meta name="lang:search.result.none" content="No matching documents"/>
    <meta name="lang:search.result.one" content="1 matching document"/>
    <meta name="lang:search.result.other" content="# matching documents"/>
    <meta name="lang:search.tokenizer" content="[\s\-]+"/>
    <link rel="shortcut icon" href="./assets/images/favicon.png"/>
    <meta name="generator" content="mkdocs-0.16.3, mkdocs-material-1.12.1"/>
    <title>{options.title}</title>
    <script src={options.lib("modernizr/modernizr.min.js")}></script>
    <link rel="stylesheet" href={options.asset("stylesheets/application-04ea671600.css")}/>
    <link rel="stylesheet" href={options.asset("stylesheets/application-23f75ab9c7.palette.css")}/>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:300,400,400i,700|Roboto+Mono"/>
    <style>
      body,input{{font-family:"Roboto","Helvetica Neue",Helvetica,Arial,sans-serif}}code,kbd,pre{{font-family:"Roboto Mono","Courier New",Courier,monospace}}
    </style>
    <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons"/>
  </head>

  def sections(site: Site) =
    <div class="md-sidebar md-sidebar--primary" data-md-component="navigation" data-md-state="lock">
      <div class="md-sidebar__scrollwrap">
        <div class="md-sidebar__inner">
          <nav class="md-nav md-nav--primary" data-md-level="0">
            <label class="md-nav__title md-nav__title--site" for="drawer">
              <span class="md-nav__button md-logo">
                <i class="md-icon md-icon--home"></i>
              </span>
              {options.title}
            </label>

            <div class="md-nav__source">
              <a href={options.github} title="Go to repository" class="md-source" data-md-source="github" data-md-state="done">
                <div class="md-source__icon">
                  <svg viewBox="0 0 24 24" width="24" height="24">
                    <use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#github" width="24" height="24"></use>
                  </svg>
                </div>
                <div class="md-source__repository">
                  {options.org}/{options.repo}
                  <ul class="md-source__facts">
                    <li class="md-source__fact">{options.stars} stars</li> <li class="md-source__fact">{options.forks} forks</li>
                  </ul>
                </div>
              </a>
            </div>
            <ul class="md-nav__list" data-md-scrollfix="">
              {site.docs.map(doc =>
              <li class="md-nav__item">
                <a href={options.href(doc)} title={doc.title} class="md-nav__link">
                  {doc.title}
                </a>
              </li>
            )}
            </ul>
          </nav>
        </div>
      </div>
    </div>

  def toc(doc: Doc): Elem =
    <div class="md-sidebar md-sidebar--secondary" data-md-component="toc" data-md-state="lock">
      <div class="md-sidebar__scrollwrap">
        <div class="md-sidebar__inner">
          <nav class="md-nav md-nav--secondary">
            <label class="md-nav__title" for="toc">Table of contents</label>
            <ul class="md-nav__list" data-md-scrollfix="">
              {doc.headers.withFilter(_.level == 2).map(header =>
              <li class="md-nav__item">
                <a href={header.target} title="What to expect" class="md-nav__link" data-md-state="">
                  {header.title}
                </a>
              </li>
            )}
            </ul>
          </nav>
        </div>
      </div>
    </div>

  //    <ul>
  //  </ul>
}
