package org.gsc.net;

import org.gsc.net.message.p2p.DisconnectMessage;
import org.gsc.protos.P2p;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class DisconnectMessageTest {

    private static final Logger logger = LoggerFactory.getLogger("test");

    /* DISCONNECT_MESSAGE */

    @Test /* DisconnectMessage 1 - Requested */
    public void test_1() {

        DisconnectMessage disconnectMessage = new DisconnectMessage(P2p.ReasonCode.REQUESTED);

        logger.trace("{}" + disconnectMessage);
        assertEquals(disconnectMessage.getReason(), 0);
    }

}