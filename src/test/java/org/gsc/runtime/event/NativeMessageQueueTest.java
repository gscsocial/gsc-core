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

package org.gsc.runtime.event;

import org.junit.Assert;
import org.junit.Test;
import org.gsc.runtime.event.nativequeue.NativeMessageQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class NativeMessageQueueTest {
  public int bindPort = 5555;
  public String dataToSend = "################";
  public String topic = "testTopic";

  @Test
  public void invalidBindPort(){
    boolean bRet = NativeMessageQueue.getInstance().start(-1111, 0);
    Assert.assertEquals(true, bRet);
    NativeMessageQueue.getInstance().stop();
  }

  @Test
  public void invalidSendLength(){
    boolean bRet = NativeMessageQueue.getInstance().start(0, -2222);
    Assert.assertEquals(true, bRet);
    NativeMessageQueue.getInstance().stop();
  }

  @Test
  public void publishTrigger(){

    int sendLength = 0;
    boolean bRet = NativeMessageQueue.getInstance().start(bindPort, sendLength);
    Assert.assertEquals(true, bRet);

    startSubscribeThread();

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    NativeMessageQueue.getInstance().publishTrigger(dataToSend, topic);

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    NativeMessageQueue.getInstance().stop();
  }

  public void startSubscribeThread(){
    Thread thread =
            new Thread(() -> {
              ZContext context = new ZContext();
              ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);

              Assert.assertEquals(true, subscriber.connect(String.format("tcp://localhost:%d", bindPort)));
              Assert.assertEquals(true, subscriber.subscribe(topic));

              while (!Thread.currentThread().isInterrupted()) {
                byte[] message = subscriber.recv();
                String triggerMsg = new String(message);

                Assert.assertEquals(true, triggerMsg.contains(dataToSend) || triggerMsg.contains(topic));

              }
            });
    thread.start();
  }
}
