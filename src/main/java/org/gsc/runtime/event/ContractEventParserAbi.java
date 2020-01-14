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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.util.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.gsc.protos.Protocol.SmartContract.ABI;

@Slf4j(topic = "Parser")
public class ContractEventParserAbi extends ContractEventParser {

    /**
     * parse Event Topic into map NOTICE: In solidity, Indexed Dynamic types's topic is just
     * EVENT_INDEXED_ARGS
     */
    public static Map<String, String> parseTopics(List<byte[]> topicList, ABI.Entry entry) {
        Map<String, String> map = new HashMap<>();
        if (topicList == null || topicList.isEmpty()) {
            return map;
        }

        // the first is the signature.
        int index = 1;
        List<ABI.Entry.Param> list = entry.getInputsList();

        // in case indexed topics doesn't match
        if (topicsMatched(topicList, entry)) {
            for (int i = 0; i < list.size(); ++i) {
                ABI.Entry.Param param = list.get(i);
                if (param.getIndexed()) {
                    if (index >= topicList.size()) {
                        break;
                    }
                    String str = parseTopic(topicList.get(index++), param.getType());
                    if (StringUtils.isNotNullOrEmpty(param.getName())) {
                        map.put(param.getName(), str);
                    }
                    map.put("" + i, str);
                }
            }
        } else {
            for (int i = 1; i < topicList.size(); ++i) {
                map.put("" + (i - 1), Hex.toHexString(topicList.get(i)));
            }
        }
        return map;
    }

    /**
     * parse Event Data into map<String, Object> If parser failed, then return {"0",
     * Hex.toHexString(data)} Only support basic solidity type, String, Bytes. Fixed Array or dynamic
     * Array are not support yet (then return {"0": Hex.toHexString(data)}).
     */
    public static Map<String, String> parseEventData(byte[] data,
                                                     List<byte[]> topicList, ABI.Entry entry) {
        Map<String, String> map = new HashMap<>();
        if (ArrayUtils.isEmpty(data)) {
            return map;
        }
        // in case indexed topics doesn't match
        if (!topicsMatched(topicList, entry)) {
            map.put("" + (topicList.size() - 1), Hex.toHexString(data));
            return map;
        }

        // the first is the signature.
        List<ABI.Entry.Param> list = entry.getInputsList();
        Integer startIndex = 0;
        try {
            // this one starts from the first position.
            int index = 0;
            for (Integer i = 0; i < list.size(); ++i) {
                ABI.Entry.Param param = list.get(i);
                if (param.getIndexed()) {
                    continue;
                }
                if (startIndex == 0) {
                    startIndex = i;
                }

                String str = parseDataBytes(data, param.getType(), index++);
                if (StringUtils.isNotNullOrEmpty(param.getName())) {
                    map.put(param.getName(), str);
                }
                map.put("" + i, str);

            }
            if (list.size() == 0) {
                map.put("0", Hex.toHexString(data));
            }
        } catch (UnsupportedOperationException e) {
            logger.debug("Unsupported Operation Exception", e);
            map.clear();
            map.put(startIndex.toString(), Hex.toHexString(data));
        }
        return map;
    }

    private static boolean topicsMatched(List<byte[]> topicList, ABI.Entry entry) {
        if (topicList == null || topicList.isEmpty()) {
            return true;
        }
        int inputSize = 1;
        for (ABI.Entry.Param param : entry.getInputsList()) {
            if (param.getIndexed()) {
                inputSize++;
            }
        }
        return inputSize == topicList.size();
    }
}
