package net.ollie.jrw.resource;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Serves up chat.html file.
 */
@Path("chat")
public class ChatResource {

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response loadChatHtml() {
        final var resource = ChatResource.class.getResourceAsStream("/js/chat.html");
        if (resource == null) throw new NotFoundException();
        return Response.ok(resource).build();
    }

}
