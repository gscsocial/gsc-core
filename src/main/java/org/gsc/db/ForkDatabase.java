package org.gsc.db;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.util.Pair;
import lombok.Getter;
import org.gsc.common.exception.UnLinkedBlockException;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.chain.BlockId;
import org.gsc.core.wrapper.BlockWrapper;
import org.springframework.beans.factory.annotation.Autowired;

public class ForkDatabase {
  private class ForkBlock {

    public Sha256Hash getParentHash() {
      return this.blk.getParentHash();
    }

    public ForkBlock(BlockWrapper blk) {
      this.blk = blk;
      this.id = blk.getBlockId();
      this.num = blk.getNum();
    }

    BlockWrapper blk;
    Reference<ForkBlock> parent = new WeakReference<>(null);
    BlockId id;
    Boolean invalid;
    long num;

    public ForkBlock getParent() {
      return parent == null ? null : parent.get();
    }

    public void setParent(ForkBlock parent) {
      this.parent = new WeakReference<>(parent);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ForkBlock that = (ForkBlock) o;
      return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {

      return Objects.hash(id);
    }
  }

  public class ForkStore {

    private HashMap<BlockId, ForkBlock> hashKblkMap = new HashMap<>();
    // private HashMap<Sha256Hash, ForkBlock> parentHashKblkMap = new HashMap<>();
    private int maxCapcity = 1024;

    private LinkedHashMap<Long, ArrayList<ForkBlock>> numKblkMap =
        new LinkedHashMap<Long, ArrayList<ForkBlock>>() {

          @Override
          protected boolean removeEldestEntry(Map.Entry<Long, ArrayList<ForkBlock>> entry) {
            if (entry.getKey() < Long.max(0L, head.num - maxCapcity)) {
              entry.getValue().forEach(b -> hashKblkMap.remove(b.id));
              return true;
            }
            return false;
          }
        };

    public void setMaxCapcity(int maxCapcity) {
      this.maxCapcity = maxCapcity;
    }

    public void insert(ForkBlock block) {
      hashKblkMap.put(block.id, block);
      numKblkMap.computeIfAbsent(block.num, listBlk -> new ArrayList<>()).add(block);
    }

    public boolean remove(Sha256Hash hash) {
      ForkBlock block = this.hashKblkMap.get(hash);
      // Sha256Hash parentHash = Sha256Hash.ZERO_HASH;
      if (block != null) {
        long num = block.num;
        // parentHash = block.getParentHash();
        ArrayList<ForkBlock> listBlk = numKblkMap.get(num);
        if (listBlk != null) {
          listBlk.removeIf(b -> b.id.equals(hash));
        }
        this.hashKblkMap.remove(hash);
        return true;
      }
      return false;
    }

    public List<ForkBlock> getBlockByNum(Long num) {
      return numKblkMap.get(num);
    }

    public ForkBlock getByHash(Sha256Hash hash) {
      return hashKblkMap.get(hash);
    }

    public int size() {
      return hashKblkMap.size();
    }

  }

  private ForkBlock head;

  @Getter
  private ForkStore miniStore = new ForkStore();

  @Getter
  private ForkStore miniUnlinkedStore = new ForkStore();

  @Autowired
  protected ForkDatabase() {
  }

  void start(BlockWrapper blk) {
    this.head = new ForkBlock(blk);
    miniStore.insert(this.head);
  }

  void setHead(ForkBlock blk) {
    this.head = blk;
  }

  void removeBlk(Sha256Hash hash) {
    if (!miniStore.remove(hash)) {
      miniUnlinkedStore.remove(hash);
    }
  }

  /**
   * check if the id is contained in the KhoasDB.
   */
  public Boolean containBlock(Sha256Hash hash) {
    return miniStore.getByHash(hash) != null || miniUnlinkedStore.getByHash(hash) != null;
  }

  /**
   * Get the Block form KhoasDB, if it doesn't exist ,return null.
   */
  public BlockWrapper getBlock(Sha256Hash hash) {
    return Stream.of(miniStore.getByHash(hash), miniUnlinkedStore.getByHash(hash))
        .filter(Objects::nonNull)
        .map(block -> block.blk)
        .findFirst()
        .orElse(null);
  }

  /**
   * Push the block in the KhoasDB.
   */
  public BlockWrapper push(BlockWrapper blk) throws UnLinkedBlockException {
    ForkBlock block = new ForkBlock(blk);
    if (head != null && block.getParentHash() != Sha256Hash.ZERO_HASH) {
      ForkBlock kblock = miniStore.getByHash(block.getParentHash());
      if (kblock != null) {
        block.setParent(kblock);
      } else {
        miniUnlinkedStore.insert(block);
        throw new UnLinkedBlockException();
      }
    }

    miniStore.insert(block);

    if (head == null || block.num > head.num) {
      head = block;
    }
    return head.blk;
  }

  public BlockWrapper getHead() {
    return head.blk;
  }

  /**
   * pop the head block then remove it.
   */
  public boolean pop() {
    ForkBlock prev = head.getParent();
    if (prev != null) {
      head = prev;
      return true;
    }
    return false;
  }

  public void setMaxSize(int maxSize) {
    miniUnlinkedStore.setMaxCapcity(maxSize);
    miniStore.setMaxCapcity(maxSize);
  }

  /**
   * Find two block's most recent common parent block.
   */
  public Pair<LinkedList<BlockWrapper>, LinkedList<BlockWrapper>> getBranch(
      Sha256Hash block1, Sha256Hash block2) {
    LinkedList<BlockWrapper> list1 = new LinkedList<>();
    LinkedList<BlockWrapper> list2 = new LinkedList<>();
    ForkBlock kblk1 = miniStore.getByHash(block1);
    ForkBlock kblk2 = miniStore.getByHash(block2);

    if (kblk1 != null && kblk2 != null) {
      while (!Objects.equals(kblk1, kblk2)) {
        if (kblk1.num > kblk2.num) {
          list1.add(kblk1.blk);
          kblk1 = kblk1.getParent();
        } else if (kblk1.num < kblk2.num) {
          list2.add(kblk2.blk);
          kblk2 = kblk2.getParent();
        } else {
          list1.add(kblk1.blk);
          list2.add(kblk2.blk);
          kblk1 = kblk1.getParent();
          kblk2 = kblk2.getParent();
        }
      }
    }

    return new Pair<>(list1, list2);
  }

  /**
   * Find two block's most recent common parent block.
   */
  @Deprecated
  public Pair<LinkedList<BlockWrapper>, LinkedList<BlockWrapper>> getBranch(
      BlockId block1, BlockId block2) {
    LinkedList<BlockWrapper> list1 = new LinkedList<>();
    LinkedList<BlockWrapper> list2 = new LinkedList<>();
    ForkBlock kblk1 = miniStore.getByHash(block1);
    ForkBlock kblk2 = miniStore.getByHash(block2);

    if (kblk1 != null && kblk2 != null) {
      while (!Objects.equals(kblk1, kblk2)) {
        if (kblk1.num > kblk2.num) {
          list1.add(kblk1.blk);
          kblk1 = kblk1.getParent();
        } else if (kblk1.num < kblk2.num) {
          list2.add(kblk2.blk);
          kblk2 = kblk2.getParent();
        } else {
          list1.add(kblk1.blk);
          list2.add(kblk2.blk);
          kblk1 = kblk1.getParent();
          kblk2 = kblk2.getParent();
        }
      }
    }

    return new Pair<>(list1, list2);
  }


  // only for unittest
  public BlockWrapper getParentBlock(Sha256Hash hash) {
    return Stream.of(miniStore.getByHash(hash), miniUnlinkedStore.getByHash(hash))
        .filter(Objects::nonNull)
        .map(ForkBlock::getParent)
        .map(khaosBlock -> khaosBlock == null ? null : khaosBlock.blk)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  public boolean hasData() {
    return !this.miniStore.hashKblkMap.isEmpty();
  }
}
