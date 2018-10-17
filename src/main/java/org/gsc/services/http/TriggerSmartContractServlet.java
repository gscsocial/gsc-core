package org.gsc.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.core.wrapper.TransactionWrapper;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.GrpcAPI.Return.response_code;
import org.gsc.api.GrpcAPI.TransactionExtention;
import org.gsc.crypto.Hash;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.Wallet;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class TriggerSmartContractServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
  }

  public static String parseMethod(String methodSign, String params) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
    System.out.println(methodSign + ":" + Hex.toHexString(selector));
    if (StringUtils.isEmpty(params)) {
      return Hex.toHexString(selector);
    }
    String result = Hex.toHexString(selector) + params;
    return result;
  }

  /**
   * message TriggerSmartContract {
   *    bytes owner_address = 1;
   *    bytes contract_address = 2;
   *    int64 call_value = 3;
   *    bytes data = 4;
   *  }
   *
   * owner_address：合约持有人地址 
   * contract_address： 合约地址。
   * call_value：值。
   * data：操作参数。
   *
   * {
   * 	"owner_address": "abd4b9367799eaa3197fecb144eb71de1e049abc",
   * 	"contract_address": "410019eb34f17c56bf2aaf71cfe5ab7362232147ba",
   * 	"call_value": 20000000,
   * 	"data": "",
   *
   * 	"function_selector": "getName()",
   * 	"parameter": "",
   * 	"fee_limit": 10000000
   * }
   *
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    TriggerSmartContract.Builder build = TriggerSmartContract.newBuilder();
    TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
    Return.Builder retBuilder = Return.newBuilder();

    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));

      JsonFormat.merge(contract, build);
      JSONObject jsonObject = JSONObject.parseObject(contract);
      System.out.println(jsonObject.toString());
      String selector = jsonObject.getString("function_selector");//
      String parameter = jsonObject.getString("parameter");//
      String data = parseMethod(selector, parameter);
      System.out.println(data);
      build.setData(ByteString.copyFrom(ByteArray.fromHexString(data)));
      System.out.println("4");
      long feeLimit = jsonObject.getLongValue("fee_limit");//
      System.out.println("5");
      TransactionWrapper trxCap = wallet
          .createTransactionCapsule(build.build(), ContractType.TriggerSmartContract);

      Transaction.Builder txBuilder = trxCap.getInstance().toBuilder();
      Transaction.raw.Builder rawBuilder = trxCap.getInstance().getRawData().toBuilder();
      rawBuilder.setFeeLimit(feeLimit);
      txBuilder.setRawData(rawBuilder);

      Transaction trx = wallet
          .triggerContract(build.build(), new TransactionWrapper(txBuilder.build()), trxExtBuilder,
              retBuilder);
      trxExtBuilder.setTransaction(trx);
      trxExtBuilder.setTxid(trxCap.getTransactionId().getByteString());
      retBuilder.setResult(true).setCode(response_code.SUCCESS);
    } catch (ContractValidateException e) {
      retBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
    } catch (Exception e) {
      retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
    }
    trxExtBuilder.setResult(retBuilder);
    response.getWriter().println(Util.printTransactionExtention(trxExtBuilder.build()));
  }
}
