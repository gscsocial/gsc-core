package org.gsc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class Args {

  @Getter
  @Setter
  private int rpcPort;

  @Getter
  @Setter
  private int nodeP2pVersion;

  @Getter
  @Setter
  private boolean nodeDiscoveryEnable;

  @Getter
  @Setter
  private int nodeListenPort;

  @Getter
  @Setter
  private int nodeConnectionTimeout;

  @Getter
  @Setter
  private int nodeMaxActiveNodes;

  @Getter
  @Setter
  private int minParticipationRate;

  @Getter
  @Setter
  private boolean needSyncCheck;

  private String storageDir;

  public String getStorageDir() {
    return storageDir;
  }

  public void setStorageDir(String storageDir) {
    this.storageDir = storageDir;
  }


  public void setParam(final String[] args, final String confFileName) {
  }
}
