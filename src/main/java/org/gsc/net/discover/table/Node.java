package org.gsc.net.discover.table;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import org.gsc.common.utils.ByteArray;
import org.spongycastle.util.encoders.Hex;

public class Node implements Serializable {

  private static final long serialVersionUID = -4267600517925770636L;

  private byte[] id;

  private String host;

  private int port;

  private boolean isFakeNodeId = false;

  public int getReputation() {
    return reputation;
  }

  public void setReputation(int reputation) {
    this.reputation = reputation;
  }

  private int reputation = 0;

  public static Node instanceOf(String addressOrEnode) {
    try {
      URI uri = new URI(addressOrEnode);
      if (uri.getScheme().equals("enode")) {
        return new Node(addressOrEnode);
      }
    } catch (URISyntaxException e) {
      // continue
    }

    //TODO: need a crypto module
    final String generatedNodeId = "nodeId";
    final Node node = new Node("enode://" + generatedNodeId + "@" + addressOrEnode);
    node.isFakeNodeId = true;
    return node;
  }

  public String getEnodeURL() {
    return new StringBuilder("enode://")
        .append(ByteArray.toHexString(id)).append("@")
        .append(host).append(":")
        .append(port).toString();
  }

  public Node(String enodeURL) {
    try {
      URI uri = new URI(enodeURL);
      if (!uri.getScheme().equals("enode")) {
        throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT");
      }
      this.id = Hex.decode(uri.getUserInfo());
      this.host = uri.getHost();
      this.port = uri.getPort();
    } catch (URISyntaxException e) {
      throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT", e);
    }
  }

  public Node(byte[] id, String host, int port) {
    this.id = id;
    this.host = host;
    this.port = port;
  }

  public String getHexId() {
    return Hex.toHexString(id);
  }

  public boolean isDiscoveryNode() {
    return isFakeNodeId;
  }

  public byte[] getId() {
    return id;
  }

  public void setId(byte[] id) {
    this.id = id;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public String toString() {
    return "Node{" +
        " host='" + host + '\'' +
        ", port=" + port +
        ", id=" + ByteArray.toHexString(id) +
        '}';
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o == this) {
      return true;
    }

    if (o instanceof Node) {
      return getId().equals(((Node) o).getId());
    }

    return false;
  }
}
