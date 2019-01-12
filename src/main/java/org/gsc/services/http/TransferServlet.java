package org.gsc.services.http;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * message TransferContract {
 *    bytes owner_address = 1;
 *    bytes to_address = 2;
 *    int64 amount = 3;
 *  }
 *
 * owner_address：合约持有人地址  如： “0x58jk…27x6”。
 * to_address： 目标账户地址。
 * amount：转账金额。
 */

@Component
@Slf4j
public class TransferServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      TransferContract.Builder build = TransferContract.newBuilder();
      JsonFormat.merge(contract, build);
      Transaction tx = wallet.createTransactionCapsule(build.build(), ContractType.TransferContract)
          .getInstance();

      response.getWriter().println(Util.printTransaction(tx));
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
