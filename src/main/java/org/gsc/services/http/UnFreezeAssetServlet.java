package org.gsc.services.http;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;


@Component
@Slf4j
public class UnFreezeAssetServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Contract.UnfreezeAssetContract.Builder build = Contract.UnfreezeAssetContract.newBuilder();
      JsonFormat.merge(contract, build);
      Transaction tx = wallet
          .createTransactionCapsule(build.build(), ContractType.UnfreezeAssetContract)
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
