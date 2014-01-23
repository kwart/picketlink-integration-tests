package org.jboss.aerogear.jaxrs.rest.producer;

import org.jboss.aerogear.jaxrs.rest.test.InstallPicketLinkLdapBasedSetupTask;
import org.picketlink.annotations.PicketLink;
import org.picketlink.idm.PartitionManager;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by hmlnarik on 1/22/14. Based on org.jboss.aerogear.shortener.users.PicketLinkLdapIdmUsers
 * class from aerogear-shortener project.
 */
public class PicketLinkLdapIdmUsers {

    @Resource(lookup = InstallPicketLinkLdapBasedSetupTask.JNDI_PICKETLINK_LDAP_BASED_PARTITION_MANAGER)
    private PartitionManager partitionManager;

    public static final Logger LOG = Logger.getLogger(PicketLinkLdapIdmUsers.class.getName());

    /**
     * Here we produce PartitionManager so it is available for CDI injection
     *
     * @return
     */
    @Produces
    @ApplicationScoped
    @PicketLink
    public PartitionManager producePartitionManager() {
        LOG.log(Level.INFO, "PartitionManager requested, returning {0}", partitionManager);

        return this.partitionManager;
    }
}
