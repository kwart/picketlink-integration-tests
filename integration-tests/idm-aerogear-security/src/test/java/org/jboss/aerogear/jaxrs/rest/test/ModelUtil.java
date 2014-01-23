package org.jboss.aerogear.jaxrs.rest.test;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class ModelUtil {

    private static final Logger log = Logger.getLogger(ModelUtil.class.getName());

    public static List<String> modelNodeAsStringList(ModelNode node) {
        List<String> ret = new LinkedList<String>();
        for (ModelNode n : node.asList())
            ret.add(n.asString());
        return ret;
    }

    public static ModelNode createCompositeNode(ModelNode... steps) {
        ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();

        ModelNode opSteps = compositeOp.get(STEPS);
        for (ModelNode step : steps) {
            opSteps.add(step);
        }

        return compositeOp;
    }

    public static boolean execute(ManagementClient client, ModelNode operation) {
        try {
            ModelNode result = client.getControllerClient().execute(operation);
            return getOperationResult(result);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed execution of operation " + operation.toJSONString(false), e);
            return false;
        }
    }

    private static boolean getOperationResult(ModelNode node) {

        boolean success = "success".equalsIgnoreCase(node.get("outcome").asString());
        if (!success) {
            log.log(Level.WARNING, "Operation failed with \n{0}", node.toJSONString(false));
        }

        return success;

    }

    public static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();

        // set address
        ModelNode list = op.get(OP_ADDR).setEmptyList();

        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=", 2);
                list.add(elements[0], elements[1]);
            }
        }
        op.get(OP).set(operation);
        return op;
    }
}