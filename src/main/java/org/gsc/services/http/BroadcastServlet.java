package org.gsc.services.http;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.crypto.ECKey;
import org.gsc.protos.Protocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.api.GrpcAPI;
import org.gsc.core.Wallet;

import static org.apache.commons.codec.digest.DigestUtils.sha256;

@Component
@Slf4j
public class BroadcastServlet extends HttpServlet {

    @Autowired
    private Wallet wallet;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String input = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Protocol.Transaction transaction = Util.packTransaction(input);

            // /*

            //transaction = sign(transaction, );
            String privStr = "ad146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d0";
            BigInteger privKey = new BigInteger(privStr, 16);
            ECKey key = ECKey.fromPrivate(privKey);

            Protocol.Transaction.Builder tbs = transaction.toBuilder();
            byte[] hash = sha256(transaction.getRawData().toByteArray());
            List<Protocol.Transaction.Contract> contractList = transaction.getRawData().getContractList();
            for (int i = 0; i < contractList.size(); i++) {
                ECKey.ECDSASignature signature = key.sign(hash);
                ByteString byteString = ByteString.copyFrom(signature.toByteArray());
                tbs.addSignature(byteString);
            }

            System.out.println("=====================================================");
            System.out.println(tbs.toString());

            GrpcAPI.Return retur = wallet.broadcastTransaction(tbs.build());
            //*/

            //GrpcAPI.Return retur = wallet.broadcastTransaction(transaction);
            response.getWriter().println(JsonFormat.printToString(retur));
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