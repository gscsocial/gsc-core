package org.gsc.net.discover.handler;

public interface EventHandler {

    void channelActivated();

    void handleEvent(UdpEvent event);

}
