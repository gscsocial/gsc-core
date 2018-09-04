package org.gsc.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.VotesWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.protos.Protocol.Vote;

@Slf4j
public class VotesStoreTest {

  private static final String dbPath = "output-votesStore-test";
  private static GSCApplicationContext context;
  VotesStore votesStore;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  @Before
  public void initDb() {
    this.votesStore = context.getBean(VotesStore.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  @Test
  public void putAndGetVotes() {
    List<Vote> oldVotes = new ArrayList<Vote>();

    VotesWrapper votesCapsule = new VotesWrapper(ByteString.copyFromUtf8("100000000x"), oldVotes);
    this.votesStore.put(votesCapsule.createDbKey(), votesCapsule);

    Assert.assertTrue("votesStore is empyt", votesStore.iterator().hasNext());
    Assert.assertTrue(votesStore.has(votesCapsule.createDbKey()));
    VotesWrapper votesSource = this.votesStore
        .get(ByteString.copyFromUtf8("100000000x").toByteArray());
    Assert.assertEquals(votesCapsule.getAddress(), votesSource.getAddress());
    Assert.assertEquals(ByteString.copyFromUtf8("100000000x"), votesSource.getAddress());

//    votesCapsule = new VotesWrapper(ByteString.copyFromUtf8(""), oldVotes);
//    this.votesStore.put(votesCapsule.createDbKey(), votesCapsule);
//    votesSource = this.votesStore.get(ByteString.copyFromUtf8("").toByteArray());
//    Assert.assertEquals(votesStore.getAllVotes().size(), 2);
//    Assert.assertEquals(votesCapsule.getAddress(), votesSource.getAddress());
//    Assert.assertEquals(null, votesSource.getAddress());
  }
}