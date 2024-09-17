# Running the website locally

Before starting;

1. Make sure the project compiles.
2. *Ensure you are using node v16*. (As of 22.07.2024) The website will not work with higher versions of node. Consider using nvm to manage node versions.

Next

1. Switch to the "website" folder. run `npm install`
2. Copy the blog folder into the `website` folder (don't commit that)
3. Back in the project root, run `sbt '++2.12.19; docs/mdoc'` in a terminal - this will run mdoc.
4. probably in a new terminal in the "website" folder , run `npm start`

