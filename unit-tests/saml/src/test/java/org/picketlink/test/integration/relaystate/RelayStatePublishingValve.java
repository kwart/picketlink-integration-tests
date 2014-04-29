/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.picketlink.test.integration.relaystate;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/**
 *
 * @author Hynek Mlnarik <hmlnarik@redhat.com>
 */
public class RelayStatePublishingValve extends ValveBase {

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        Session sess = request.getSessionInternal();

        Object relayState = sess.getNote("RelayState");
        request.getSession().setAttribute("X-RelayState", relayState);

        System.out.println("RelayState: " + relayState);

        getNext().invoke(request, response);
    }

}
