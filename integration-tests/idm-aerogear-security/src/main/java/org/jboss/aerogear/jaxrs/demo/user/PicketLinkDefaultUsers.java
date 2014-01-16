/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.aerogear.jaxrs.demo.user;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Singleton
@Startup
public class PicketLinkDefaultUsers {

    final String DEFAULT_JOHN_LOGIN_NAME = "john";
    final String DEFAULT_JOHN_EMAIL = "john@doe.com";
    final String DEFAULT_JOHN_FIRST_NAME = "John";
    final String DEFAULT_JOHN_LAST_NAME = "Doe";
    final String DEFAULT_JOHN_PASSWORD = "123";

    final String DEFAULT_AGNES_LOGIN_NAME = "agnes";
    final String DEFAULT_AGNES_EMAIL = "agnes@agnes.com";
    final String DEFAULT_AGNES_FIRST_NAME = "Agnes";
    final String DEFAULT_AGNES_LAST_NAME = "Doe";
    final String DEFAULT_AGNES_PASSWORD = "123";

    @Inject
    private PartitionManager partitionManager;

    private IdentityManager identityManager;
    private RelationshipManager relationshipManager;

    /**
     * <p>
     * Loads some users during the first construction.
     * </p>
     */
    @PostConstruct
    public void create() {

        // this need to be here since APE from tests cleans it so that partition does not exist anymore
        Realm defaultRealm = partitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM);
        if (defaultRealm == null) {
            this.partitionManager.add(new Realm(Realm.DEFAULT_REALM));
        }

        this.identityManager = partitionManager.createIdentityManager();
        this.relationshipManager = partitionManager.createRelationshipManager();

        // John

        User john = newUser(DEFAULT_JOHN_LOGIN_NAME, DEFAULT_JOHN_EMAIL, DEFAULT_JOHN_FIRST_NAME, DEFAULT_JOHN_LAST_NAME);
        this.identityManager.updateCredential(john, new Password(DEFAULT_JOHN_PASSWORD));

        // Agnes

        User agnes = newUser(DEFAULT_AGNES_LOGIN_NAME, DEFAULT_AGNES_EMAIL, DEFAULT_AGNES_FIRST_NAME, DEFAULT_AGNES_LAST_NAME);
        this.identityManager.updateCredential(agnes, new Password(DEFAULT_AGNES_PASSWORD));

        Role roleUser = new Role(UserRoles.USER);
        Role roleAdmin = new Role(UserRoles.ADMIN);

        this.identityManager.add(roleUser);
        this.identityManager.add(roleAdmin);

        // Grant roles to them

        grantRoles(john, roleAdmin);
        grantRoles(agnes, roleUser);

    }

    private void grantRoles(User user, Role role) {
        BasicModel.grantRole(relationshipManager, user, role);
    }

    private User newUser(String loginName, String email, String firstName, String lastName) {
        User user = new User(loginName);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        /*
         * Note: Password will be encoded in SHA-512 with SecureRandom-1024 salt See
         * http://lists.jboss.org/pipermail/security-dev/2013-January/000650.html for more information
         */
        this.identityManager.add(user);
        return user;
    }

}
