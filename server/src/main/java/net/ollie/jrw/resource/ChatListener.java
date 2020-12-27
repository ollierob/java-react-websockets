package net.ollie.jrw.resource;

import com.google.common.collect.Sets;
import net.ollie.jrw.ChatProto;
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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Websocket adapter for chat messages.
 */
public class ChatListener extends WebSocketAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ChatListener.class);
    private final String username;
    private final Set<Session> sessions;
    private final List<ChatProto.ChatMessage> messages;

    private ChatListener(
            final String username,
            final Set<Session> sessions,
            final List<ChatProto.ChatMessage> messages) {
        this.username = username;
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
            for (final ChatProto.ChatMessage message : messages) {
                //TODO re-use buffer
                session.getRemote().sendBytes(message.toByteString().asReadOnlyByteBuffer());
            }
        } catch (final Exception ex) {
            logger.warn("Could not replay messages to session " + session, ex);
        }
    }

    @Override
    public void onWebSocketText(final String message) {
        super.onWebSocketText(message);
        logger.info("Received text: {}", message);
        final var proto = createMessage(message);
        messages.add(proto);
        for (final var iterator = sessions.iterator(); iterator.hasNext(); ) {
            final var session = iterator.next();
            if (!session.isOpen()) {
                //Tidy up
                iterator.remove();
                continue;
            }
            try {
                session.getRemote().sendBytes(proto.toByteString().asReadOnlyByteBuffer());
            } catch (final Exception ex) {
                logger.warn("Could not send message to session " + session, ex);
            }
        }
    }

    private ChatProto.ChatMessage createMessage(final String message) {
        return ChatProto.ChatMessage.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setUser(username)
                .setMessage(message)
                .build();
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
        private final List<ChatProto.ChatMessage> messages = new CopyOnWriteArrayList<>();

        @Override
        public WebSocketConnectionListener createWebSocket(
                final ServletUpgradeRequest request,
                final ServletUpgradeResponse response) {
            final var users = request.getParameterMap().get("username");
            final var user = users == null || users.size() != 1 ? "?" : users.get(0);
            return new ChatListener(user, sessions, messages);
        }

    }

}
