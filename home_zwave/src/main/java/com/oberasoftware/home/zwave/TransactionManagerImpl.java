package com.oberasoftware.home.zwave;

import com.oberasoftware.home.api.events.EventBus;
import com.oberasoftware.home.api.exceptions.HomeAutomationException;
import com.oberasoftware.home.zwave.api.ZWaveAction;
import com.oberasoftware.home.zwave.api.ZWaveDeviceAction;
import com.oberasoftware.home.zwave.api.events.ControllerEvent;
import com.oberasoftware.home.zwave.api.events.WaitForWakeUpAction;
import com.oberasoftware.home.zwave.connector.ControllerConnector;
import com.oberasoftware.home.zwave.converter.ConverterHandler;
import com.oberasoftware.home.zwave.core.NodeManager;
import com.oberasoftware.home.zwave.core.ZWaveNode;
import com.oberasoftware.home.zwave.messages.ZWaveRawMessage;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

import static com.oberasoftware.home.zwave.core.NodeAvailability.AWAKE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
@Component
public class TransactionManagerImpl implements TransactionManager {
    private static final Logger LOG = getLogger(TransactionManagerImpl.class);

    @Autowired
    private ControllerConnector connector;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private ConverterHandler<ZWaveAction, ZWaveRawMessage> converterHandler;

    private AtomicInteger callbackGenerator = new AtomicInteger(1);

    @Override
    public int startAction(ZWaveAction action) throws HomeAutomationException {
        if (action instanceof WaitForWakeUpAction) {
            WaitForWakeUpAction wakeUpEventAction = (WaitForWakeUpAction) action;
            LOG.debug("Starting a parked wake up event action: {} with callback: {}", action, wakeUpEventAction.getCallbackId());

            convertAndSendMessage(wakeUpEventAction.getDeviceAction(), wakeUpEventAction.getCallbackId());

            return wakeUpEventAction.getCallbackId();
        } else {
            int callbackId = getCallbackId();

            if (isDeviceReady(action)) {
                convertAndSendMessage(action, callbackId);
            } else {
                LOG.debug("Starting action on battery device: {} that could be asleep", action);
                eventBus.pushAsync(new WaitForWakeUpAction((ZWaveDeviceAction) action, callbackId));
            }

            return callbackId;
        }
    }

    private void convertAndSendMessage(ZWaveAction action, int callbackId) throws HomeAutomationException {
        LOG.debug("Starting device action: {}", action);
        ZWaveRawMessage rawMessage = converterHandler.convert(v -> v.getClass().getSimpleName(), action);
        if (rawMessage != null) {
            rawMessage.setCallbackId(callbackId);
            rawMessage.setTransmitOptions(0x01 | 0x04 | 0x20);

            connector.send(rawMessage);
        } else {
            LOG.error("Message could not be converted, cannot send action: {}", action);
        }
    }

    private boolean isDeviceReady(ZWaveAction action) {
        if(action instanceof ZWaveDeviceAction) {
            ZWaveNode node = nodeManager.getNode(((ZWaveDeviceAction) action).getNodeId());

            if(node != null) {
                boolean batteryDevice = nodeManager.isBatteryDevice(node.getNodeId());

                LOG.debug("This device: {} is a battery device: {}", ((ZWaveDeviceAction) action).getNodeId(), batteryDevice);

                //we can send if the device is a battery device that is awake, or if not a battery device at all
                return (batteryDevice && node.getAvailability() == AWAKE) || !batteryDevice;
            } else {
                LOG.warn("We have no node information for node: {} device is not ready", ((ZWaveDeviceAction) action).getNodeId());
                return false;
            }
        }

        return true;
    }

    @Override
    public void completeTransaction(ControllerEvent controllerEvent) throws HomeAutomationException {
        connector.completeTransaction();
    }

    @Override
    public void cancelTransaction() throws HomeAutomationException {
        connector.completeTransaction();
    }

    private int getCallbackId() {
        int callbackId = callbackGenerator.incrementAndGet();
        if(callbackId > 0xFF) {
            callbackId = callbackGenerator.getAndSet(1);
        }
        return callbackId;
    }
}
