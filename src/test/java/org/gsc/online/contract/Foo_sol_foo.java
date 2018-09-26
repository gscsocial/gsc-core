package org.gsc.online.contract;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import rx.Observable;
import rx.functions.Func1;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.2.0.
 */
public class Foo_sol_foo extends Contract {
    private static final String BINARY = "600a60005560c0604052600360808190527f666f6f000000000000000000000000000000000000000000000000000000000060a09081526100439160019190610056565b5034801561005057600080fd5b506100f1565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061009757805160ff19168380011785556100c4565b828001600101855582156100c4579182015b828111156100c45782518255916020019190600101906100a9565b506100d09291506100d4565b5090565b6100ee91905b808211156100d057600081556001016100da565b90565b61042d806101006000396000f3006080604052600436106100615763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166306fdde03811461006657806317d7de7c146100f0578063af640d0f14610105578063f2c9ecd81461012c575b600080fd5b34801561007257600080fd5b5061007b610141565b6040805160208082528351818301528351919283929083019185019080838360005b838110156100b557818101518382015260200161009d565b50505050905090810190601f1680156100e25780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156100fc57600080fd5b5061007b6101ce565b34801561011157600080fd5b5061011a61035e565b60408051918252519081900360200190f35b34801561013857600080fd5b5061011a610364565b60018054604080516020600284861615610100026000190190941693909304601f810184900484028201840190925281815292918301828280156101c65780601f1061019b576101008083540402835291602001916101c6565b820191906000526020600020905b8154815290600101906020018083116101a957829003601f168201915b505050505081565b6040805180820190915260078082527f6b61796668616e0000000000000000000000000000000000000000000000000060209092019182526060916102169160019190610369565b506000546040805182815260208101828152600180546002600019610100838516150201909116049383018490527fefe3a239c459d04fd51ed25dc6f8aea69fc88366cdeddc2fb3b0715fe85928e29493909291906060830190849080156102bf5780601f10610294576101008083540402835291602001916102bf565b820191906000526020600020905b8154815290600101906020018083116102a257829003601f168201915b5050935050505060405180910390a160018054604080516020600284861615610100026000190190941693909304601f810184900484028201840190925281815292918301828280156103535780601f1061032857610100808354040283529160200191610353565b820191906000526020600020905b81548152906001019060200180831161033657829003601f168201915b505050505090505b90565b60005481565b606490565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106103aa57805160ff19168380011785556103d7565b828001600101855582156103d7579182015b828111156103d75782518255916020019190600101906103bc565b506103e39291506103e7565b5090565b61035b91905b808211156103e357600081556001016103ed5600a165627a7a72305820e9d79c48b65e4ebcea2f6778605b59e26fcdced658aaf72be2e86d6d621454b80029";

    protected Foo_sol_foo(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Foo_sol_foo(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public List<FooEventEventResponse> getFooEventEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("fooEvent",
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}));
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<FooEventEventResponse> responses = new ArrayList<FooEventEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            FooEventEventResponse typedResponse = new FooEventEventResponse();
            typedResponse.id = (Uint256) eventValues.getNonIndexedValues().get(0);
            typedResponse.name = (Utf8String) eventValues.getNonIndexedValues().get(1);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<FooEventEventResponse> fooEventEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("fooEvent",
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}));
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, FooEventEventResponse>() {
            @Override
            public FooEventEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                FooEventEventResponse typedResponse = new FooEventEventResponse();
                typedResponse.id = (Uint256) eventValues.getNonIndexedValues().get(0);
                typedResponse.name = (Utf8String) eventValues.getNonIndexedValues().get(1);
                return typedResponse;
            }
        });
    }

    public RemoteCall<Utf8String> name() {
        Function function = new Function("name",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteCall<TransactionReceipt> getName() {
        Function function = new Function(
                "getName",
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<Uint256> id() {
        Function function = new Function("id",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteCall<TransactionReceipt> getNumber() {
        Function function = new Function(
                "getNumber",
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static RemoteCall<Foo_sol_foo> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Foo_sol_foo.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<Foo_sol_foo> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Foo_sol_foo.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static Foo_sol_foo load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Foo_sol_foo(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static Foo_sol_foo load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Foo_sol_foo(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static class FooEventEventResponse {
        public Uint256 id;

        public Utf8String name;
    }
}
