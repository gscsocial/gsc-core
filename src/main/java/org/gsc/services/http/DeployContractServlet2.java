package org.gsc.services.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.common.storage.DepositImpl;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.TransactionTraceException;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.Manager;
import org.gsc.db.TransactionTrace;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.runtime.Runtime;
import org.gsc.runtime.config.SystemProperties;
import org.gsc.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.SmartContract.ABI;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class DeployContractServlet2 extends HttpServlet {

    /**
     * String contractName,
     * byte[] callerAddress,
     * String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
     * String libraryAddressPair, DepositImpl deposit, BlockWrapper block
     */
    @Autowired
    private Wallet wallet;
    @Autowired
    private Manager dbManager;

    private DepositImpl deposit;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        // logger.info("1---------------------+++++++++++++--------------------------------------------------");
        try {
            String contract = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            CreateSmartContract.Builder build = CreateSmartContract.newBuilder();
            JSONObject jsonObject = JSONObject.parseObject(contract);

            String abi = jsonObject.getString("abi");//
            StringBuffer abiSB = new StringBuffer("{");
            abiSB.append("\"entrys\":");
            abiSB.append(abi);
            abiSB.append("}");
            ABI.Builder abiBuilder = ABI.newBuilder();
            JsonFormat.merge(abiSB.toString(), abiBuilder);
            // logger.info("2---------------------+++++++++++++--------------------------------------------------");
            long feeLimit = jsonObject.getLongValue("fee_limit");//
            long value = jsonObject.getLongValue("call_value");
            long consumeUserResourcePercent = jsonObject.getLongValue("consume_user_resource_percent");
            String contractName = jsonObject.getString("contract_name");//
            byte[] byteCode = ByteArray.fromHexString(jsonObject.getString("bytecode"));

            // String ownerAddress = jsonObject.getString("owner_address");//
            // logger.info("4---------------------+++++++++++++--------------------------------------------------");
            String ownerAddress = Wallet.getAddressPreFixString() + jsonObject.getString("owner_Address");
            // logger.info("5---------------------+++++++++++++--------------------------------------------------");

            deposit = DepositImpl.createRoot(dbManager);
            deposit.createAccount(Hex.decode(ownerAddress), Protocol.AccountType.Normal);
            deposit.addBalance(Hex.decode(ownerAddress), 100000000);

            byte[] callerAddress = Hex.decode(ownerAddress);
            CreateSmartContract createSmartContract = wallet.createSmartContract(contractName, callerAddress, abiBuilder, byteCode,
                    value, consumeUserResourcePercent);

            // logger.info(contract.toString());
            TransactionWrapper trxCapWithoutFeeLimit = new TransactionWrapper(createSmartContract,
                    ContractType.CreateSmartContract);

            Transaction.Builder transactionBuilder = trxCapWithoutFeeLimit.getInstance().toBuilder();
            Transaction.raw.Builder rawBuilder = trxCapWithoutFeeLimit.getInstance().getRawData()
                    .toBuilder();

            rawBuilder.setFeeLimit(feeLimit);
            transactionBuilder.setRawData(rawBuilder);
            Transaction trx = transactionBuilder.build();
            // logger.info(trx.toString());
            // logger.info("3---------------------+++++++++++++--------------------------------------------------");

            // logger.info("6---------------------+++++++++++++--------------------------------------------------");
            Runtime runtime = wallet.processTransactionAndReturnRuntime(trx, deposit, null);
            // logger.info("runtime.getResult-----------------" + runtime.getResult().toString());
            // Wallet.generateContractAddress(trx);
            // response.getWriter().println(Wallet.generateContractAddress(trx));


            // logger.info("runtime.getResult().getHReturn())" + Hex.toHexString(runtime.getResult().getHReturn()).toString());
            System.out.println(Util.printTransaction(trx));
            System.out.println("-----------------------------------------------------------------------------------------");
            Map trxMap = JSON.parseObject(Util.printTransaction(trx));
            System.out.println("contract_address: " + trxMap.get("contract_address"));
            response.getWriter().println(Util.printTransaction(trx));

        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
            try {
                response.getWriter().println(Util.printErrorMsg(e));
            } catch (IOException ioe) {
                logger.debug("IOException: {}", ioe.getMessage());
            }
        }
    }
}