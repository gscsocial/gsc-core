package org.gsc.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class Args {

  private int rpcPort;

  private int nodeP2pVersion;


  private boolean nodeDiscoveryEnable;

  private int nodeListenPort;

  private int nodeConnectionTimeout;


  private int nodeMaxActiveNodes;


  private int minParticipationRate;

  private boolean needSyncCheck;

  private String storageDir;

  private String outputDirectory;

  private long maintenanceTimeInterval;


  public void setParam(final String[] args, final String confFileName) {
  }
}
