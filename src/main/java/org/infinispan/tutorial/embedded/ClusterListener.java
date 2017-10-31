package org.infinispan.tutorial.embedded;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * <code>ClusterListener</code>
 *
 * @author masonhsieh
 * @version 1.0
 */
@Listener
public class ClusterListener {
    CountDownLatch clusterFormedLatch = new CountDownLatch(1);

    CountDownLatch shutdownLatch = new CountDownLatch(1);

    private final int expectedNodes;

    public ClusterListener(int expectedNodes) {
        this.expectedNodes = expectedNodes;
    }

    @ViewChanged
    public void viewChanged(ViewChangedEvent event) {
        List<Address> oldMemebers = event.getOldMembers();
        List<Address> newMemebers = event.getNewMembers();

        System.out.println(String.format("View Changed:\nOld members: %s\nNew members: %s",
                                         Arrays.toString(oldMemebers.toArray()),
                                         Arrays.toString(newMemebers.toArray())));

        int cachedMemebersCount = event.getCacheManager().getMembers().size();

        // waits for the expected number of nodes: form the initial cluster
        if (cachedMemebersCount == expectedNodes) {
            System.out.println("cachedMemebersCount == expectedNodes");

            clusterFormedLatch.countDown();
        }

        // waits for the original coordinator node has left the cluster: shutdown
        else if (event.getNewMembers().size() < event.getOldMembers().size()) {
            System.out.println("event.getNewMembers().size() < event.getOldMembers().size()");

            shutdownLatch.countDown();
        }
        
    }


}
