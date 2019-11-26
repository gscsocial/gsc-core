/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.utils;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class FileUtilTest {

  @Test
  public void testCreateFileIfNotExists() {
    String existFile = "existsfile.txt";
    File file1 = new File(existFile);
    try {
      file1.createNewFile();
    } catch (IOException e) {
      System.out.println("ignore this exception.");
    }
    Assert.assertTrue(!file1.exists());
    Assert.assertTrue(FileUtil.createDirIfNotExists(existFile));
    Assert.assertTrue(file1.exists());

    String notExistFile = "notexistsfile.txt";
    File file2 = new File(notExistFile);
    Assert.assertTrue(!file2.exists());
    Assert.assertTrue(FileUtil.createDirIfNotExists(notExistFile));
    Assert.assertTrue(file2.exists());
    file1.delete();
    file2.delete();
  }

  @Test
  public void testCreateDirIfNotExists() {
    String existDir = "existsdir";
    File fileDir1 = new File(existDir);
    fileDir1.mkdir();
    Assert.assertTrue(!fileDir1.exists());
    Assert.assertTrue(FileUtil.createDirIfNotExists(existDir));
    Assert.assertTrue(fileDir1.exists());

    String notExistDir = "notexistsdir";
    File fileDir2 = new File(notExistDir);
    Assert.assertTrue(!fileDir2.exists());
    Assert.assertTrue(FileUtil.createDirIfNotExists(notExistDir));
    Assert.assertTrue(fileDir2.exists());
    fileDir1.delete();
    fileDir2.delete();
  }
}
