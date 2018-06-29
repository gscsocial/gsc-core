package org.gsc.net;


import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.FileUtil;
import org.gsc.config.Args;
import org.gsc.config.DefaultConfig;
import org.gsc.net.discover.Node;
import org.gsc.net.discover.NodeManager;
import org.gsc.net.discover.RefreshTask;
import org.gsc.net.discover.UDPListener;
import org.gsc.net.message.discover.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class UdpTest {
    ApplicationContext context;
    NodeManager nodeManager;
    Args args;
    UDPListener listener;

    @Before
    public void init(){
        new Thread(() -> {
            Args.setParam(
                    new String[]{ "--output-directory", "udp_test", "--storage-db-directory", "database",
                            "--storage-index-directory", "index"},"config.conf"
            );
            Args cfgArgs = Args.getInstance();
            cfgArgs.getSeedNode().setIpList(Lists.newArrayList());
            cfgArgs.setNodeP2pVersion(100);
            cfgArgs.setNodeListenPort(10001);
            context = new AnnotationConfigApplicationContext(DefaultConfig.class);
        }).start();
    }

    @After
    public void after() {
        FileUtil.deleteDir(new File("udp_test"));
    }



    @Test
    public void udpTest() throws Exception {
        ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);
        Args config = context.getBean(Args.class);
        nodeManager = context.getBean(NodeManager.class);

        InetAddress server = InetAddress.getByName("127.0.0.1");

        Node from = Node.instanceOf("127.0.0.1:10002");
        Node peer1 = Node.instanceOf("127.0.0.1:10003");
        Node peer2 = Node.instanceOf("127.0.0.1:10004");

        Assert.assertTrue(nodeManager.getNodeHandler(peer1)!=null);
        Assert.assertTrue(nodeManager.getNodeHandler(peer2)!=null);
        Assert.assertTrue(nodeManager.getTable().getAllNodes().size() == 0);

        PingMessage pingMessage = new PingMessage(from, nodeManager.getPublicHomeNode());

        PongMessage pongMessage = new PongMessage(from);

        FindNodeMessage findNodeMessage = new FindNodeMessage(from, RefreshTask.getNodeId());

        List<Node> peers = Lists.newArrayList(peer1, peer2);
        NeighborsMessage neighborsMessage = new NeighborsMessage(from, peers);

        DatagramSocket socket = new DatagramSocket();

        DatagramPacket pingPacket = new DatagramPacket(pingMessage.getSendData(),
                pingMessage.getSendData().length, server, 10001);

        DatagramPacket pongPacket = new DatagramPacket(pongMessage.getSendData(),
                pongMessage.getSendData().length, server, 10001);

        DatagramPacket findNodePacket = new DatagramPacket(findNodeMessage.getSendData(),
                findNodeMessage.getSendData().length, server, 10001);

        DatagramPacket neighborsPacket = new DatagramPacket(neighborsMessage.getSendData(),
                neighborsMessage.getSendData().length, server, 10001);

        // send ping msg
        socket.send(pingPacket);
        byte[] data = new byte[1024];
        DatagramPacket packet = new DatagramPacket(data, data.length);

        boolean pingFlag = false;
        boolean pongFlag = false;
        boolean findNodeFlag = false;
        boolean neighborsFlag = false;
        while (true) {
            socket.receive(packet);
            byte[] bytes = Arrays.copyOfRange(data, 0, packet.getLength());
            Message msg = Message.parse(bytes);
            Assert.assertTrue(Arrays.equals(msg.getNodeId(), nodeManager.getPublicHomeNode().getId()));
            if (!pingFlag) {
                pingFlag = true;
                Assert.assertTrue(msg instanceof PingMessage);
                Assert.assertTrue(Arrays.equals(((PingMessage) msg).getTo().getId(), from.getId()));
                socket.send(pongPacket);
            } else if (!pongFlag) {
                pongFlag = true;
                Assert.assertTrue(msg instanceof PongMessage);
            } else if (!findNodeFlag) {
                findNodeFlag = true;
                Assert.assertTrue(msg instanceof FindNodeMessage);
                socket.send(neighborsPacket);
                socket.send(findNodePacket);
            } else if (!neighborsFlag) {
                Assert.assertTrue(msg instanceof NeighborsMessage);
                break;
            }
        }

        Assert.assertTrue(nodeManager.getNodeHandler(peer1)!=null);
        Assert.assertTrue(nodeManager.getNodeHandler(peer2)!=null);
        Assert.assertTrue(nodeManager.getTable().getAllNodes().size() == 1);

        socket.close();
    }
}
