package org.gsc.net;


import lombok.extern.slf4j.Slf4j;
import org.gsc.config.Args;
import org.gsc.config.DefaultConfig;
import org.gsc.net.discover.Node;
import org.gsc.net.discover.NodeManager;
import org.gsc.net.discover.UDPListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.InetAddress;

@Slf4j
public class UdpTest {
    ApplicationContext context;
    NodeManager nodeManager;
    Args args;
    UDPListener listener;

    @Before
    public void before(){
        context = new AnnotationConfigApplicationContext(DefaultConfig.class);
        args = context.getBean(Args.class);
        listener = context.getBean(UDPListener.class);
        args = new Args();
        args.setParam(
                new String[]{ "--output-directory", "udp_test", "--storage-db-directory", "database",
                        "--storage-index-directory", "index"},"config.conf"
        );
        //cfgArgs.getSeedNode().setIpList(Lists.newArrayList());
        args.setNodeP2pVersion(100);
        args.setNodeListenPort(10001);
    }
    @Test
    public void test(){}


    @Test
    public void udpTest() throws Exception {
        nodeManager = context.getBean(NodeManager.class);
        InetAddress inetAddress = InetAddress.getByName("127.0.0.1");

        Node from = Node.instanceOf("127.0.0.1:10002");
        Node peer1 = Node.instanceOf("127.0.0.1:10003");
        Node peer2 = Node.instanceOf("127.0.0.1:10004");

        Assert.assertTrue(nodeManager.getNodeHandler(peer1) == null);
        Assert.assertTrue(nodeManager.getNodeHandler(peer2) == null);
        Assert.assertTrue(nodeManager.getTable().getNodesCount() == 0);
    }
}
