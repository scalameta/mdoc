// See https://docusaurus.io/docs/site-config.html for all the possible
// site configuration options.

const repoUrl = "https://github.com/olafurpg/mdoc";
const gitterUrl = "https://gitter.im/olafurpg/mdoc";
const baseUrl = "/mdoc/";

const siteConfig = {
  title: "mdoc",
  tagline: "Markdown documentation for Scala",
  url: "https://olafurpg.github.io/mdoc",
  baseUrl: baseUrl,

  // Used for publishing and more
  projectName: "mdoc",
  organizationName: "olafurpg",

  // algolia: {
  //   apiKey: "c865f6d974a3072a35d4b53d48ac2307",
  //   indexName: "metals"
  // },

  // For no header links in the top nav bar -> headerLinks: [],
  headerLinks: [
    { doc: "installation", label: "Docs" },
    { blog: true, label: "Blog" },
    { href: repoUrl, label: "GitHub", external: true }
  ],

  // If you have users set above, you add it here:
  // users,

  /* path to images for header/footer */
  headerIcon: "img/mdoc-logo.png",
  footerIcon: "img/mdoc-logo.png",
  favicon: "img/favicon.ico",

  /* colors for website */
  colors: {
    primaryColor: "#FF5000",
    secondaryColor: "#B23800"
  },

  customDocsPath: "website/target/docs",

  stylesheets: [baseUrl + "css/custom.css"],

  // This copyright info is used in /core/Footer.js and blog rss/atom feeds.
  copyright: `Copyright © ${new Date().getFullYear()} mdoc developers`,

  highlight: {
    // Highlight.js theme to use for syntax highlighting in code blocks
    theme: "github"
  },

  /* On page navigation for the current documentation page */
  onPageNav: "separate",

  /* Open Graph and Twitter card images */
  ogImage: "img/scalameta-logo.png",
  twitterImage: "img/scalameta-logo.png",

  editUrl: `${repoUrl}/edit/master/docs/`,

  repoUrl,
  gitterUrl
};

module.exports = siteConfig;
