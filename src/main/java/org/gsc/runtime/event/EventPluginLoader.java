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

package org.gsc.runtime.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.CompoundPluginDescriptorFinder;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginManager;
import org.springframework.util.StringUtils;
import org.gsc.runtime.event.nativequeue.NativeMessageQueue;
import org.gsc.runtime.event.trigger.BlockLogTrigger;
import org.gsc.runtime.event.trigger.ContractEventTrigger;
import org.gsc.runtime.event.trigger.ContractLogTrigger;
import org.gsc.runtime.event.trigger.TransactionLogTrigger;
import org.gsc.runtime.event.trigger.Trigger;

@Slf4j
public class EventPluginLoader {

    private static EventPluginLoader instance;

    private PluginManager pluginManager = null;

    private List<IPluginEventListener> eventListeners;

    private ObjectMapper objectMapper = new ObjectMapper();

    private String serverAddress;

    private String dbConfig;

    private List<TriggerConfig> triggerConfigList;

    private boolean blockLogTriggerEnable = false;

    private boolean transactionLogTriggerEnable = false;

    private boolean contractEventTriggerEnable = false;

    private boolean contractLogTriggerEnable = false;

    private FilterQuery filterQuery;

    private boolean useNativeQueue = false;

    public static EventPluginLoader getInstance() {
        if (Objects.isNull(instance)) {
            synchronized (EventPluginLoader.class) {
                if (Objects.isNull(instance)) {
                    instance = new EventPluginLoader();
                }
            }
        }
        return instance;
    }

    private boolean launchNativeQueue(EventPluginConfig config) {

        if (!NativeMessageQueue.getInstance().start(config.getBindPort(), config.getSendQueueLength())) {
            return false;
        }

        if (Objects.isNull(triggerConfigList)) {
            logger.error("trigger config is null");
            return false;
        }

        triggerConfigList.forEach(triggerConfig -> {
            setSingleTriggerConfig(triggerConfig);
        });

        return true;
    }

    private boolean launchEventPlugin(EventPluginConfig config) {
        boolean success = false;
        // parsing subscribe config from config.conf
        String pluginPath = config.getPluginPath();
        this.serverAddress = config.getServerAddress();
        this.dbConfig = config.getDbConfig();

        if (!startPlugin(pluginPath)) {
            logger.error("failed to load '{}'", pluginPath);
            return success;
        }

        setPluginConfig();

        if (Objects.nonNull(eventListeners)) {
            eventListeners.forEach(listener -> listener.start());
        }

        return true;
    }

    private void setPluginConfig() {

        if (Objects.isNull(eventListeners)) {
            return;
        }

        // set server address to plugin
        eventListeners.forEach(listener -> listener.setServerAddress(this.serverAddress));

        // set dbconfig to plugin
        eventListeners.forEach(listener -> listener.setDBConfig(this.dbConfig));

        triggerConfigList.forEach(triggerConfig -> {
            setSingleTriggerConfig(triggerConfig);
        });
    }

    public boolean start(EventPluginConfig config) {
        if (Objects.isNull(config)) {
            return false;
        }

        this.triggerConfigList = config.getTriggerConfigList();

        useNativeQueue = config.isUseNativeQueue();

        if (config.isUseNativeQueue()) {
            return launchNativeQueue(config);
        }

        return launchEventPlugin(config);
    }

    private void setSingleTriggerConfig(TriggerConfig triggerConfig) {
        if (EventPluginConfig.BLOCK_TRIGGER_NAME.equalsIgnoreCase(triggerConfig.getTriggerName())) {
            if (triggerConfig.isEnabled()) {
                blockLogTriggerEnable = true;
            } else {
                blockLogTriggerEnable = false;
            }

            if (!useNativeQueue) {
                setPluginTopic(Trigger.BLOCK_TRIGGER, triggerConfig.getTopic());
            }

        } else if (EventPluginConfig.TRANSACTION_TRIGGER_NAME
                .equalsIgnoreCase(triggerConfig.getTriggerName())) {
            if (triggerConfig.isEnabled()) {
                transactionLogTriggerEnable = true;
            } else {
                transactionLogTriggerEnable = false;
            }

            if (!useNativeQueue) {
                setPluginTopic(Trigger.TRANSACTION_TRIGGER, triggerConfig.getTopic());
            }

        } else if (EventPluginConfig.CONTRACTEVENT_TRIGGER_NAME
                .equalsIgnoreCase(triggerConfig.getTriggerName())) {
            if (triggerConfig.isEnabled()) {
                contractEventTriggerEnable = true;
            } else {
                contractEventTriggerEnable = false;
            }

            if (!useNativeQueue) {
                setPluginTopic(Trigger.CONTRACTEVENT_TRIGGER, triggerConfig.getTopic());
            }

        } else if (EventPluginConfig.CONTRACTLOG_TRIGGER_NAME
                .equalsIgnoreCase(triggerConfig.getTriggerName())) {
            if (triggerConfig.isEnabled()) {
                contractLogTriggerEnable = true;
            } else {
                contractLogTriggerEnable = false;
            }

            if (!useNativeQueue) {
                setPluginTopic(Trigger.CONTRACTLOG_TRIGGER, triggerConfig.getTopic());
            }
        }
    }

    public synchronized boolean isBlockLogTriggerEnable() {
        return blockLogTriggerEnable;
    }

    public synchronized boolean isTransactionLogTriggerEnable() {
        return transactionLogTriggerEnable;
    }

    public synchronized boolean isContractEventTriggerEnable() {
        return contractEventTriggerEnable;
    }

    public synchronized boolean isContractLogTriggerEnable() {
        return contractLogTriggerEnable;
    }

    private void setPluginTopic(int eventType, String topic) {
        eventListeners.forEach(listener -> listener.setTopic(eventType, topic));
    }

    private boolean startPlugin(String path) {
        logger.info("start loading '{}'", path);

        File pluginPath = new File(path);
        if (!pluginPath.exists()) {
            logger.error("'{}' doesn't exist", path);
            return false;
        }

        if (Objects.isNull(pluginManager)) {

            pluginManager = new DefaultPluginManager(pluginPath.toPath()) {
                @Override
                protected CompoundPluginDescriptorFinder createPluginDescriptorFinder() {
                    return new CompoundPluginDescriptorFinder()
                            .add(new ManifestPluginDescriptorFinder());
                }
            };
        }

        String pluginId = pluginManager.loadPlugin(pluginPath.toPath());
        if (StringUtils.isEmpty(pluginId)) {
            logger.error("invalid pluginID");
            return false;
        }

        pluginManager.startPlugins();

        eventListeners = pluginManager.getExtensions(IPluginEventListener.class);

        if (Objects.isNull(eventListeners) || eventListeners.isEmpty()) {
            logger.error("No eventListener is registered");
            return false;
        }

        logger.info("'{}' loaded", path);

        return true;
    }

    public void stopPlugin() {
        if (Objects.nonNull(pluginManager)) {
            pluginManager.stopPlugins();
        }

        NativeMessageQueue.getInstance().stop();

        logger.info("eventPlugin stopped");
    }

    public void postBlockTrigger(BlockLogTrigger trigger) {
        if (useNativeQueue) {
            NativeMessageQueue.getInstance().publishTrigger(toJsonString(trigger), trigger.getTriggerName());
        } else {
            eventListeners.forEach(listener ->
                    listener.handleBlockEvent(toJsonString(trigger)));
        }
    }

    public void postTransactionTrigger(TransactionLogTrigger trigger) {
        if (useNativeQueue) {
            NativeMessageQueue.getInstance().publishTrigger(toJsonString(trigger), trigger.getTriggerName());
        } else {
            eventListeners.forEach(listener -> listener.handleTransactionTrigger(toJsonString(trigger)));
        }
    }

    public void postContractLogTrigger(ContractLogTrigger trigger) {
        if (useNativeQueue) {
            NativeMessageQueue.getInstance().publishTrigger(toJsonString(trigger), trigger.getTriggerName());
        } else {
            eventListeners.forEach(listener ->
                    listener.handleContractLogTrigger(toJsonString(trigger)));
        }
    }

    public void postContractEventTrigger(ContractEventTrigger trigger) {
        if (useNativeQueue) {
            NativeMessageQueue.getInstance().publishTrigger(toJsonString(trigger), trigger.getTriggerName());
        } else {
            eventListeners.forEach(listener ->
                    listener.handleContractEventTrigger(toJsonString(trigger)));
        }
    }

    public synchronized void setFilterQuery(FilterQuery filterQuery) {
        this.filterQuery = filterQuery;
    }

    public synchronized FilterQuery getFilterQuery() {
        return filterQuery;
    }

    private String toJsonString(Object data) {
        String jsonData = "";

        try {
            jsonData = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            logger.error("'{}'", e);
        }

        return jsonData;
    }
}
