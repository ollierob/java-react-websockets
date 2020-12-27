package net.ollie.jrw;

import com.google.inject.Guice;
import net.ollie.jrw.resource.ChatListener;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.NativeWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import java.util.EnumSet;

/**
 * Runs a Jetty server.
 * <p>
 * Regular HTTP endpoints are bound with Guice.
 * <p>
 * WebSocket endpoints are manually installed here.
 */
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final GuiceResteasyBootstrapServletContextListener contextListener;
    private final ChatListener.Factory listenerFactory;

    @Inject
    Server(final GuiceResteasyBootstrapServletContextListener contextListener, final ChatListener.Factory listenerFactory) {
        this.contextListener = contextListener;
        this.listenerFactory = listenerFactory;
    }

    public void run(final int port) {

        try {

            logger.info("Running on port {}", port);
            final var server = new org.eclipse.jetty.server.Server(port);

            final var context = new ServletContextHandler();
            context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false"); //Disable directory listings
            context.addEventListener(contextListener);
            context.addServlet(new ServletHolder(this.defaultServlet()), "/*");
            context.addFilter(new FilterHolder(FilterDispatcher.class), "/*", EnumSet.of(DispatcherType.REQUEST));

            final var gzipHandler = new GzipHandler();
            gzipHandler.setHandler(context);
            server.setHandler(gzipHandler);

            //WS
            NativeWebSocketServletContainerInitializer.configure(context, (sc, config) -> config.addMapping("/chat/subscribe", listenerFactory));
            WebSocketUpgradeFilter.configure(context);

            server.start();
            server.join();

        } catch (final Exception ex) {
            logger.error("Error running Jetty", ex);
            throw new Error(ex);
        }

    }

    public void runAsync(final int port) {
        new Thread(() -> this.run(port)).start();
    }

    private Servlet defaultServlet() {
        final var resourceService = new ResourceService();
        resourceService.setEtags(true);
        return new DefaultServlet(resourceService);
    }

    public static void main(final String[] args) {
        try {
            final var injector = Guice.createInjector(new ServerModule());
            //Run Jetty thread
            injector.getInstance(Server.class).run(args.length > 0 ? Integer.parseInt(args[0]) : 8090);
        } catch (final Exception e) {
            logger.error("Error running server", e);
            System.exit(-1);
        }
    }

}
