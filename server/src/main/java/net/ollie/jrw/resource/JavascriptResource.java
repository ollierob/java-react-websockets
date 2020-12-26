package net.ollie.jrw.resource;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("js")
public class JavascriptResource {

    @GET
    @Path("{file}")
    @Produces("application/javascript")
    public Response readJs(@PathParam("file") final String file) {
        if (file.startsWith(".") || !file.endsWith(".js") || file.contains("/")) throw new NotFoundException();
        final var resource = this.getClass().getResourceAsStream("/js/" + file);
        if (resource == null) throw new NotFoundException();
        return Response.ok(resource).build();
    }

}
