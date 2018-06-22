package org.gsc.net.discover.table;

import lombok.extern.slf4j.Slf4j;
import org.gsc.config.Parameter;
import org.gsc.crypto.jce.SpongyCastleProvider;
import org.gsc.net.discover.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

@Slf4j
public class DistancComparatorTest {

    private static DistanceComparator comparator = null;

    @Before
    public void initDistanceComparator(){
        Security.addProvider(new BouncyCastleProvider());
        SpongyCastleProvider.getInstance();
        Node node = Node.instanceOf("127.0.0.1:20001");
        comparator = new DistanceComparator(node.getId());
    }
    @Test
    public void test(){
        Node node1 = Node.instanceOf("127.0.0.1:10001");
        NodeEntry entry1 = new NodeEntry(node1);

        Node node2 = Node.instanceOf("127.0.0.1:20001");
        NodeEntry entry2 = new NodeEntry(node2);

        Assert.assertEquals(1,comparator.compare(entry1,entry2));

    }
}
