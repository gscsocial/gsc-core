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

  private

  public void setParam(final String[] args, final String confFileName) {
  }
}
