/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require("react");

const CompLibrary = require("../../core/CompLibrary.js");
const Container = CompLibrary.Container;
const GridBlock = CompLibrary.GridBlock;

const siteConfig = require(process.cwd() + "/siteConfig.js");

function imgUrl(img) {
  return siteConfig.baseUrl + "img/" + img;
}

function docUrl(doc, language) {
  return siteConfig.baseUrl + "docs/" + (language ? language + "/" : "") + doc;
}

class Button extends React.Component {
  render() {
    return (
      <div className="pluginWrapper buttonWrapper">
        <a className="button" href={this.props.href} target={this.props.target}>
          {this.props.children}
        </a>
      </div>
    );
  }
}

Button.defaultProps = {
  target: "_self"
};

const SplashContainer = props => (
  <div className="homeContainer">
    <div className="homeSplashFade">
      <div className="wrapper homeWrapper">{props.children}</div>
    </div>
  </div>
);

const ProjectTitle = props => (
  <h2 className="projectTitle">
    {siteConfig.title}
    <small>{siteConfig.tagline}</small>
  </h2>
);

const PromoSection = props => (
  <div className="section promoSection">
    <div className="promoRow">
      <div className="pluginRowBlock">{props.children}</div>
    </div>
  </div>
);

class HomeSplash extends React.Component {
  render() {
    let language = this.props.language || "";
    return (
      <SplashContainer>
        <div className="inner">
          <ProjectTitle />
          <PromoSection>
            <Button href={docUrl("installation.html", language)}>
              Get Started
            </Button>
          </PromoSection>
        </div>
      </SplashContainer>
    );
  }
}

const Block = props => (
  <Container
    padding={["bottom", "top"]}
    id={props.id}
    background={props.background}
  >
    <GridBlock align="left" contents={props.children} layout={props.layout} />
  </Container>
);

const Features = props => {
  const features = [
    {
      title: "Fast",
      content: "Get quick feedback as you write documentation.",
      image:
        "https://user-images.githubusercontent.com/1408093/50530706-74dc2200-0b00-11e9-896e-b3ad115b9ffb.gif",
      imageAlign: "left"
    },
    {
      title: "Helpful",
      content:
        "Compile errors point to range positions in the original markdown source making it easy to track down problems. Dead links are reported as warnings helping you keep documentation up-to-date.",
      image:
        "https://user-images.githubusercontent.com/1408093/50537668-f6fd3280-0b62-11e9-8015-d349acfe9809.png",
      imageAlign: "right"
    },
    {
      title: "Extensible",
      content:
        "Define custom variables like `@VERSION@` or programmatically implement custom modifiers to render tailor-made documentation for your project.",
      image:
        "https://user-images.githubusercontent.com/1408093/50530764-1ebbae80-0b01-11e9-89f1-e0e9be4ef1b0.png",
      imageAlign: "left"
    }
  ];
  return (
    <div
      className="productShowcaseSection paddingBottom"
      style={{ textAlign: "left" }}
    >
      {features.map(feature => (
        <Block key={feature.title}>{[feature]}</Block>
      ))}
    </div>
  );
};
class Index extends React.Component {
  render() {
    let language = this.props.language || "";

    return (
      <div>
        <HomeSplash language={language} />
        <Features />
      </div>
    );
  }
}

module.exports = Index;
