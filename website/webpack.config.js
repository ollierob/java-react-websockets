const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = {
    entry: {
        "chat": ["./src/main/js/chat/ChatRoom.tsx"]
    },
    output: {
        path: __dirname + "/target/classes/js",
        filename: "[name].bundle.js",
        chunkFilename: "[name].bundle.js",
    },
    resolve: {
        extensions: [".ts", ".tsx", ".js", ".json", ".less"]
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: ["ts-loader"]
            }
        ]
    },
    plugins: [
        new HtmlWebpackPlugin({
            inject: false,
            chunks: ["chat"],
            publicPath: "/js",
            template: "src/main/resources/router.html.template",
            filename: "chat.html"
        })
    ],
    optimization: {
        splitChunks: {
            chunks: "all",
            maxInitialRequests: Infinity,
            minSize: 0,
            maxSize: 1000000,
            cacheGroups: {
                vendor: {
                    name: "vendors",
                    test: /node_modules/
                }
            }
        }
    }
}