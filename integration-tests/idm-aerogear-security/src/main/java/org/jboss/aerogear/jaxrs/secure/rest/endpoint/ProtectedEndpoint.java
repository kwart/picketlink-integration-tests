package org.jboss.aerogear.jaxrs.secure.rest.endpoint;

import org.jboss.aerogear.jaxrs.demo.user.UserRoles;
import org.jboss.aerogear.security.authz.Secure;

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
    @Path("/userRestrictedMethod")
//    @Secure({UserRoles.USER})
    String userRestrictedMethod();

    @GET
    @Path("/adminOrUserRestrictedMethod")
    String adminOrUserRestrictedMethod();

}
