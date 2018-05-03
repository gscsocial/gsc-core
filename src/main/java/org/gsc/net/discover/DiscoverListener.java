
package org.gsc.net.discover;


public interface DiscoverListener {


    void nodeAppeared(NodeHandler handler);

    void nodeDisappeared(NodeHandler handler);

    class Adapter implements DiscoverListener {
        public void nodeAppeared(NodeHandler handler) {}
        public void nodeDisappeared(NodeHandler handler) {}
    }
}
