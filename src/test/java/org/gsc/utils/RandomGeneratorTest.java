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

package org.gsc.utils;

import com.beust.jcommander.internal.Lists;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.gsc.core.wrapper.WitnessWrapper;

@Slf4j
@Ignore
public class RandomGeneratorTest {

  @Test
  public void shuffle() {
    final List<WitnessWrapper> witnessWrapperListBefore = this.getWitnessList();
    logger.info("updateWitnessSchedule,before: " + getWitnessStringList(witnessWrapperListBefore));
    final List<WitnessWrapper> witnessWrapperListAfter = new RandomGenerator<WitnessWrapper>()
        .shuffle(witnessWrapperListBefore, DateTime.now().getMillis());
    logger.info("updateWitnessSchedule,after: " + getWitnessStringList(witnessWrapperListAfter));
  }

  private List<WitnessWrapper> getWitnessList() {
    final List<WitnessWrapper> witnessWrapperList = Lists.newArrayList();
    final WitnessWrapper witnessGSC = new WitnessWrapper(
        ByteString.copyFrom("00000000001".getBytes()), 0, "");
    final WitnessWrapper witnessOlivier = new WitnessWrapper(
        ByteString.copyFrom("00000000002".getBytes()), 100, "");
    final WitnessWrapper witnessVivider = new WitnessWrapper(
        ByteString.copyFrom("00000000003".getBytes()), 200, "");
    final WitnessWrapper witnessSenaLiu = new WitnessWrapper(
        ByteString.copyFrom("00000000004".getBytes()), 300, "");
    witnessWrapperList.add(witnessGSC);
    witnessWrapperList.add(witnessOlivier);
    witnessWrapperList.add(witnessVivider);
    witnessWrapperList.add(witnessSenaLiu);
    return witnessWrapperList;
  }

  private List<String> getWitnessStringList(List<WitnessWrapper> witnessStates) {
    return witnessStates.stream()
        .map(witnessWrapper -> ByteArray.toHexString(witnessWrapper.getAddress().toByteArray()))
        .collect(Collectors.toList());
  }
}
