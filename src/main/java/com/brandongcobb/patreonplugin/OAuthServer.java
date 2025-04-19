package com.brandongcobb.patreonplugin;

import spark.Spark;

public class OAuthServer {
    private final PatreonPlugin plugin;

    public OAuthServer(PatreonPlugin plugin) {
        this.plugin = plugin;
        Spark.port(8000); // or any port you wish to use
        Spark.get("/oauth/patreon_callback", (req, res) -> {
            String code = req.queryParams("code");
            plugin.handleOAuthCallback(code); // Pass the code to your plugin
            res.status(200);
            return "OAuth code received!";
        });
    }

    public void start() {
        Spark.init();
    }

    public void stop() {
        Spark.stop();
    }
}
