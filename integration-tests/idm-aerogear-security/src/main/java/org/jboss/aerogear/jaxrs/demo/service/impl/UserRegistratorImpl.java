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
package org.jboss.aerogear.jaxrs.demo.service.impl;

import org.jboss.aerogear.jaxrs.demo.service.UserRegistrator;
import org.jboss.aerogear.jaxrs.demo.service.UserValidationException;
import org.jboss.aerogear.jaxrs.demo.user.SimpleUser;
import org.jboss.aerogear.jaxrs.demo.utils.Utils;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;

import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
@Stateless
public class UserRegistratorImpl implements UserRegistrator {

    @Inject
    private IdentityManager identityManager;

    @Inject
    private RelationshipManager relationshipManager;

    public SimpleUser register(SimpleUser userToRegister) throws UserValidationException {

        // first we validate him
        Utils.validate(userToRegister);

        // then we register him
        User user = getUser(userToRegister);
        this.identityManager.add(user);

        // we set credentials & update him
        Password password = new Password(userToRegister.getPassword());
        this.identityManager.updateCredential(user, password);

        if (userToRegister.getRoles() != null && ! userToRegister.getRoles().isEmpty()) {
            for (String sRole : userToRegister.getRoles().split("\\s*,\\s*")) {
                Role role = BasicModel.getRole(identityManager, sRole);
                if (role == null) {
                    role = new Role(sRole);
                    identityManager.add(role);
                }
                BasicModel.grantRole(relationshipManager, user, role);
            }
        }

        return userToRegister;
    }

    private User getUser(SimpleUser simpleUser) {
        User user = new User();
        user.setLoginName(simpleUser.getLoginName());
        user.setFirstName(simpleUser.getFirstName());
        user.setLastName(simpleUser.getLastName());
        user.setEmail(simpleUser.getEmail());
        return user;
    }

}
