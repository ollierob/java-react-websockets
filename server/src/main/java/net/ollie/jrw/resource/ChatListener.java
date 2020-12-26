package net.ollie.jrw.resource;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatListener.class);

    public void runAsync(final int port) {

        final var server = new SocketIOServer(this.config(port));

        server.addConnectListener(socket -> {
            logger.info("Opened socket: {}", socket);
        });

        server.addDisconnectListener(socket -> {
            logger.info("Closed socket: {}", socket);
        });

        server.addEventListener("message", String.class, (socket, message, ackRequest) -> {
            logger.info("Received message: {}", message);
            server.getBroadcastOperations().sendEvent("message", message);
        });

        server.startAsync();

    }

    private Configuration config(final int port) {
        final var config = new Configuration();
        config.setHostname("localhost");
        config.setPort(port);
        return config;
    }

}
