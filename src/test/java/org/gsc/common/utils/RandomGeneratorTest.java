package org.gsc.common.utils;

import com.beust.jcommander.internal.Lists;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.WitnessWrapper;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

@Slf4j
@Ignore
public class RandomGeneratorTest {

  @Test
  public void shuffle() {
    final List<WitnessWrapper> witnessCapsuleListBefore = this.getWitnessList();
    logger.info("updateWitnessSchedule,before: " + getWitnessStringList(witnessCapsuleListBefore));
    final List<WitnessWrapper> witnessCapsuleListAfter = new RandomGenerator<WitnessWrapper>()
        .shuffle(witnessCapsuleListBefore, DateTime.now().getMillis());
    logger.info("updateWitnessSchedule,after: " + getWitnessStringList(witnessCapsuleListAfter));
  }

  private List<WitnessWrapper> getWitnessList() {
    final List<WitnessWrapper> witnessCapsuleList = Lists.newArrayList();
    final WitnessWrapper witnessTron = new WitnessWrapper(
        ByteString.copyFrom("00000000001".getBytes()), 0, "");
    final WitnessWrapper witnessOlivier = new WitnessWrapper(
        ByteString.copyFrom("00000000003".getBytes()), 100, "");
    final WitnessWrapper witnessVivider = new WitnessWrapper(
        ByteString.copyFrom("00000000005".getBytes()), 200, "");
    final WitnessWrapper witnessSenaLiu = new WitnessWrapper(
        ByteString.copyFrom("00000000006".getBytes()), 300, "");
    witnessCapsuleList.add(witnessTron);
    witnessCapsuleList.add(witnessOlivier);
    witnessCapsuleList.add(witnessVivider);
    witnessCapsuleList.add(witnessSenaLiu);
    return witnessCapsuleList;
  }

  private List<String> getWitnessStringList(List<WitnessWrapper> witnessStates) {
    return witnessStates.stream()
        .map(witnessCapsule -> ByteArray.toHexString(witnessCapsule.getAddress().toByteArray()))
        .collect(Collectors.toList());
  }
}