package org.gsc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class Args {

  @Getter
  @Setter
  private int rpcPort;

  public void setParam(final String[] args, final String confFileName) {
  }
}
