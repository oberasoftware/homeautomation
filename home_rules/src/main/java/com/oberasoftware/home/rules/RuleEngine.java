package com.oberasoftware.home.rules;

import com.oberasoftware.base.event.EventHandler;
import com.oberasoftware.base.event.EventSubscribe;
import com.oberasoftware.home.api.events.DeviceEvent;
import com.oberasoftware.home.api.exceptions.HomeAutomationException;
import com.oberasoftware.home.api.managers.DeviceManager;
import com.oberasoftware.home.rules.api.Action;
import com.oberasoftware.home.rules.api.Condition;
import com.oberasoftware.home.rules.api.Rule;
import com.oberasoftware.home.rules.evaluators.EvalException;
import com.oberasoftware.home.rules.evaluators.EvaluatorFactory;
import com.oberasoftware.home.rules.evaluators.actions.ActionEvaluator;
import com.oberasoftware.home.rules.evaluators.conditions.ConditionEvaluator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Renze de Vries
 */
@Component
public class RuleEngine implements EventHandler {
    private static final Logger LOG = getLogger(RuleEngine.class);

    @Autowired
    private EvaluatorFactory evaluatorFactory;

    @Autowired
    private DeviceManager deviceManager;

    private List<Rule> rules = new CopyOnWriteArrayList<>();

    private Map<String, List<Rule>> itemMappedRules = new HashMap<>();

    public void evaluateRules(String itemId, TriggerSource triggerSource) {
        LOG.debug("Evaluating: {} rules for item: {} source: {}", itemId, triggerSource, rules.size());

//        List<Rule> itemRules = itemMappedRules.get(itemId);
//        itemRules.forEach(this::eval);
        rules.forEach(this::eval);
    }

    public void addRule(Rule rule) throws HomeAutomationException {
        checkNotNull(rule);

        LOG.info("Registering rule: {}", rule);

        Optional<ConditionEvaluator> evaluator = evaluatorFactory.getEvaluator(rule.getCondition());
        if(evaluator.isPresent()) {
//            Set<String> dependentItems = evaluator.get().getDependentItems(rule.getCondition());
//            dependentItems.forEach(i -> addDependentItem(rule, i));

            this.rules.add(rule);
        } else {
            throw new HomeAutomationException("Unable to register rule, no evaluator could be found to support the rule syntax");
        }
    }

    private void addDependentItem(Rule rule, String itemId) {
        itemMappedRules.putIfAbsent(itemId, new ArrayList<>());
        itemMappedRules.get(itemId).add(rule);
    }

    public void evalRule(String name) {
        checkNotNull(name);

        Optional<Rule> optionalRule = rules.stream().filter(r -> r.getName().equals(name)).findFirst();
        if(optionalRule.isPresent()) {
            eval(optionalRule.get());
        }
    }

    private void eval(Rule rule) {
        LOG.debug("Evaluating rule: {}", rule);
        Condition condition = rule.getCondition();
        Action action = rule.getAction();

        Optional<ConditionEvaluator> conditionEvaluator = evaluatorFactory.getEvaluator(condition);
        Optional<ActionEvaluator> actionEvaluator = evaluatorFactory.getEvaluator(action);

        if(conditionEvaluator.isPresent() && actionEvaluator.isPresent()) {
            ConditionEvaluator<Condition> e = conditionEvaluator.get();
            try {
                boolean eval = e.eval(condition);

                if (eval) {
                    ActionEvaluator<Action> a = actionEvaluator.get();
                    boolean actionEval = a.eval(action);

                    LOG.debug("Rule: {} was evaluated to: {} action: {} was run: {}", rule, eval, action, actionEval);
                }
            } catch(EvalException ex) {
                LOG.debug("Rule could not be evaluated: {}", ex.getMessage());
            }
        } else {
            LOG.error("Could not evaluate rule: {} condition: {} cannot be evaluated as it is not supported", rule.getName(), condition);
        }
    }

    @EventSubscribe
    public void receive(DeviceEvent event) throws Exception {
        LOG.debug("Received a device event: {}", event);
        evaluateRules(deviceManager.findDeviceItem(event.getControllerId(), event.getPluginId(), event.getDeviceId()).get().getId(),
                TriggerSource.DEVICE_EVENT);
    }

//    @EventSubscribe
//    public void receive(StateUpdateEvent event) throws Exception {
//        LOG.debug("Received a state update event: {}", event);
//        evaluateRules(event.getItemId(), TriggerSource.STATE_UPDATE);
//    }

    enum TriggerSource {
        STATE_UPDATE,
        DEVICE_EVENT
    }
}
