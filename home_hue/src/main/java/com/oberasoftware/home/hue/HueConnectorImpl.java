package com.oberasoftware.home.hue;

import com.oberasoftware.home.api.events.EventBus;
import com.oberasoftware.home.api.events.EventHandler;
import com.oberasoftware.home.api.events.EventSubscribe;
import com.oberasoftware.home.api.storage.model.DevicePlugin;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueParsingError;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
@Component
public class HueConnectorImpl implements EventHandler, HueConnector {
    private static final Logger LOG = getLogger(HueConnectorImpl.class);

    private PHHueSDK sdk;
    private PHAccessPoint ap;

    private AtomicBoolean connected = new AtomicBoolean(false);

    @Autowired
    private EventBus automationBus;

    @Override
    public void connect(Optional<DevicePlugin> pluginItem) {
        LOG.info("Connecting to Philips HUE bridge");

        sdk = PHHueSDK.create();
        sdk.setAppName("Home Automation System");

        sdk.getNotificationManager().registerSDKListener(new HueListener());

        if(!pluginItem.isPresent()) {
            LOG.info("No existing bridge found, searching for a bridge");
            PHBridgeSearchManager sm = (PHBridgeSearchManager) sdk.getSDKService(PHHueSDK.SEARCH_BRIDGE);
            sm.search(true, true);
        } else {
            Map<String, String> properties = pluginItem.get().getProperties();
            String bridgeIp = properties.get("bridgeIp");
            String bridgeUser = properties.get("username");

            LOG.info("Existing bridge found: {}", bridgeIp);
            automationBus.publish(new HueBridgeDiscovered(bridgeIp, bridgeUser));
        }
    }

    @Override
    public PHHueSDK getSdk() {
        return sdk;
    }

    private class HueListener implements PHSDKListener {
        @Override
        public void onCacheUpdated(List<Integer> list, PHBridge phBridge) {
            LOG.debug("Cache updated");
        }

        @Override
        public void onBridgeConnected(PHBridge phBridge) {
            LOG.info("Bridge connected: {}", phBridge);
            connected.set(true);

            sdk.setSelectedBridge(phBridge);
            sdk.enableHeartbeat(phBridge, PHHueSDK.HB_INTERVAL);
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint phAccessPoint) {
            LOG.info("Hue authentication required: {}", phAccessPoint.getIpAddress());
            automationBus.publish(new HueBridgeAuthEvent(phAccessPoint));
        }

        @Override
        public void onAccessPointsFound(List<PHAccessPoint> list) {
            list.forEach(a -> LOG.debug("Found accesspoint: {}", a.getIpAddress()));

            if(list.size() == 1) {
                Optional<PHAccessPoint> ap = list.stream().findFirst();

                ap.ifPresent(a -> automationBus.publish(new HueBridgeDiscovered(a.getIpAddress(), UUID.randomUUID().toString())));
            } else {
                LOG.warn("Detected multiple accesspoints");
            }
        }

        @Override
        public void onError(int i, String s) {
            LOG.error("Hue Connection error: {} reason: {}", i, s);
        }

        @Override
        public void onConnectionResumed(PHBridge phBridge) {
            LOG.debug("Connection resumed");
        }

        @Override
        public void onConnectionLost(PHAccessPoint phAccessPoint) {
            LOG.debug("Connection lost");
        }

        @Override
        public void onParsingErrors(List<PHHueParsingError> list) {
            LOG.debug("Parsing error");
        }
    }

    @EventSubscribe
    public void receive(HueBridgeDiscovered bridgeEvent) {
        LOG.info("Connecting to bridge: {} with user: {}", bridgeEvent.getBridgeIp(), bridgeEvent.getUsername());

        ap = new PHAccessPoint();
        ap.setIpAddress(bridgeEvent.getBridgeIp());
        ap.setUsername(bridgeEvent.getUsername());

        sdk.connect(ap);
    }

    @EventSubscribe
    public void receive(HueBridgeAuthEvent authEvent) {
        LOG.info("Authentication on bridge required: {}", authEvent);

        sdk.startPushlinkAuthentication(authEvent.getAp());
        LOG.info("Please push the link button on your Philips Hue Bridge: {}", authEvent.getAp().getIpAddress());
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    public void setAutomationBus(EventBus automationBus) {
        this.automationBus = automationBus;
    }
}
