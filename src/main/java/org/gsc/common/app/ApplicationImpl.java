package org.gsc.common.app;

import org.gsc.config.Args;
import org.gsc.db.Manager;
import org.gsc.service.Service;
import org.springframework.stereotype.Component;

@Component
public class ApplicationImpl implements  Application{

  @Override
  public void setOptions(Args args) {

  }

  @Override
  public void init(Args args) {

  }

  @Override
  public void initServices(Args args) {

  }

  @Override
  public void startup() {

  }

  @Override
  public void shutdown() {

  }

  @Override
  public void startServices() {

  }

  @Override
  public void shutdownServices() {

  }

  @Override
  public void addService(Service service) {

  }

  @Override
  public Manager getDbManager() {
    return null;
  }
}
