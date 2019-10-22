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

import com.google.protobuf.ByteString;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.ProposalWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol.Proposal.State;

@Slf4j(topic = "witness")
public class ProposalController {

    @Setter
    @Getter
    private Manager manager;

    public static ProposalController createInstance(Manager manager) {
        ProposalController instance = new ProposalController();
        instance.setManager(manager);
        return instance;
    }

    public void processProposal(ProposalWrapper proposalWrapper) {

        List<ByteString> activeWitnesses = this.manager.getWitnessScheduleStore().getActiveWitnesses();
        if (proposalWrapper.hasMostApprovals(activeWitnesses)) {
            logger.info(
                    "Processing proposal,id:{},it has received most approvals, "
                            + "begin to set dynamic parameter:{}, "
                            + "and set proposal state as APPROVED",
                    proposalWrapper.getID(), proposalWrapper.getParameters());
            setDynamicParameters(proposalWrapper);
            proposalWrapper.setState(State.APPROVED);
            manager.getProposalStore().put(proposalWrapper.createDbKey(), proposalWrapper);
        } else {
            logger.info(
                    "Processing proposal,id:{}, "
                            + "it has not received enough approvals, set proposal state as DISAPPROVED",
                    proposalWrapper.getID());
            proposalWrapper.setState(State.DISAPPROVED);
            manager.getProposalStore().put(proposalWrapper.createDbKey(), proposalWrapper);
        }

    }

    public void processProposals() {
        long latestProposalNum = manager.getDynamicPropertiesStore().getLatestProposalNum();
        if (latestProposalNum == 0) {
            logger.info("latestProposalNum is 0,return");
            return;
        }

        long proposalNum = latestProposalNum;

        ProposalWrapper proposalWrapper = null;

        while (proposalNum > 0) {
            try {
                proposalWrapper = manager.getProposalStore()
                        .get(ProposalWrapper.calculateDbKey(proposalNum));
            } catch (Exception ex) {
                logger.error("", ex);
                continue;
            }

            if (proposalWrapper.hasProcessed()) {
                logger
                        .info("Proposal has processed，id:[{}],skip it and before it",
                                proposalWrapper.getID());
                //proposals with number less than this one, have been processed before
                break;
            }

            if (proposalWrapper.hasCanceled()) {
                logger.info("Proposal has canceled，id:[{}],skip it", proposalWrapper.getID());
                proposalNum--;
                continue;
            }

            long currentTime = manager.getDynamicPropertiesStore().getNextMaintenanceTime();
            if (proposalWrapper.hasExpired(currentTime)) {
                processProposal(proposalWrapper);
                proposalNum--;
                continue;
            }

            proposalNum--;
            logger.info("Proposal has not expired，id:[{}],skip it", proposalWrapper.getID());
        }
        logger.info("Processing proposals done, oldest proposal[{}]", proposalNum);
    }

    public void setDynamicParameters(ProposalWrapper proposalWrapper) {
        Map<Long, Long> map = proposalWrapper.getInstance().getParametersMap();
        for (Map.Entry<Long, Long> entry : map.entrySet()) {

            switch (entry.getKey().intValue()) {
                case (0): {
                    manager.getDynamicPropertiesStore().saveMaintenanceTimeInterval(entry.getValue());
                    break;
                }
                case (1): {
                    manager.getDynamicPropertiesStore().saveAccountUpgradeCost(entry.getValue());
                    break;
                }
                case (2): {
                    manager.getDynamicPropertiesStore().saveCreateAccountFee(entry.getValue());
                    break;
                }
                case (3): {
                    manager.getDynamicPropertiesStore().saveTransactionFee(entry.getValue());
                    break;
                }
                case (4): {
                    manager.getDynamicPropertiesStore().saveAssetIssueFee(entry.getValue());
                    break;
                }
                case (5): {
                    manager.getDynamicPropertiesStore().saveWitnessPayPerBlock(entry.getValue());
                    break;
                }
                case (6): {
                    manager.getDynamicPropertiesStore().saveWitnessStandbyAllowance(entry.getValue());
                    break;
                }
                case (7): {
                    manager.getDynamicPropertiesStore()
                            .saveCreateNewAccountFeeInSystemContract(entry.getValue());
                    break;
                }
                case (8): {
                    manager.getDynamicPropertiesStore().saveCreateNewAccountNetRate(entry.getValue());
                    break;
                }
                case (9): {
                    manager.getDynamicPropertiesStore().saveAllowCreationOfContracts(entry.getValue());
                    break;
                }
                case (10): {
                    if (manager.getDynamicPropertiesStore().getRemoveThePowerOfTheGr() == 0) {
                        manager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(entry.getValue());
                    }
                    break;
                }
                case (11): {
                    manager.getDynamicPropertiesStore().saveCpuFee(entry.getValue());
                    break;
                }
                case (12): {
                    manager.getDynamicPropertiesStore().saveExchangeCreateFee(entry.getValue());
                    break;
                }
                case (13): {
                    manager.getDynamicPropertiesStore().saveMaxCpuTimeOfOneTx(entry.getValue());
                    break;
                }
                case (14): {
                    manager.getDynamicPropertiesStore().saveAllowUpdateAccountName(entry.getValue());
                    break;
                }
                case (15): {
                    manager.getDynamicPropertiesStore().saveAllowSameTokenName(entry.getValue());
                    break;
                }
                case (16): {
                    manager.getDynamicPropertiesStore().saveAllowDelegateResource(entry.getValue());
                    break;
                }
                case (17): {
                    manager.getDynamicPropertiesStore().saveTotalCpuLimit(entry.getValue());
                    break;
                }
                case (18): {
                    manager.getDynamicPropertiesStore().saveAllowGvmTransferGrc10(entry.getValue());
                    break;
                }
                case (19): {
                    manager.getDynamicPropertiesStore().saveTotalCpuLimit2(entry.getValue());
                    break;
                }
                case (20): {
                    if (manager.getDynamicPropertiesStore().getAllowMultiSign() == 0) {
                        manager.getDynamicPropertiesStore().saveAllowMultiSign(entry.getValue());
                    }
                    break;
                }
                case (21): {
                    if (manager.getDynamicPropertiesStore().getAllowAdaptiveCpu() == 0) {
                        manager.getDynamicPropertiesStore().saveAllowAdaptiveCpu(entry.getValue());
                    }
                    break;
                }
                case (22): {
                    manager.getDynamicPropertiesStore().saveUpdateAccountPermissionFee(entry.getValue());
                    break;
                }
                case (23): {
                    manager.getDynamicPropertiesStore().saveMultiSignFee(entry.getValue());
                    break;
                }
                case (24): {
                    manager.getDynamicPropertiesStore().saveAllowProtoFilterNum(entry.getValue());
                    break;
                }
                case (25): {
                    manager.getDynamicPropertiesStore().saveAllowAccountStateRoot(entry.getValue());
                    break;
                }
                case (26): {
                    manager.getDynamicPropertiesStore().saveAllowGvmConstantinople(entry.getValue());
                    manager.getDynamicPropertiesStore().addSystemContractAndSetPermission(48);
                    break;
                }
                default:
                    break;
            }
        }
    }

}
