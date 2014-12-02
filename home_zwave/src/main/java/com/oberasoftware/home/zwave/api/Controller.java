package com.oberasoftware.home.zwave.api;

import com.oberasoftware.home.api.EventListener;
import com.oberasoftware.home.api.exceptions.HomeAutomationException;

/**
 * @author renarj
 */
public interface Controller {
    int getControllerId();

    <T> void subscribe(EventListener<T> topicListener);

    int send(ZWaveAction message) throws HomeAutomationException;

    boolean isNetworkReady();
}
