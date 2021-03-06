package org.infinispan.tutorial.embedded;

import org.infinispan.notifications.IncorrectListenerException;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;

/**
 * <code>CacheListener</code> listens to the changes to the data within the cluster.
 *
 * @author masonhsieh
 * @version 1.0
 */
@Listener(clustered = true)
public class CacheListener {

    @CacheEntryCreated
    public void entryCreated(CacheEntryCreatedEvent<String, LocationWeather> event) throws IncorrectListenerException {
        // for those calls originated from remote nodes

        if (!event.isOriginLocal()) {
            System.out.printf("---- Entry for %s modified by another node\n ----", event.getKey());
        }
    }

}
