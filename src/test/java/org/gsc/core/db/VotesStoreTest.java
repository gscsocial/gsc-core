package org.gsc.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gsc.db.VotesStore;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.wrapper.VotesWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.protos.Protocol.Vote;

@Slf4j
public class VotesStoreTest {

  private static final String dbPath = "output-votesStore-test";
  private static AnnotationConfigApplicationContext context;
  VotesStore votesStore;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
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

    VotesWrapper votesWrapper = new VotesWrapper(ByteString.copyFromUtf8("100000000x"), oldVotes);
    this.votesStore.put(votesWrapper.createDbKey(), votesWrapper);

    Assert.assertTrue("votesStore is empyt", votesStore.getIterator().hasNext());
    Assert.assertTrue(votesStore.has(votesWrapper.createDbKey()));
    VotesWrapper votesSource = this.votesStore
        .get(ByteString.copyFromUtf8("100000000x").toByteArray());
    Assert.assertEquals(votesWrapper.getAddress(), votesSource.getAddress());
    Assert.assertEquals(ByteString.copyFromUtf8("100000000x"), votesSource.getAddress());

//    votesWrapper = new VotesWrapper(ByteString.copyFromUtf8(""), oldVotes);
//    this.votesStore.put(votesWrapper.createDbKey(), votesWrapper);
//    votesSource = this.votesStore.get(ByteString.copyFromUtf8("").toByteArray());
//    Assert.assertEquals(votesStore.getAllVotes().size(), 2);
//    Assert.assertEquals(votesWrapper.getAddress(), votesSource.getAddress());
//    Assert.assertEquals(null, votesSource.getAddress());
  }
}