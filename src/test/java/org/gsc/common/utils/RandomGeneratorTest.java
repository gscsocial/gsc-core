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
    final List<WitnessWrapper> witnessWrappersList = Lists.newArrayList();

    final  WitnessWrapper witness1 = new WitnessWrapper(ByteString.copyFrom("00000000001".getBytes()), 0, "");
    final  WitnessWrapper witness2 = new WitnessWrapper(ByteString.copyFrom("00000000002".getBytes()), 100, "");
    final  WitnessWrapper witness3 = new WitnessWrapper(ByteString.copyFrom("00000000003".getBytes()), 200, "");
    final  WitnessWrapper witness4 = new WitnessWrapper(ByteString.copyFrom("00000000004".getBytes()), 300, "");

    witnessWrappersList.add(witness1);
    witnessWrappersList.add(witness2);
    witnessWrappersList.add(witness3);
    witnessWrappersList.add(witness4);

    return witnessWrappersList;
  }

  private List<String> getWitnessStringList(List<WitnessWrapper> witnessStates) {
    return witnessStates.stream()
        .map(witnessCapsule -> ByteArray.toHexString(witnessCapsule.getAddress().toByteArray()))
        .collect(Collectors.toList());
  }
}