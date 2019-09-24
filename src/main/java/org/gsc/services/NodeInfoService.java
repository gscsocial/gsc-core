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

package org.gsc.services;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.net.node.NodeInfo;
import org.gsc.net.node.NodeInfo.ConfigNodeInfo;
import org.gsc.net.node.NodeInfo.MachineInfo;
import org.gsc.net.node.NodeInfo.MachineInfo.DeadLockThreadInfo;
import org.gsc.net.node.NodeInfo.MachineInfo.MemoryDescInfo;
import org.gsc.net.peer.PeerInfo;
import org.gsc.net.node.NodeManager;
import org.gsc.net.server.SyncPool;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.net.peer.PeerConnection;
import org.gsc.services.WitnessProductBlockService.CheatWitnessInfo;
import org.gsc.Version;
import org.gsc.protos.Protocol.ReasonCode;

@Component
public class NodeInfoService {

    private MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory
            .getOperatingSystemMXBean();
    private Args args = Args.getInstance();

    @Autowired
    private SyncPool syncPool;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private Manager dbManager;

    @Autowired
    private WitnessProductBlockService witnessProductBlockService;

    public NodeInfo getNodeInfo() {
        NodeInfo nodeInfo = new NodeInfo();
        setConnectInfo(nodeInfo);
        setMachineInfo(nodeInfo);
        setConfigNodeInfo(nodeInfo);
        setBlockInfo(nodeInfo);
        setCheatWitnessInfo(nodeInfo);
        return nodeInfo;
    }

    private void setMachineInfo(NodeInfo nodeInfo) {
        MachineInfo machineInfo = new MachineInfo();
        machineInfo.setThreadCount(threadMXBean.getThreadCount());
        machineInfo.setCpuCount(Runtime.getRuntime().availableProcessors());
        machineInfo.setTotalMemory(operatingSystemMXBean.getTotalPhysicalMemorySize());
        machineInfo.setFreeMemory(operatingSystemMXBean.getFreePhysicalMemorySize());
        machineInfo.setCpuRate(operatingSystemMXBean.getSystemCpuLoad());
        machineInfo.setJavaVersion(runtimeMXBean.getSystemProperties().get("java.version"));
        machineInfo
                .setOsName(operatingSystemMXBean.getName() + " " + operatingSystemMXBean.getVersion());
        machineInfo.setJvmTotalMemoery(memoryMXBean.getHeapMemoryUsage().getMax());
        machineInfo.setJvmFreeMemory(
                memoryMXBean.getHeapMemoryUsage().getMax() - memoryMXBean.getHeapMemoryUsage().getUsed());
        machineInfo.setProcessCpuRate(operatingSystemMXBean.getProcessCpuLoad());
        List<MemoryDescInfo> memoryDescInfoList = new ArrayList<>();
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        if (CollectionUtils.isNotEmpty(pools)) {
            for (MemoryPoolMXBean pool : pools) {
                MemoryDescInfo memoryDescInfo = new MemoryDescInfo();
                memoryDescInfo.setName(pool.getName());
                memoryDescInfo.setInitSize(pool.getUsage().getInit());
                memoryDescInfo.setUseSize(pool.getUsage().getUsed());
                memoryDescInfo.setMaxSize(pool.getUsage().getMax());
                if (pool.getUsage().getMax() > 0) {
                    memoryDescInfo.setUseRate((double) pool.getUsage().getUsed() / pool.getUsage().getMax());
                } else {
                    memoryDescInfo
                            .setUseRate((double) pool.getUsage().getUsed() / pool.getUsage().getCommitted());
                }
                memoryDescInfoList.add(memoryDescInfo);
            }
        }
        machineInfo.setMemoryDescInfoList(memoryDescInfoList);
        //dead lock thread
        long[] deadlockedIds = threadMXBean.findDeadlockedThreads();
        if (ArrayUtils.isNotEmpty(deadlockedIds)) {
            machineInfo.setDeadLockThreadCount(deadlockedIds.length);
            ThreadInfo[] deadlockInfos = threadMXBean.getThreadInfo(deadlockedIds);
            List<DeadLockThreadInfo> deadLockThreadInfoList = new ArrayList<>();
            for (ThreadInfo deadlockInfo : deadlockInfos) {
                DeadLockThreadInfo deadLockThreadInfo = new DeadLockThreadInfo();
                deadLockThreadInfo.setName(deadlockInfo.getThreadName());
                deadLockThreadInfo.setLockName(deadlockInfo.getLockName());
                deadLockThreadInfo.setLockOwner(deadlockInfo.getLockOwnerName());
                deadLockThreadInfo.setBlockTime(deadlockInfo.getBlockedTime());
                deadLockThreadInfo.setWaitTime(deadlockInfo.getWaitedTime());
                deadLockThreadInfo.setState(deadlockInfo.getThreadState().name());
                deadLockThreadInfo.setStackTrace(Arrays.toString(deadlockInfo.getStackTrace()));
                deadLockThreadInfoList.add(deadLockThreadInfo);
            }
            machineInfo.setDeadLockThreadInfoList(deadLockThreadInfoList);
        }
        nodeInfo.setMachineInfo(machineInfo);
    }

    private void setConnectInfo(NodeInfo nodeInfo) {
        nodeInfo.setCurrentConnectCount(syncPool.getActivePeers().size());
        nodeInfo.setActiveConnectCount(syncPool.getActivePeersCount().get());
        nodeInfo.setPassiveConnectCount(syncPool.getPassivePeersCount().get());
        long totalFlow = 0;
        List<PeerInfo> peerInfoList = new ArrayList<>();
        for (PeerConnection peerConnection : syncPool.getActivePeers()) {
            PeerInfo peerInfo = new PeerInfo();
            peerInfo.setHeadBlockWeBothHave(peerConnection.getBlockBothHave().getString());
            peerInfo.setActive(peerConnection.isActive());
            peerInfo.setAvgLatency(peerConnection.getNodeStatistics().pingMessageLatency.getAvrg());
            peerInfo.setBlockInPorcSize(peerConnection.getSyncBlockInProcess().size());
            peerInfo.setConnectTime(peerConnection.getStartTime());
            peerInfo.setDisconnectTimes(peerConnection.getNodeStatistics().getDisconnectTimes());
            //peerInfo.setHeadBlockTimeWeBothHave(peerConnection.getHeadBlockTimeWeBothHave());
            peerInfo.setHost(peerConnection.getNode().getHost());
            peerInfo.setInFlow(peerConnection.getNodeStatistics().tcpFlow.getTotalCount());
            peerInfo.setLastBlockUpdateTime(peerConnection.getBlockBothHaveUpdateTime());
            peerInfo.setLastSyncBlock(peerConnection.getLastSyncBlockId() == null ? ""
                    : peerConnection.getLastSyncBlockId().getString());
            ReasonCode reasonCode = peerConnection.getNodeStatistics()
                    .getGSCLastLocalDisconnectReason();
            peerInfo.setLocalDisconnectReason(reasonCode == null ? "" : reasonCode.toString());
            reasonCode = peerConnection.getNodeStatistics().getGSCLastRemoteDisconnectReason();
            peerInfo.setRemoteDisconnectReason(reasonCode == null ? "" : reasonCode.toString());
            peerInfo.setNeedSyncFromPeer(peerConnection.isNeedSyncFromPeer());
            peerInfo.setNeedSyncFromUs(peerConnection.isNeedSyncFromUs());
            peerInfo.setNodeCount(nodeManager.getTable().getAllNodes().size());
            peerInfo.setNodeId(peerConnection.getNode().getHexId());
            peerInfo.setPort(peerConnection.getNode().getPort());
            peerInfo.setRemainNum(peerConnection.getRemainNum());
            peerInfo.setScore(peerConnection.getNodeStatistics().getReputation());
            peerInfo.setSyncBlockRequestedSize(peerConnection.getSyncBlockRequested().size());
            peerInfo.setSyncFlag(peerConnection.isDisconnect());
            peerInfo.setSyncToFetchSize(peerConnection.getSyncBlockToFetch().size());
            peerInfo.setSyncToFetchSizePeekNum(peerConnection.getSyncBlockToFetch().size() > 0
                    ? peerConnection.getSyncBlockToFetch().peek().getNum() : -1);
            peerInfo.setUnFetchSynNum(peerConnection.getRemainNum());
            totalFlow += peerConnection.getNodeStatistics().tcpFlow.getTotalCount();
            peerInfoList.add(peerInfo);
        }
        nodeInfo.setPeerList(peerInfoList);
        nodeInfo.setTotalFlow(totalFlow);
    }

    private void setConfigNodeInfo(NodeInfo nodeInfo) {
        ConfigNodeInfo configNodeInfo = new ConfigNodeInfo();
        configNodeInfo.setCodeVersion(Version.getVersion());
        configNodeInfo.setVersionName(Version.versionName);
        configNodeInfo.setVersionNum(Version.versionCode);
        configNodeInfo.setP2pVersion(String.valueOf(args.getNodeP2pVersion()));
        configNodeInfo.setListenPort(args.getNodeListenPort());
        configNodeInfo.setDiscoverEnable(args.isNodeDiscoveryEnable());
        configNodeInfo.setActiveNodeSize(args.getActiveNodes().size());
        configNodeInfo.setPassiveNodeSize(args.getPassiveNodes().size());
       // configNodeInfo.setSendNodeSize(args.getSeedNodes().size());
        configNodeInfo.setSendNodeSize(args.getBootNodes().size());
        configNodeInfo.setMaxConnectCount(args.getNodeMaxActiveNodes());
        configNodeInfo.setSameIpMaxConnectCount(args.getNodeMaxActiveNodesWithSameIp());
        configNodeInfo.setBackupListenPort(args.getBackupPort());
        configNodeInfo.setBackupMemberSize(args.getBackupMembers().size());
        configNodeInfo.setBackupPriority(args.getBackupPriority());
        configNodeInfo.setDbVersion(args.getStorage().getDbVersion());
        configNodeInfo.setMinParticipationRate(args.getMinParticipationRate());
        configNodeInfo.setSupportConstant(args.isSupportConstant());
        configNodeInfo.setMinTimeRatio(args.getMinTimeRatio());
        configNodeInfo.setMaxTimeRatio(args.getMaxTimeRatio());
        configNodeInfo.setAllowCreationOfContracts(args.getAllowCreationOfContracts());
        configNodeInfo.setAllowAdaptiveCpu(args.getAllowAdaptiveCpu());
        nodeInfo.setConfigNodeInfo(configNodeInfo);
    }

    protected void setBlockInfo(NodeInfo nodeInfo) {
        nodeInfo.setBeginSyncNum(dbManager.getSyncBeginNumber());
        nodeInfo.setBlock(dbManager.getHeadBlockId().getString());
        nodeInfo.setConfirmedBlock(dbManager.getConfirmedBlockId().getString());
    }

    protected void setCheatWitnessInfo(NodeInfo nodeInfo) {
        for (Entry<String, CheatWitnessInfo> entry : witnessProductBlockService.queryCheatWitnessInfo()
                .entrySet()) {
            nodeInfo.getCheatWitnessInfoMap().put(entry.getKey(), entry.getValue().toString());
        }
    }

}
