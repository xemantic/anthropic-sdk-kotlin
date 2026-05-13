const webpack = require("webpack");
const envPlugin = new webpack.DefinePlugin({
  'process': {
    'env': {
      'ANTHROPIC_API_KEY': JSON.stringify(process.env.ANTHROPIC_API_KEY),
      'MOONSHOT_API_BASE_URL': JSON.stringify(process.env.MOONSHOT_API_BASE_URL),
      'MOONSHOT_DEFAULT_MODEL': JSON.stringify(process.env.MOONSHOT_DEFAULT_MODEL),
      'MOONSHOT_API_KEY': JSON.stringify(process.env.MOONSHOT_API_KEY)
    }
  }
});
config.plugins.push(envPlugin);
