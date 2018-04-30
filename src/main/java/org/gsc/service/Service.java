package org.gsc.service;

import org.gsc.config.Args;

public interface Service {

  void init();

  void init(Args args);

  void start();

  void stop();
}
