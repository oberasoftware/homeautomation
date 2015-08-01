package com.oberasoftware.home.rules.evaluators.blocks;

import com.oberasoftware.home.api.AutomationBus;
import com.oberasoftware.home.api.commands.DeviceCommand;
import com.oberasoftware.home.api.events.devices.DeviceCommandEvent;
import com.oberasoftware.home.core.commands.SwitchCommandImpl;
import com.oberasoftware.home.rules.api.general.SwitchItem;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Renze de Vries
 */
@Component
public class SwitchItemEvaluator implements BlockEvaluator<SwitchItem> {
    private static final Logger LOG = getLogger(SwitchItemEvaluator.class);

    @Autowired
    private AutomationBus automationBus;

    @Override
    public Boolean eval(SwitchItem input) {
        LOG.debug("Generating a switch command: {}", input);

        DeviceCommand command = new SwitchCommandImpl(input.getItemId(), input.getState());

        automationBus.publish(new DeviceCommandEvent(input.getItemId(), command));

        return true;
    }
}
