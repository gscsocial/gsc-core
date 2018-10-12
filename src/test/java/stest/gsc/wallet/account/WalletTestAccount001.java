package stest.gsc.wallet.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.api.GrpcAPI;
import org.gsc.common.utils.Utils;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.common.utils.ByteArray;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import stest.gsc.wallet.common.client.Configuration;
import stest.gsc.wallet.common.client.Parameter.CommonConstant;
import stest.gsc.wallet.common.client.WalletClient;
import stest.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.core.Wallet;
import stest.gsc.wallet.common.client.utils.TransactionUtils;


@Slf4j
public class WalletTestAccount001 {
  //testng001、testng002、testng003、testng004
  private final String testKey002 =
          "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final String testKey003 =
          "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";

  /*  //testng001、testng002、testng003、testng004
  private static final byte[] fromAddress = Base58
      .decodeFromBase58Check("THph9K2M2nLvkianrMGswRhz5hjSA9fuH7");
  private static final byte[] toAddress = Base58
      .decodeFromBase58Check("TV75jZpdmP2juMe1dRwGrwpV6AMU6mr1EU");*/

  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);


  private static final long now = System.currentTimeMillis();
  private static final String name = "testAssetIssue_" + Long.toString(now);
  private static final long TotalSupply = now;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";

  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] lowBalAddress = ecKey.getAddress();
  String lowBalTest = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("config.conf").getStringList("fullnode.ip.list")
          .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] noBandwitchAddress = ecKey1.getAddress();
  String noBandwitch = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass
  public void beforeClass() {
    logger.info(ByteArray.toHexString(ecKey.getPrivKeyBytes()));
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }
}


