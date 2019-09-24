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

package org.gsc.core.operator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;

public interface Operator {

    boolean execute(TransactionResultWrapper result) throws ContractExeException;

    boolean validate() throws ContractValidateException;

    ByteString getOwnerAddress() throws InvalidProtocolBufferException;

    long calcFee();

}
