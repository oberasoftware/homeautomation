package com.oberasoftware.home.zwave.converter.controller;

import com.google.common.collect.Sets;
import com.oberasoftware.home.api.exceptions.HomeAutomationException;
import com.oberasoftware.home.zwave.api.events.controller.ControllerInformationEvent;
import com.oberasoftware.home.zwave.converter.ZWaveConverter;
import com.oberasoftware.home.zwave.messages.types.CommandClass;
import com.oberasoftware.home.zwave.messages.types.ControllerMessageType;
import com.oberasoftware.home.zwave.core.utils.MessageUtil;
import com.oberasoftware.home.zwave.messages.ZWaveRawMessage;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static java.lang.Integer.toHexString;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
public class ControllerInformationConverter implements ZWaveConverter<ZWaveRawMessage, ControllerInformationEvent> {
    private static final Logger LOG = getLogger(ControllerInformationConverter.class);


    @Override
    public Set<String> getSupportedTypeNames() {
        return Sets.newHashSet(ControllerMessageType.GetCapabilities.getLabel());
    }

    @Override
    public ControllerInformationEvent convert(ZWaveRawMessage message) throws HomeAutomationException {
        byte[] payload = message.getMessage();
        LOG.debug("Handle Controller Information reuqest, message length {}", payload.length);

        String serialAPIVersion = String.format("%d.%d", message.getMessageByte(0), message.getMessageByte(1));
        int manufactureId = ((message.getMessageByte(2)) << 8) | (message.getMessageByte(3));
        int deviceType = ((message.getMessageByte(4)) << 8) | (message.getMessageByte(5));
        int deviceId = (((message.getMessageByte(6)) << 8) | (message.getMessageByte(7)));

        LOG.debug("API Version = {}", serialAPIVersion);
        LOG.debug("Manufacture ID: {}", toHexString(manufactureId));
        LOG.debug("Device Type: {}" , toHexString(deviceType));
        LOG.debug("Device ID: {}", toHexString(deviceId));

        // Print the list of messages supported by this controller
        Set<CommandClass> supportedCommandClasses = new HashSet<>();
        for (int by = 8; by < payload.length; by++) {
            for (int bi = 0; bi < 8; bi++) {
                if ((payload[by] & (0x01 << bi)) != 0) {
                    CommandClass commandClass = MessageUtil.getCommandClass(((by - 8) << 3) + bi + 1);

                    if(commandClass != null) {
                        LOG.debug("Supports command class: {}", commandClass.getLabel());
                        supportedCommandClasses.add(commandClass);
                    } else {
                        LOG.debug("Supports: Unknown Command Class {}", ((by - 8) << 3) + bi + 1);
                    }
                }
            }
        }

        return new ControllerInformationEvent(serialAPIVersion, manufactureId, deviceType, deviceId, supportedCommandClasses);
    }
}
