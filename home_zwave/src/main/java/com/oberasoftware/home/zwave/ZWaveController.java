package com.oberasoftware.home.zwave;

import com.oberasoftware.home.api.EventListener;
import com.oberasoftware.home.api.events.EventBus;
import com.oberasoftware.home.api.exceptions.HomeAutomationException;
import com.oberasoftware.home.zwave.api.Controller;
import com.oberasoftware.home.zwave.api.ZWaveAction;
import com.oberasoftware.home.zwave.api.events.controller.ControllerIdEvent;
import com.oberasoftware.home.zwave.connector.ControllerConnector;
import com.oberasoftware.home.zwave.core.NodeManager;
import com.oberasoftware.home.zwave.core.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * @author renarj
 */
@Component
public class ZWaveController implements Controller, EventListener<ControllerIdEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(ZWaveController.class);

    @Autowired
    private ControllerConnector connector;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private TransactionManager transactionManager;

    private ControllerIdEvent controllerIdEvent;

    @PreDestroy
    public void disconnect() throws HomeAutomationException {
        if(this.connector != null) {
            LOG.info("Disconnecting ZWave controller connector: {}", this.connector);
            this.connector.close();
        }
    }

    @Override
    public <T> void subscribe(EventListener<T> eventListener) {
        eventBus.addListener(eventListener);
    }

    @Override
    public int send(ZWaveAction message) throws HomeAutomationException {
        return transactionManager.startAction(message);
    }

    @Override
    public boolean isNetworkReady() {
        return controllerIdEvent != null && !nodeManager.getNodes().isEmpty() && nodeManager.haveNodeMinimalStatus(NodeStatus.INITIALIZING);
    }

    @Override
    public int getControllerId() {
        return controllerIdEvent != null ? controllerIdEvent.getControllerId() : -1;
    }

    @Override
    public void receive(ControllerIdEvent event) throws Exception {
        LOG.info("Received controller information: {}", event);

        this.controllerIdEvent = event;
    }
}
