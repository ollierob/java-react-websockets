package net.ollie.jrw.resource;

import com.google.common.collect.Sets;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WebSocketConnectionListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatListener extends WebSocketAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ChatListener.class);
    private final Set<Session> sessions;
    private final List<String> messages;

    private ChatListener(final Set<Session> sessions, final List<String> messages) {
        this.sessions = sessions;
        this.messages = messages;
    }

    @Override
    public void onWebSocketConnect(final Session session) {
        super.onWebSocketConnect(session);
        logger.info("Session connected: {}", session);
        sessions.add(session);
        //Replay messages
        try {
            for (final String message : messages) {
                session.getRemote().sendString(message);
            }
        } catch (final Exception ex) {
            logger.warn("Could not replay messages to session " + session, ex);
        }
    }

    @Override
    public void onWebSocketText(final String message) {
        super.onWebSocketText(message);
        logger.info("Received text: {}", message);
        messages.add(message);
        for (final var iterator = sessions.iterator(); iterator.hasNext(); ) {
            final var session = iterator.next();
            if (!session.isOpen()) {
                //Tidy up
                iterator.remove();
                continue;
            }
            try {
                session.getRemote().sendString(message);
            } catch (final Exception ex) {
                logger.warn("Could not send message to session " + session, ex);
            }
        }
    }

    @Override
    public void onWebSocketError(final Throwable cause) {
        super.onWebSocketError(cause);
        logger.warn("Socket error:", cause);
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason) {
        final var session = this.getSession();
        if (session != null) sessions.remove(session);
        super.onWebSocketClose(statusCode, reason);
        logger.warn("Session closed: {}", reason);
    }

    @Singleton
    public static class Factory implements WebSocketCreator {

        private final Set<Session> sessions = Sets.newConcurrentHashSet();
        private final List<String> messages = new CopyOnWriteArrayList<>();

        @Override
        public WebSocketConnectionListener createWebSocket(
                final ServletUpgradeRequest servletUpgradeRequest,
                final ServletUpgradeResponse servletUpgradeResponse) {
            return new ChatListener(sessions, messages);
        }

    }

}
