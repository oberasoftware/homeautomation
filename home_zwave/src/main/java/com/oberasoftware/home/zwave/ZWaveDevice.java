package com.oberasoftware.home.zwave;

import com.oberasoftware.home.api.model.Device;
import com.oberasoftware.home.api.model.Status;
import com.oberasoftware.home.zwave.api.events.controller.NodeInformationEvent;
import com.oberasoftware.home.zwave.api.events.devices.ManufactorInfoEvent;
import com.oberasoftware.home.zwave.core.ZWaveNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author renarj
 */
public class ZWaveDevice implements Device {

    private final ZWaveNode node;

    public ZWaveDevice(ZWaveNode node) {
        this.node = node;
    }

    @Override
    public String getId() {
        return Integer.toString(node.getNodeId());
    }

    @Override
    public String getName() {
        return getId();
    }

    @Override
    public Status getStatus() {
        switch(node.getNodeStatus()) {
            case IDENTIFIED:
            case ACTIVE:
                return Status.READY;
            case INITIALIZED:
            case INITIALIZING:
                return Status.DISCOVERED;
            case UNKNOWN:
            default:
                return Status.UNKNOWN;
        }
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> deviceProperties = new HashMap<>();
        deviceProperties.put("nodeId", Integer.toString(node.getNodeId()));
        deviceProperties.put("status", node.getNodeStatus().toString());
        deviceProperties.put("availability", node.getAvailability().toString());

        Optional<ManufactorInfoEvent> manufactorInfoEventOptional = node.getManufactorInfoEvent();
        Optional<NodeInformationEvent> nodeInformationEventOptional = node.getNodeInformation();
        if(manufactorInfoEventOptional.isPresent()) {
            ManufactorInfoEvent manufactorInfoEvent = manufactorInfoEventOptional.get();
            deviceProperties.put("manufacturerId", Integer.toString(manufactorInfoEvent.getManufactorId()));
            deviceProperties.put("deviceId", Integer.toString(manufactorInfoEvent.getDeviceId()));
            deviceProperties.put("deviceType", Integer.toString(manufactorInfoEvent.getDeviceType()));
        }

        if(nodeInformationEventOptional.isPresent()) {
            NodeInformationEvent nodeInformationEvent = nodeInformationEventOptional.get();

            deviceProperties.put("batteryDevice", Boolean.toString(nodeInformationEvent.isListening()));
            deviceProperties.put("routingDevice", Boolean.toString(nodeInformationEvent.isRouting()));
            deviceProperties.put("deviceVersion", Integer.toString(nodeInformationEvent.getVersion()));
        }

        return deviceProperties;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ZWaveDevice{");
        builder.append("id=").append(getId());
        builder.append(",name=").append(getName());
        builder.append(",status=").append(getStatus().toString());
        builder.append("}");

        return builder.toString();
    }
}
