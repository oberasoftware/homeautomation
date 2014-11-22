package com.oberasoftware.home.zwave.converter.controller;

import com.google.common.collect.Sets;
import com.oberasoftware.home.api.exceptions.HomeAutomationException;

import com.oberasoftware.home.zwave.api.events.SendDataEvent;
import com.oberasoftware.home.zwave.api.events.SendDataStateEvent;
import com.oberasoftware.home.zwave.converter.ZWaveConverter;
import com.oberasoftware.home.zwave.exceptions.ZWaveConverterException;
import com.oberasoftware.home.zwave.messages.ControllerMessageType;
import com.oberasoftware.home.zwave.messages.TransmissionState;
import com.oberasoftware.home.zwave.messages.ZWaveRawMessage;
import org.slf4j.Logger;

import com.oberasoftware.home.zwave.api.events.SEND_STATE;
import static com.oberasoftware.home.zwave.api.events.SEND_STATE.FAILED;
import static com.oberasoftware.home.zwave.api.events.SEND_STATE.SUCCESS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

/**
 * @author renarj
 */
public class SendDataConverter implements ZWaveConverter<ZWaveRawMessage, SendDataStateEvent> {
    private static final Logger LOG = getLogger(SendDataConverter.class);

    @Override
    public Set<String> getSupportedTypeNames() {
        return Sets.newHashSet(ControllerMessageType.SendData.getLabel());
    }

    @Override
    public SendDataStateEvent convert(ZWaveRawMessage source) throws HomeAutomationException {
        switch(source.getMessageType()) {
            case Response:
                SEND_STATE state = source.getMessageByte(0) != 0x00 ? SUCCESS : FAILED;

                LOG.debug("Received a response message with status: {}", state);
                return new SendDataStateEvent(state);
            case Request:
                int callbackId = source.getMessageByte(0);
                TransmissionState transmissionState = TransmissionState.getTransmissionState(source.getMessageByte(1));

                LOG.debug("Received a request message for callback: ", callbackId);
                return new SendDataEvent(source.getNodeId(), callbackId, transmissionState == TransmissionState.COMPLETE_OK ? SUCCESS : FAILED);
            default:
                throw new ZWaveConverterException("Unable to convert message of SendData type: " + source);
        }
    }
}
