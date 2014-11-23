package com.oberasoftware.home.zwave;

import com.oberasoftware.home.api.exceptions.HomeAutomationException;
import com.oberasoftware.home.zwave.api.ZWaveAction;
import com.oberasoftware.home.zwave.api.events.ControllerEvent;

/**
 * @author renarj
 */
public interface TransactionManager {
    void startAction(ZWaveAction action) throws HomeAutomationException;

    void completeTransaction(ControllerEvent controllerEvent) throws HomeAutomationException;

    void cancelTransaction() throws HomeAutomationException;
}
