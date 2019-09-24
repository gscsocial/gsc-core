/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.runtime.event;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class EventLoaderTest {
  @Test
  public void launchNativeQueue(){
    EventPluginConfig config = new EventPluginConfig();
    config.setSendQueueLength(1000);
    config.setBindPort(5555);
    config.setUseNativeQueue(true);

    List<TriggerConfig> triggerConfigList = new ArrayList<>();

    TriggerConfig blockTriggerConfig = new TriggerConfig();
    blockTriggerConfig.setTriggerName("block");
    blockTriggerConfig.setEnabled(true);
    blockTriggerConfig.setTopic("block");
    triggerConfigList.add(blockTriggerConfig);

    config.setTriggerConfigList(triggerConfigList);

    Assert.assertEquals(true,EventPluginLoader.getInstance().start(config));

    EventPluginLoader.getInstance().stopPlugin();
  }
}
