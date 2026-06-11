// @ts-check

const config = {
  title: "mdoc Docusaurus v3",
  url: "https://example.com",
  baseUrl: "/",
  organizationName: "scalameta",
  projectName: "mdoc-docusaurus-v3",
  onBrokenLinks: "throw",
  presets: [
    [
      "classic",
      {
        docs: {
          routeBasePath: "/",
          sidebarPath: require.resolve("./sidebars.js")
        },
        blog: false
      }
    ]
  ]
};

module.exports = config;
