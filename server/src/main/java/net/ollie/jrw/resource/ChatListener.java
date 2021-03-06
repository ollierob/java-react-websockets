package net.ollie.jrw.resource;

import com.google.common.collect.Sets;
import com.google.protobuf.CodedOutputStream;
import net.ollie.jrw.ChatProto;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Websocket adapter for chat messages.
 */
@WebSocket
public class ChatListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatListener.class);
    private final Set<Session> sessions;
    private final List<ChatProto.ChatMessage> messages;

    private ChatListener(
            final Set<Session> sessions,
            final List<ChatProto.ChatMessage> messages) {
        this.sessions = sessions;
        this.messages = messages;
    }

    @OnWebSocketConnect
    public void onWebSocketConnect(final Session session) {
        logger.info("Session connected: {}", session);
        sessions.add(session);
        //Replay messages
        try {
            for (final ChatProto.ChatMessage message : messages) {
                send(message, session);
            }
        } catch (final Exception ex) {
            logger.warn("Could not replay messages to session " + session, ex);
        }
    }

    @OnWebSocketMessage
    public void onWebSocketText(final Session sourceSession, final String messageText) {
        logger.info("Received text from {}: {}", sourceSession, messageText);
        final var message = createMessage(sourceSession, messageText);
        messages.add(message);
        for (final var iterator = sessions.iterator(); iterator.hasNext(); ) {
            final var session = iterator.next();
            if (!session.isOpen()) {
                //Tidy up
                iterator.remove();
                continue;
            }
            try {
                send(message, session);
            } catch (final Exception ex) {
                logger.warn("Could not send message to session " + session, ex);
            }
        }
    }

    private static void send(final ChatProto.ChatMessage message, final Session session) throws IOException {
        final ByteBuffer bb = ByteBuffer.allocateDirect(message.getSerializedSize());
        message.writeTo(CodedOutputStream.newInstance(bb));
        session.getRemote().sendBytes(bb);
    }

    private static ChatProto.ChatMessage createMessage(final Session session, final String message) {
        final var users = session.getUpgradeRequest().getParameterMap().get("username");
        final var user = users == null || users.size() != 1 ? "?" : users.get(0);
        return ChatProto.ChatMessage.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setUser(user)
                .setMessage(message)
                .build();
    }

    @OnWebSocketError
    public void onWebSocketError(final Session session, final Throwable cause) {
        logger.warn("Socket error on session " + session, cause);
    }

    @OnWebSocketClose
    public void onWebSocketClose(final Session session, final int statusCode, final String reason) {
        if (session != null) sessions.remove(session);
        logger.warn("Session {} closed: {}", session, reason);
    }

    @Singleton
    public static class Factory implements WebSocketCreator {

        private final Set<Session> sessions = Sets.newConcurrentHashSet();
        private final List<ChatProto.ChatMessage> messages = new CopyOnWriteArrayList<>();
        private final ChatListener listener = new ChatListener(sessions, messages);

        @Override
        public Object createWebSocket(
                final ServletUpgradeRequest request,
                final ServletUpgradeResponse response) {
            return listener;
        }

    }

}
