package org.gsc.net.discover.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gsc.net.discover.Node;
import org.slf4j.LoggerFactory;

/**
 * Created by kest on 5/25/15.
 */
public class NodeTable {

  static final org.slf4j.Logger logger = LoggerFactory.getLogger("NodeTable");


  private final Node node;  // our node
  private transient NodeBucket[] buckets;
  private transient List<NodeEntry> nodes;
  private Map<Node, Node> evictedCandidates = new HashMap<>();
  private Map<Node, Date> expectedPongs = new HashMap<>();

  public NodeTable(Node n) {
    this.node = n;
    initialize();
  }

  public Node getNode() {
    return node;
  }

  public final void initialize() {
    nodes = new ArrayList<>();
    buckets = new NodeBucket[KademliaOptions.BINS];
    for (int i = 0; i < KademliaOptions.BINS; i++) {
      buckets[i] = new NodeBucket(i);
    }
  }

  public synchronized Node addNode(Node n) {
    NodeEntry e = new NodeEntry(node.getId(), n);
    NodeEntry lastSeen = buckets[getBucketId(e)].addNode(e);
    if (lastSeen != null) {
      return lastSeen.getNode();
    }
    if (!nodes.contains(e)) {
      nodes.add(e);
    }
    return null;
  }

  public synchronized void dropNode(Node n) {
    NodeEntry e = new NodeEntry(node.getId(), n);
    buckets[getBucketId(e)].dropNode(e);
    nodes.remove(e);
  }

  public synchronized boolean contains(Node n) {
    NodeEntry e = new NodeEntry(node.getId(), n);
    for (NodeBucket b : buckets) {
      if (b.getNodes().contains(e)) {
        return true;
      }
    }
    return false;
  }

  public synchronized void touchNode(Node n) {
    NodeEntry e = new NodeEntry(node.getId(), n);
    for (NodeBucket b : buckets) {
      if (b.getNodes().contains(e)) {
        b.getNodes().get(b.getNodes().indexOf(e)).touch();
        break;
      }
    }
  }

  public int getBucketsCount() {
    int i = 0;
    for (NodeBucket b : buckets) {
      if (b.getNodesCount() > 0) {
        i++;
      }
    }
    return i;
  }

  public synchronized NodeBucket[] getBuckets() {
    return buckets;
  }

  public int getBucketId(NodeEntry e) {
    int id = e.getDistance() - 1;
    return id < 0 ? 0 : id;
  }

  public synchronized int getNodesCount() {
    return nodes.size();
  }

  public synchronized List<NodeEntry> getAllNodes() {
    List<NodeEntry> nodes = new ArrayList<>();

    for (NodeBucket b : buckets) {
      for (NodeEntry e : b.getNodes()) {
        if (!e.getNode().equals(node)) {
          nodes.add(e);
        }
      }
    }

    return nodes;
  }

  public synchronized List<Node> getClosestNodes(byte[] targetId) {
    List<NodeEntry> closestEntries = getAllNodes();
    List<Node> closestNodes = new ArrayList<>();
    Collections.sort(closestEntries, new DistanceComparator(targetId));
    if (closestEntries.size() > KademliaOptions.BUCKET_SIZE) {
      closestEntries = closestEntries.subList(0, KademliaOptions.BUCKET_SIZE);
    }
    for (NodeEntry e : closestEntries) {
      if (!e.getNode().isDiscoveryNode()) {
        closestNodes.add(e.getNode());
      }
    }
    return closestNodes;
  }
}
