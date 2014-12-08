package com.oberasoftware.home.zwave.handlers;

import com.oberasoftware.home.api.EventListener;
import com.oberasoftware.home.api.events.Subscribe;
import com.oberasoftware.home.api.exceptions.HomeAutomationException;
import com.oberasoftware.home.zwave.ZWaveController;
import com.oberasoftware.home.zwave.api.ZWaveAction;
import com.oberasoftware.home.zwave.api.actions.devices.DeviceManufactorAction;
import com.oberasoftware.home.zwave.api.actions.devices.IdentifyNodeAction;
import com.oberasoftware.home.zwave.api.actions.devices.NodeNoOpAction;
import com.oberasoftware.home.zwave.api.events.SendDataEvent;
import com.oberasoftware.home.zwave.api.events.controller.ControllerInitialDataEvent;
import com.oberasoftware.home.zwave.api.events.controller.NodeInformationEvent;
import com.oberasoftware.home.zwave.api.events.devices.ManufactorInfoEvent;
import com.oberasoftware.home.zwave.core.NodeManager;
import com.oberasoftware.home.zwave.core.NodeStatus;
import com.oberasoftware.home.zwave.core.utils.EventSupplier;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.oberasoftware.home.zwave.core.NodeAvailability.AVAILABLE;
import static com.oberasoftware.home.zwave.core.NodeAvailability.AWAKE;
import static com.oberasoftware.home.zwave.core.NodeStatus.ACTIVE;
import static com.oberasoftware.home.zwave.core.NodeStatus.INITIALIZED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
@Component
public class NodeEventHandler implements EventListener<ControllerInitialDataEvent> {
    private static final Logger LOG = getLogger(NodeEventHandler.class);

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private ZWaveController zWaveController;

    private ConcurrentLinkedQueue<Integer> nodeInformationRequests = new ConcurrentLinkedQueue<>();

    private Map<Integer, Integer> outstandingNodeActions = new ConcurrentHashMap<>();

    @Override
    public void receive(ControllerInitialDataEvent event) throws Exception {
        LOG.debug("Received a initial controller data with node information: {}", event);

        event.getNodeIds().forEach(n -> {
            LOG.debug("Registering node: {}", n);
            nodeManager.registerNode(n);

            nodeInformationRequests.add(n);

            send(() -> new IdentifyNodeAction(n));
        });
    }

    @Subscribe
    public void receiveNodeInformation(NodeInformationEvent nodeInformationEvent) {
        LOG.debug("Received node information: {}", nodeInformationEvent);

        if(!nodeInformationRequests.isEmpty()) {
            int nodeId = nodeInformationRequests.poll();

            nodeManager.setNodeInformation(nodeId, nodeInformationEvent);
            nodeManager.setNodeStatus(nodeId, NodeStatus.IDENTIFIED);
            LOG.debug("Received identity information for node: {}", nodeId);

            if(nodeId != zWaveController.getControllerId()) {
                int callbackId = send(() -> new NodeNoOpAction(nodeId));

                outstandingNodeActions.put(callbackId, nodeId);
            } else {
                LOG.debug("Received information for Controller: {}, advancing stage to: {}", nodeId, INITIALIZED);
                nodeManager.setNodeStatus(nodeId, INITIALIZED);
                nodeManager.setNodeAvailability(nodeId, AVAILABLE);
            }
        }
    }

    @Subscribe
    public void receivedManufactorInformation(ManufactorInfoEvent manufactorInfoEvent) {
        LOG.debug("Received device manufacturer info: {} for node: {}", manufactorInfoEvent, manufactorInfoEvent.getNodeId());

        nodeManager.setNodeInformation(manufactorInfoEvent.getNodeId(), manufactorInfoEvent);
        nodeManager.setNodeStatus(manufactorInfoEvent.getNodeId(), NodeStatus.INITIALIZED);
    }

    @Subscribe
    public void receivePing(SendDataEvent sendDataEvent) {
        if(outstandingNodeActions.containsKey(sendDataEvent.getCallbackId())) {
            int nodeId = outstandingNodeActions.remove(sendDataEvent.getCallbackId());

            LOG.debug("Received a callback from node: {} for callback: {}", nodeId, sendDataEvent.getCallbackId());

            LOG.debug("Setting node: {} status to: {}", nodeId, ACTIVE);
            nodeManager.setNodeStatus(nodeId, ACTIVE);

            if(!nodeManager.isBatteryDevice(nodeId)) {
                nodeManager.setNodeAvailability(nodeId, AVAILABLE);
            } else {
                nodeManager.setNodeAvailability(nodeId, AWAKE);
            }

            send(() -> new DeviceManufactorAction(nodeId));
        }
    }

    public int getNodeId(int callbackId) {
        return outstandingNodeActions.getOrDefault(callbackId, -1);
    }

    private int send(EventSupplier<ZWaveAction> delegate) {
        try {
            return zWaveController.send(delegate.get());
        } catch(HomeAutomationException e) {
            LOG.error("Could not submit ZWaveController event", e);
        }

        return -1;
    }
}
