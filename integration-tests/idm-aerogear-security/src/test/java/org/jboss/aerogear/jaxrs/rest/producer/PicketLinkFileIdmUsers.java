package org.jboss.aerogear.jaxrs.rest.producer;

import org.jboss.aerogear.jaxrs.rest.test.InstallPicketLinkFileBasedSetupTask;
import org.picketlink.annotations.PicketLink;
import org.picketlink.idm.PartitionManager;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * Created by hmlnarik on 1/22/14. Based on org.jboss.aerogear.shortener.users.PicketLinkFileIdmUsers
 * class from aerogear-shortener project.
 */
public class PicketLinkFileIdmUsers {

    @Resource(lookup = InstallPicketLinkFileBasedSetupTask.JNDI_PICKETLINK_FILE_BASED_PARTITION_MANAGER)
    private PartitionManager partitionManager;

    /**
     * Here we produce PartitionManager so it is available for CDI injection
     *
     * @return
     */
    @Produces
    @ApplicationScoped
    @PicketLink
    public PartitionManager producePartitionManager() {
        return this.partitionManager;
    }
}
