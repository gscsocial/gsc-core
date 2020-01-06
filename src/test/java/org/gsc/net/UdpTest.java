/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.collections.Lists;
import org.gsc.application.GSCApplicationContext;
import org.gsc.net.discover.message.Message;
import org.gsc.net.discover.message.discover.FindNodeMessage;
import org.gsc.net.discover.message.discover.NeighborsMessage;
import org.gsc.net.discover.message.discover.PingMessage;
import org.gsc.net.discover.message.discover.PongMessage;
import org.gsc.net.node.Node;
import org.gsc.net.node.NodeManager;
import org.gsc.config.args.Args;

@Slf4j
public class UdpTest {

  private NodeManager nodeManager;
  private int port = Args.getInstance().getNodeListenPort();

  public UdpTest(GSCApplicationContext context) {
    nodeManager = context.getBean(NodeManager.class);
  }

  public void discover() throws Exception {

    InetAddress server = InetAddress.getByName("127.0.0.1");

    Node from = Node.instanceOf("127.0.0.1:10002");
    Node peer1 = Node.instanceOf("127.0.0.1:10003");
    Node peer2 = Node.instanceOf("127.0.0.1:10004");

    Assert.assertTrue(!nodeManager.hasNodeHandler(peer1));
    Assert.assertTrue(!nodeManager.hasNodeHandler(peer2));
    Assert.assertTrue(nodeManager.getTable().getAllNodes().isEmpty());

    PingMessage pingMessage = new PingMessage(from, nodeManager.getPublicHomeNode());
    DatagramPacket pingPacket = new DatagramPacket(pingMessage.getSendData(),
        pingMessage.getSendData().length, server, port);

    FindNodeMessage findNodeMessage = new FindNodeMessage(from, Node.getNodeId());
    DatagramPacket findNodePacket = new DatagramPacket(findNodeMessage.getSendData(),
        findNodeMessage.getSendData().length, server, port);

    DatagramSocket socket = new DatagramSocket();

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
      Assert.assertTrue(
          Arrays.equals(msg.getFrom().getId(), nodeManager.getPublicHomeNode().getId()));
      if (!pingFlag) {
        pingFlag = true;
        Assert.assertTrue(msg instanceof PingMessage);
        Assert.assertTrue(Arrays.equals(((PingMessage) msg).getTo().getId(), from.getId()));
        PongMessage pongMessage = new PongMessage(from, msg.getTimestamp());
        DatagramPacket pongPacket = new DatagramPacket(pongMessage.getSendData(),
            pongMessage.getSendData().length, server, port);
        socket.send(pongPacket);
      } else if (!pongFlag) {
        pongFlag = true;
        Assert.assertTrue(msg instanceof PongMessage);
      } else if (!findNodeFlag) {
        findNodeFlag = true;
        Assert.assertTrue(msg instanceof FindNodeMessage);
        List<Node> peers = Lists.newArrayList(peer1, peer2);
        NeighborsMessage neighborsMessage = new NeighborsMessage(from, peers, msg.getTimestamp());
        DatagramPacket neighborsPacket = new DatagramPacket(neighborsMessage.getSendData(),
            neighborsMessage.getSendData().length, server, port);
        socket.send(neighborsPacket);
        socket.send(findNodePacket);
      } else if (!neighborsFlag) {
        Assert.assertTrue(msg instanceof NeighborsMessage);
        break;
      }
    }

    Assert.assertTrue(nodeManager.hasNodeHandler(peer1));

    Assert.assertTrue(nodeManager.hasNodeHandler(peer2));

    Assert.assertTrue(nodeManager.getTable().getAllNodes().size() == 1);

    socket.close();

  }
}

