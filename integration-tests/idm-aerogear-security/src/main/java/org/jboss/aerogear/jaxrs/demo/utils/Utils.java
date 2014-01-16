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
package org.jboss.aerogear.jaxrs.demo.utils;

import org.jboss.aerogear.jaxrs.demo.service.UserValidationException;
import org.jboss.aerogear.jaxrs.demo.user.SimpleUser;

/**
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
public class Utils {

    /**
     * 
     * @param testString tests string on its nullity and zero length
     * @return true if string is null or empty, false otherwise
     */
    public static boolean nullOrEmpty(String testString) {
        if (testString == null || testString.length() == 0) {
            return true;
        }
        return false;
    }

    public static void validate(SimpleUser user) throws UserValidationException {
        if (Utils.nullOrEmpty(user.getLoginName())
            || Utils.nullOrEmpty(user.getPassword())
            || Utils.nullOrEmpty(user.getEmail())) {
            throw new UserValidationException();
        }
    }
}
