package org.gsc.net;

import lombok.extern.slf4j.Slf4j;
import org.gsc.net.message.p2p.P2pMessageCodes;
import org.gsc.net.message.p2p.PingMessage;
import org.gsc.net.message.p2p.PongMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Slf4j
public class PingPongMessageTest {
    @Test
    public void testPing() {

        PingMessage pingMessage = new PingMessage();
        System.out.println(pingMessage);

        assertEquals(PongMessage.class, pingMessage.getAnswerMessage());

        //assertEquals(P2pMessageCodes.PING, pingMessage.getData());
    }

    @Test
    public void testPong() {

        PongMessage pongMessage = new PongMessage();
        System.out.println(pongMessage);

        //assertEquals(P2pMessageCodes.PONG, pongMessage.getData());
        assertEquals(null, pongMessage.getAnswerMessage());
    }
}
