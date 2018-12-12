package org.gsc.common.overlay.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import lombok.extern.slf4j.Slf4j;
import org.gsc.protos.Protocol.SmartContract;

/**
 * @Auther: kay
 * @Date: 11/30/18 16:04
 * @Description:
 */
@Slf4j
public class RPCUtils {

    public static SmartContract.ABI jsonStr2ABI(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
        JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
        SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
        for (int index = 0; index < jsonRoot.size(); index++) {
            JsonElement abiItem = jsonRoot.get(index);
            boolean anonymous = abiItem.getAsJsonObject().get("anonymous") != null &&
                    abiItem.getAsJsonObject().get("anonymous").getAsBoolean();
            boolean constant = abiItem.getAsJsonObject().get("constant") != null &&
                    abiItem.getAsJsonObject().get("constant").getAsBoolean();
            String name = abiItem.getAsJsonObject().get("name") != null ?
                    abiItem.getAsJsonObject().get("name").getAsString() : null;
            JsonArray inputs = abiItem.getAsJsonObject().get("inputs") != null ?
                    abiItem.getAsJsonObject().get("inputs").getAsJsonArray() : null;
            JsonArray outputs = abiItem.getAsJsonObject().get("outputs") != null ?
                    abiItem.getAsJsonObject().get("outputs").getAsJsonArray() : null;
            String type = abiItem.getAsJsonObject().get("type") != null ?
                    abiItem.getAsJsonObject().get("type").getAsString() : null;
            boolean payable = abiItem.getAsJsonObject().get("payable") != null &&
                    abiItem.getAsJsonObject().get("payable").getAsBoolean();
            String stateMutability = abiItem.getAsJsonObject().get("stateMutability") != null ?
                    abiItem.getAsJsonObject().get("stateMutability").getAsString() : null;
            if (type == null) {
                logger.error("No type!");
                return null;
            }
            if (!type.equalsIgnoreCase("fallback") && null == inputs) {
                logger.error("No inputs!");
                return null;
            }

            SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
            entryBuilder.setAnonymous(anonymous);
            entryBuilder.setConstant(constant);
            if (name != null) {
                entryBuilder.setName(name);
            }

            /* { inputs : optional } since fallback function not requires inputs*/
            if (null != inputs) {
                for (int j = 0; j < inputs.size(); j++) {
                    JsonElement inputItem = inputs.get(j);
                    if (inputItem.getAsJsonObject().get("name") == null ||
                            inputItem.getAsJsonObject().get("type") == null) {
                        logger.error("Input argument invalid due to no name or no type!");
                        return null;
                    }
                    String inputName = inputItem.getAsJsonObject().get("name").getAsString();
                    String inputType = inputItem.getAsJsonObject().get("type").getAsString();
                    SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
                            .newBuilder();
                    paramBuilder.setIndexed(false);
                    paramBuilder.setName(inputName);
                    paramBuilder.setType(inputType);
                    entryBuilder.addInputs(paramBuilder.build());
                }
            }

            /* { outputs : optional } */
            if (outputs != null) {
                for (int k = 0; k < outputs.size(); k++) {
                    JsonElement outputItem = outputs.get(k);
                    if (outputItem.getAsJsonObject().get("name") == null ||
                            outputItem.getAsJsonObject().get("type") == null) {
                        logger.error("Output argument invalid due to no name or no type!");
                        return null;
                    }
                    String outputName = outputItem.getAsJsonObject().get("name").getAsString();
                    String outputType = outputItem.getAsJsonObject().get("type").getAsString();
                    SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
                            .newBuilder();
                    paramBuilder.setIndexed(false);
                    paramBuilder.setName(outputName);
                    paramBuilder.setType(outputType);
                    entryBuilder.addOutputs(paramBuilder.build());
                }
            }

            entryBuilder.setType(getEntryType(type));
            entryBuilder.setPayable(payable);
            if (stateMutability != null) {
                entryBuilder.setStateMutability(getStateMutability(stateMutability));
            }

            abiBuilder.addEntrys(entryBuilder.build());
        }

        return abiBuilder.build();
    }

    private static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
        switch (type) {
            case "constructor":
                return SmartContract.ABI.Entry.EntryType.Constructor;
            case "function":
                return SmartContract.ABI.Entry.EntryType.Function;
            case "event":
                return SmartContract.ABI.Entry.EntryType.Event;
            case "fallback":
                return SmartContract.ABI.Entry.EntryType.Fallback;
            default:
                return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
        }
    }

    private static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
            String stateMutability) {
        switch (stateMutability) {
            case "pure":
                return SmartContract.ABI.Entry.StateMutabilityType.Pure;
            case "view":
                return SmartContract.ABI.Entry.StateMutabilityType.View;
            case "nonpayable":
                return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
            case "payable":
                return SmartContract.ABI.Entry.StateMutabilityType.Payable;
            default:
                return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
        }
    }

}
