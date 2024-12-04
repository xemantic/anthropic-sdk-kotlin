var webpack = require("webpack");

var definePlugin = new webpack.DefinePlugin({
  'process.env.ANTHROPIC_API_KEY': JSON.stringify(process.env.ANTHROPIC_API_KEY)
});

config.plugins.push(definePlugin);
