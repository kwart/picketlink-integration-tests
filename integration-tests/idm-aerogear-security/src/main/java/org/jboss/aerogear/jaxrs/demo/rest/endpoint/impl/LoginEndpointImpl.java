/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.jaxrs.demo.rest.endpoint.impl;

import org.jboss.aerogear.jaxrs.demo.rest.endpoint.LoginEndpoint;
import org.jboss.aerogear.jaxrs.demo.service.UserValidationException;
import org.jboss.aerogear.jaxrs.demo.user.SimpleUser;
import org.jboss.aerogear.jaxrs.demo.utils.Utils;
import org.jboss.aerogear.security.auth.AuthenticationManager;
import org.jboss.aerogear.security.exception.AeroGearSecurityException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
@Stateless
public class LoginEndpointImpl implements LoginEndpoint {

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private IdentityManager identityManager;

    @Override
    public Response login(SimpleUser userToLogin) {

        Map<String, Object> response = new HashMap<String, Object>();

        try {
            validate(userToLogin);
        } catch (UserValidationException ex) {
            response.put("error", ex.getMessage());
            return Response.status(Status.BAD_REQUEST).entity(response).build();
        }

        User user = BasicModel.getUser(identityManager, userToLogin.getLoginName());

        if (user == null) {
            response.put("error", "User is null!");
            return Response.status(Status.BAD_REQUEST).entity(response).build();
        }

        try {
            authenticationManager.login(user, userToLogin.getPassword());
            return Response.status(Status.OK).build();
        } catch (AeroGearSecurityException ex) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }

    @Override
    public String hello() {
        return "Hello";
    }

    private void validate(SimpleUser userToLogin) {
        if (Utils.nullOrEmpty(userToLogin.getLoginName())
            || Utils.nullOrEmpty(userToLogin.getPassword())) {
            throw new UserValidationException("Login name or password is null or empty string!");
        }
    }
}
