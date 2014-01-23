package org.jboss.aerogear.jaxrs.secure.rest.endpoint.impl;

import org.jboss.aerogear.jaxrs.demo.user.UserRoles;
import org.jboss.aerogear.jaxrs.secure.rest.endpoint.ProtectedEndpoint;
import org.jboss.aerogear.security.authz.Secure;

import javax.ejb.Stateless;

/**
 * Created by hmlnarik on 1/17/14.
 */
@Stateless
public class ProtectedEndpointImpl implements ProtectedEndpoint {

    @Override
    public String publicMethod() {
        return "publicMethod";
    }

    @Override
    @Secure({UserRoles.ADMIN})
    public String adminRestrictedMethod() {
        return "adminRestrictedMethod";
    }

    @Override
    // @Secure annotation should be inherited from interface.
    // It does not work as of now, hence it is here
    @Secure({UserRoles.USER})
    public String userRestrictedMethod() {
        return "userRestrictedMethod";
    }

    @Override
    @Secure({UserRoles.ADMIN, UserRoles.USER})
    public String adminOrUserRestrictedMethod() {
        return "adminOrUserRestrictedMethod";
    }
}
