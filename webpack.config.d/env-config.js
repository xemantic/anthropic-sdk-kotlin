const webpack = require("webpack");
const envPlugin = new webpack.DefinePlugin({
  'process': {
    'env': {
      'ANTHROPIC_API_KEY': JSON.stringify(process.env.ANTHROPIC_API_KEY)
    }
  }
});
config.plugins.push(envPlugin);
