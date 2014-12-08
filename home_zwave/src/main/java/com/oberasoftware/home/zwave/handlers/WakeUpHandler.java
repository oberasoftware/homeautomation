package com.oberasoftware.home.zwave.handlers;

import com.oberasoftware.home.api.EventListener;
import com.oberasoftware.home.api.events.Subscribe;
import com.oberasoftware.home.zwave.TransactionManager;
import com.oberasoftware.home.zwave.api.events.SendDataEvent;
import com.oberasoftware.home.zwave.api.events.WaitForWakeUpAction;
import com.oberasoftware.home.zwave.api.events.devices.WakeUpNoMoreInformationEvent;
import com.oberasoftware.home.zwave.api.events.devices.WakeUpReceivedEvent;
import com.oberasoftware.home.zwave.core.NodeManager;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import static com.oberasoftware.home.zwave.core.NodeAvailability.AWAKE;
import static com.oberasoftware.home.zwave.core.NodeAvailability.SLEEPING;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
@Component
public class WakeUpHandler implements EventListener<WakeUpReceivedEvent> {
    private static final Logger LOG = getLogger(WakeUpHandler.class);

    private final ConcurrentMap<Integer, Queue<WaitForWakeUpAction>> waitForWakeUpEvents = new ConcurrentHashMap<>();

    @Autowired
    private TransactionManager transactionManager;

    @Autowired
    private NodeManager nodeManager;

    @Override
    public void receive(WakeUpReceivedEvent event) throws Exception {
        LOG.debug("Received a wake-up event for node: {}", event.getNodeId());

        nodeManager.setNodeAvailability(event.getNodeId(), AWAKE);

        queueDeviceAction(event.getNodeId());
    }

    @Subscribe
    public void dateReceived(SendDataEvent sendDataEvent) throws Exception {
        LOG.debug("Received a node confirmation from node: {}", sendDataEvent.getNodeId());
        queueDeviceAction(sendDataEvent.getNodeId());
    }

    private void queueDeviceAction(int nodeId) throws Exception {
        if(nodeId != 255) {
            Queue<WaitForWakeUpAction> queue = waitForWakeUpEvents.getOrDefault(nodeId, new LinkedBlockingQueue<>());
            LOG.debug("Sending device actions to node: {} actions waiting: {}", nodeId, queue.size());

            WaitForWakeUpAction deviceEvent = queue.poll();
            if (deviceEvent != null) {
                transactionManager.startAction(deviceEvent);
            } else {
                LOG.debug("No action waiting for device: {}, sending sleep message", nodeId);

                nodeManager.setNodeAvailability(nodeId, SLEEPING);

//                transactionManager.startAction(new NoMoreInformationAction(nodeId));
            }
        }
    }

    @Subscribe
    public void wakeUpNoMoreInformation(WakeUpNoMoreInformationEvent noMoreInformationEvent) {
        LOG.debug("Node: {} has gone back to sleep", noMoreInformationEvent.getNodeId());
        nodeManager.setNodeAvailability(noMoreInformationEvent.getNodeId(), SLEEPING);
    }

    @Subscribe
    public void waitForWakeUp(WaitForWakeUpAction waitForWakeUpEvent) {
        LOG.debug("Received an action: {} that needs to wait for device wake-up", waitForWakeUpEvent);

        waitForWakeUpEvents.putIfAbsent(waitForWakeUpEvent.getNodeId(), new ConcurrentLinkedQueue<>());
        waitForWakeUpEvents.get(waitForWakeUpEvent.getNodeId()).add(waitForWakeUpEvent);
    }
}
