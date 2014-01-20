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

import org.jboss.aerogear.jaxrs.demo.rest.endpoint.RegistrationEndpoint;
import org.jboss.aerogear.jaxrs.demo.service.UserRegistrator;
import org.jboss.aerogear.jaxrs.demo.service.UserValidationException;
import org.jboss.aerogear.jaxrs.demo.user.SimpleUser;
import org.jboss.aerogear.jaxrs.demo.utils.Utils;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.basic.BasicModel;

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
public class RegistrationEndpointImpl implements RegistrationEndpoint {

    @Inject
    private IdentityManager identityManager;

    @Inject
    private UserRegistrator userRegistrator;

    @Override
    public Response register(SimpleUser userToRegister) {

        Map<String, Object> response = new HashMap<String, Object>();

        SimpleUser registeredUser = null;

        try {
            Utils.validate(userToRegister);
        } catch (UserValidationException ex) {
            Response.status(Status.BAD_REQUEST).build();
        }

        try {
            if (BasicModel.getUser(this.identityManager, userToRegister.getLoginName()) == null) {
                registeredUser = userRegistrator.register(userToRegister);

                return Response.ok().entity(registeredUser).build();
            } else {
                response.put("error", "This username already exists. Try another one!");
            }
        } catch (IdentityManagementException ex) {
            response.put("error", "Registration failed.");
        }

        return Response.noContent().status(Status.BAD_REQUEST).entity(response).build();
    }
}
