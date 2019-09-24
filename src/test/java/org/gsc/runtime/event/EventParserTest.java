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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gsc.runtime.GVMTestUtils;
import org.junit.Test;
import org.testng.Assert;
import org.gsc.crypto.Hash;
import org.gsc.runtime.vm.LogInfoTriggerParser;
import org.gsc.utils.ByteArray;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.SmartContract.ABI;

public class EventParserTest {

  @Test
  public synchronized void testEventParser() {

    Wallet.setAddressPreFixByte(Constant.ADD_PRE_FIX_BYTE);

    String eventSign = "eventBytesL(address,bytes,bytes32,uint256,string)";

    String abiStr = "[{\"constant\":false,\"inputs\":[{\"name\":\"_address\",\"type\":\"address\"},{\"name\":\"_random\",\"type\":\"bytes\"}],\"name\":\"randomNum\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},{\"anonymous\":true,\"inputs\":[{\"indexed\":true,\"name\":\"addr\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"random\",\"type\":\"bytes\"},{\"indexed\":false,\"name\":\"last1\",\"type\":\"bytes32\"},{\"indexed\":false,\"name\":\"t2\",\"type\":\"uint256\"}],\"name\":\"eventAnonymous\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"addr\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"random\",\"type\":\"bytes\"},{\"indexed\":true,\"name\":\"last1\",\"type\":\"bytes32\"},{\"indexed\":false,\"name\":\"t2\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"str\",\"type\":\"string\"}],\"name\":\"eventBytesL\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"addr\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"random\",\"type\":\"bytes\"},{\"indexed\":false,\"name\":\"last1\",\"type\":\"bytes32\"},{\"indexed\":false,\"name\":\"t2\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"str\",\"type\":\"string\"}],\"name\":\"eventBytes\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"addr\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"random\",\"type\":\"bytes\"},{\"indexed\":false,\"name\":\"\",\"type\":\"bytes32\"},{\"indexed\":false,\"name\":\"last1\",\"type\":\"bytes32[]\"},{\"indexed\":false,\"name\":\"t2\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"str\",\"type\":\"string\"}],\"name\":\"eventByteArr\",\"type\":\"event\"}]";

    String dataStr = "0x000000000000000000000000ca35b7d915458ef540ade6068dfe2f44e8fa733c0000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000020109000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a6162636465666731323300000000000000000000000000000000000000000000";
    ABI abi = GVMTestUtils.jsonStr2Abi(abiStr);

    byte[] data = ByteArray.fromHexString(dataStr);
    List<byte[]> topicList = new LinkedList<>();
    topicList.add(Hash.sha3(eventSign.getBytes()));
    topicList.add(ByteArray
        .fromHexString("0xb7685f178b1c93df3422f7bfcb61ae2c6f66d0947bb9eb293259c231b986b81b"));

    ABI.Entry entry = null;
    for (ABI.Entry e : abi.getEntrysList()) {
      System.out.println(e.getName());
      if (e.getName().equalsIgnoreCase("eventBytesL")) {
        entry = e;
        break;
      }
    }

    Assert.assertEquals(LogInfoTriggerParser.getEntrySignature(entry), eventSign);
    Assert.assertEquals(Hash.sha3(LogInfoTriggerParser.getEntrySignature(entry).getBytes()),
        topicList.get(0));
    Assert.assertNotNull(entry);
    Map<String, String> dataMap = ContractEventParserAbi.parseEventData(data, topicList, entry);
    Map<String, String> topicMap = ContractEventParserAbi.parseTopics(topicList, entry);

    Assert.assertEquals(dataMap.get("0"), "GSChnYfqiK4FwKgjaweaJggWz1P9yrb1xNix");
    Assert.assertEquals(dataMap.get("addr"), "GSChnYfqiK4FwKgjaweaJggWz1P9yrb1xNix");

    Assert.assertEquals(dataMap.get("1"), "0109");
    Assert.assertEquals(dataMap.get("random"), "0109");

    Assert.assertEquals(topicMap.get("2"),
        "b7685f178b1c93df3422f7bfcb61ae2c6f66d0947bb9eb293259c231b986b81b");
    Assert.assertEquals(topicMap.get("last1"),
        "b7685f178b1c93df3422f7bfcb61ae2c6f66d0947bb9eb293259c231b986b81b");

    Assert.assertEquals(dataMap.get("3"), "1");
    Assert.assertEquals(dataMap.get("t2"), "1");

    Assert.assertEquals(dataMap.get("4"), "abcdefg123");
    Assert.assertEquals(dataMap.get("str"), "abcdefg123");

  }
}
