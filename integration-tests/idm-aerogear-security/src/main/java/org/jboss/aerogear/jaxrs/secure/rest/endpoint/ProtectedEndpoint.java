package org.jboss.aerogear.jaxrs.secure.rest.endpoint;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Endpoint to test the {@link org.jboss.aerogear.security.authz.Secure} annotation
 */
@Path("/protected")
public interface ProtectedEndpoint {

    @GET
    @Path("/publicMethod")
    String publicMethod();

    @GET
    @Path("/adminRestrictedMethod")
    String adminRestrictedMethod();

    @GET
    @Path("/developerRestrictedMethod")
    String developerRestrictedMethod();

    @GET
    @Path("/adminOrDeveloperRestrictedMethod")
    String adminOrDeveloperRestrictedMethod();

}
