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

package org.gsc.core.witness;

public enum BlockProductionCondition {
    PRODUCED,           // Successfully generated block
    UNELECTED,
    NOT_MY_TURN,        // It isn't my turn
    NOT_SYNCED,
    NOT_TIME_YET,       // Not yet arrived
    NO_PRIVATE_KEY,
    WITNESS_PERMISSION_ERROR,
    LOW_PARTICIPATION,
    LAG,
    CONSECUTIVE,
    TIME_OUT,
    BACKUP_STATUS_IS_NOT_MASTER,
    DUP_WITNESS,
    EXCEPTION_PRODUCING_BLOCK
}
