package net.ollie.jrw;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import java.util.EnumSet;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final Injector injector;

    Server(final Injector injector) {
        this.injector = injector;
    }

    public void run(final int port) throws Exception {

        logger.info("Running on port {}", port);
        final var server = new org.eclipse.jetty.server.Server(port);

        final var servletHandler = new ServletContextHandler();
        servletHandler.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false"); //Disable directory listings
        servletHandler.addEventListener(injector.getInstance(GuiceResteasyBootstrapServletContextListener.class));
        servletHandler.addServlet(new ServletHolder(this.defaultServlet()), "/*");
        servletHandler.addFilter(new FilterHolder(FilterDispatcher.class), "/*", EnumSet.of(DispatcherType.REQUEST));

        final var gzipHandler = new GzipHandler();
        gzipHandler.setHandler(servletHandler);
        server.setHandler(gzipHandler);

        server.start();
        server.join();

    }

    private Servlet defaultServlet() {
        final var resourceService = new ResourceService();
        resourceService.setEtags(true);
        return new DefaultServlet(resourceService);
    }

    public static void main(final String[] args) {
        try {
            final var injector = Guice.createInjector(new ServerModule());
            new Server(injector).run(args.length > 0 ? Integer.parseInt(args[0]) : 8090);
        } catch (Exception e) {
            logger.error("Error running server", e);
            System.exit(-1);
        }
    }

}
