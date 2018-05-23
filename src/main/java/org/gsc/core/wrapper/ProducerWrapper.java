package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.crypto.ECKey;
import org.gsc.protos.Protocol.Producer;

@Slf4j
public class ProducerWrapper implements StoreWrapper<Producer>, Comparable<ProducerWrapper> {

  private Producer producer;

  @Override
  public int compareTo(ProducerWrapper otherObject) {
    return Long.compare(otherObject.getVoteCount(), this.getVoteCount());
  }

  /**
   * ProducerCapsule constructor with pubKey and url.
   */
  public ProducerWrapper(final ByteString pubKey, final String url) {
    final Producer.Builder producerBuilder = Producer.newBuilder();
    this.producer = producerBuilder
        .setPubKey(pubKey)
        .setAddress(ByteString.copyFrom(ECKey.computeAddress(pubKey.toByteArray())))
        .setUrl(url).build();
  }

  public ProducerWrapper(final Producer producer) {
    this.producer = producer;
  }

  /**
   * ProducerCapsule constructor with address.
   */
  public ProducerWrapper(final ByteString address) {
    this.producer = Producer.newBuilder().setAddress(address).build();
  }

  /**
   * ProducerCapsule constructor with address and voteCount.
   */
  public ProducerWrapper(final ByteString address, final long voteCount, final String url) {
    final Producer.Builder producerBuilder = Producer.newBuilder();
    this.producer = producerBuilder
        .setAddress(address)
        .setVoteCount(voteCount).setUrl(url).build();
  }

  public ProducerWrapper(final byte[] data) {
    try {
      this.producer = Producer.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public ByteString getAddress() {
    return this.producer.getAddress();
  }

  public byte[] createDbKey() {
    return getAddress().toByteArray();
  }

  public String createReadableString() {
    return ByteArray.toHexString(getAddress().toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.producer.toByteArray();
  }

  @Override
  public Producer getInstance() {
    return this.producer;
  }

  public void setPubKey(final ByteString pubKey) {
    this.producer = this.producer.toBuilder().setPubKey(pubKey).build();
  }

  public long getVoteCount() {
    return this.producer.getVoteCount();
  }

  public void setVoteCount(final long voteCount) {
    this.producer = this.producer.toBuilder().setVoteCount(voteCount).build();
  }

  public void setTotalProduced(final long totalProduced) {
    this.producer = this.producer.toBuilder().setTotalProduced(totalProduced).build();
  }

  public long getTotalProduced() {
    return this.producer.getTotalProduced();
  }

  public void setTotalMissed(final long totalMissed) {
    this.producer = this.producer.toBuilder().setTotalMissed(totalMissed).build();
  }

  public long getTotalMissed() {
    return this.producer.getTotalMissed();
  }

  public void setLatestBlockNum(final long latestBlockNum) {
    this.producer = this.producer.toBuilder().setLatestBlockNum(latestBlockNum).build();
  }

  public long getLatestBlockNum() {
    return this.producer.getLatestBlockNum();
  }

  public void setLatestSlotNum(final long latestSlotNum) {
    this.producer = this.producer.toBuilder().setLatestSlotNum(latestSlotNum).build();
  }

  public long getLatestSlotNum() {
    return this.producer.getLatestSlotNum();
  }

  public void setIsJobs(final boolean isJobs) {
    this.producer = this.producer.toBuilder().setIsJobs(isJobs).build();
  }

  public boolean getIsJobs() {
    return this.producer.getIsJobs();
  }

  public void setUrl(final String url) {
    this.producer = this.producer.toBuilder().setUrl(url).build();
  }

  public String getUrl() {
    return this.producer.getUrl();
  }
}
