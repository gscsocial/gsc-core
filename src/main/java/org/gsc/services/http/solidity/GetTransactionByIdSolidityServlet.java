package org.gsc.services.http.solidity;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.api.GrpcAPI.BytesMessage;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.WalletSolidity;
import org.gsc.services.http.JsonFormat;
import org.gsc.services.http.Util;
import org.gsc.protos.Protocol.Transaction;

@Component
@Slf4j
public class GetTransactionByIdSolidityServlet extends HttpServlet {

  @Autowired
  private WalletSolidity walletSolidity;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getParameter("value");
      Transaction reply = walletSolidity
              .getTransactionById(ByteString.copyFrom(ByteArray.fromHexString(input)));
      if (reply != null) {
        response.getWriter().println(Util.printTransaction(reply));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(e.getMessage());
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
              .collect(Collectors.joining(System.lineSeparator()));
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build);
      Transaction reply = walletSolidity.getTransactionById(build.build().getValue());
      if (reply != null) {
        response.getWriter().println(Util.printTransaction(reply));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(e.getMessage());
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
}