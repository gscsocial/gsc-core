package org.gsc.common.app;

import org.gsc.config.Args;
import org.gsc.service.Service;

public interface Application {

  void setOptions(Args args);

  void init(Args args);

  void initServices(Args args);

  void startup();

  void shutdown();

  void startServices();

  void shutdownServices();

  void addService(Service service);
}
