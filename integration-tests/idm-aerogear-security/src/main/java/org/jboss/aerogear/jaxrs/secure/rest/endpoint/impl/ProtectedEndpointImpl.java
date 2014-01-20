package org.jboss.aerogear.jaxrs.secure.rest.endpoint.impl;

import org.jboss.aerogear.jaxrs.secure.rest.endpoint.ProtectedEndpoint;
import org.jboss.aerogear.security.authz.Secure;

import javax.ejb.Stateless;

/**
 * Created by hmlnarik on 1/17/14.
 */
@Stateless
public class ProtectedEndpointImpl implements ProtectedEndpoint {

    public static final String DEVELOPER = "developer";
    public static final String ADMIN = "admin";

    @Override
    public String publicMethod() {
        return "publicMethod";
    }

    @Override
    @Secure({ADMIN})
    public String adminRestrictedMethod() {
        return "adminRestrictedMethod";
    }

    @Override
    @Secure({DEVELOPER})
    public String developerRestrictedMethod() {
        return "developerRestrictedMethod";
    }

    @Override
    @Secure({ADMIN, DEVELOPER})
    public String adminOrDeveloperRestrictedMethod() {
        return "adminOrDeveloperRestrictedMethod";
    }
}
