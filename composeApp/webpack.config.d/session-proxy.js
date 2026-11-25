if (config.devServer) {
    config.devServer.host = "127.0.0.1";
    config.devServer.proxy = [
        {
            context: ["/api/auth/web-session"],
            target: "http://127.0.0.1:5000",
        },
    ];
}
