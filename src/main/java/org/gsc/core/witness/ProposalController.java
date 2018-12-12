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

@Slf4j
public class ProposalController {

  @Setter
  @Getter
  private Manager manager;

  public static ProposalController createInstance(Manager manager) {
    ProposalController instance = new ProposalController();
    instance.setManager(manager);
    return instance;
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
      }

      if (proposalWrapper.hasProcessed()) {
        logger
            .info("Proposal has processed，id:[{}],skip it and before it", proposalWrapper.getID());
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

  public void processProposal(ProposalWrapper proposalWrapper) {

    List<ByteString> activeWitnesses = this.manager.getWitnessScheduleStore().getActiveWitnesses();
    if (proposalWrapper.hasMostApprovals(activeWitnesses)) {
      logger.info(
          "Processing proposal,id:{},it has received most approvals ,begin to set dynamic parameter,{},and set  proposal state as DISAPPROVED",
          proposalWrapper.getID(), proposalWrapper.getParameters());
      setDynamicParameters(proposalWrapper);
      proposalWrapper.setState(State.APPROVED);
      manager.getProposalStore().put(proposalWrapper.createDbKey(), proposalWrapper);
    } else {
      logger.info(
          "Processing proposal,id:{},it has not received enough approvals,set proposal state as DISAPPROVED",
          proposalWrapper.getID());
      proposalWrapper.setState(State.DISAPPROVED);
      manager.getProposalStore().put(proposalWrapper.createDbKey(), proposalWrapper);
    }

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
          //manager.getDynamicPropertiesStore().saveWitnessPayPerBlock(entry.getValue());
          break;
        }
        case (6): {
          //manager.getDynamicPropertiesStore().saveWitnessStandbyAllowance(entry.getValue());
          break;
        }
        case (7): {
          manager.getDynamicPropertiesStore().saveCreateNewAccountFeeInSystemContract(entry.getValue());
          break;
        }
        case (8): {
          manager.getDynamicPropertiesStore().saveCreateNewAccountBandwidthRate(entry.getValue());
          break;
        }
        case (9): {
          manager.getDynamicPropertiesStore().saveAllowCreationOfContracts(entry.getValue());
          break;
        }
        case (10): {
          manager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(entry.getValue());
          break;
        }
        case (11): {
          manager.getDynamicPropertiesStore().saveEnergyFee(entry.getValue());
          break;
        }
        case (12): {
          manager.getDynamicPropertiesStore().saveExchangeCreateFee(entry.getValue());
          break;
        }
        case (13): {
          manager.getDynamicPropertiesStore().saveMaxCpuTimeOfOneTX(entry.getValue());
          break;
        }
        default:
          break;
      }
    }
  }


}
