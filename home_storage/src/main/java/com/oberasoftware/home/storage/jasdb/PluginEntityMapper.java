package com.oberasoftware.home.storage.jasdb;

import com.oberasoftware.home.api.storage.model.DevicePlugin;
import nl.renarj.jasdb.api.EmbeddedEntity;
import nl.renarj.jasdb.api.SimpleEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author renarj
 */
public class PluginEntityMapper implements EntityMapper<DevicePlugin> {
    @Override
    public SimpleEntity mapFrom(DevicePlugin plugin) {
        SimpleEntity pluginEntity = new SimpleEntity(plugin.getId());
        pluginEntity.addProperty("type", "plugin");
        pluginEntity.addProperty("pluginId", plugin.getPluginId());
        pluginEntity.addProperty("name", plugin.getName());
        pluginEntity.addProperty("controllerId", plugin.getControllerId());
        plugin.getDeviceIds().forEach(d -> pluginEntity.addProperty("devices", d));

        EmbeddedEntity deviceProperties = new EmbeddedEntity();
        plugin.getProperties().forEach(deviceProperties::addProperty);
        pluginEntity.addEntity("pluginProperties", deviceProperties);

        return pluginEntity;
    }

    @Override
    public DevicePlugin mapTo(SimpleEntity entity) {
        String controllerId = entity.getValue("controllerId");
        String pluginId = entity.getValue("pluginId");
        String name = entity.getValue("name");

        Map<String, String> properties = new HashMap<>();
        SimpleEntity pluginProperties = entity.getEntity("pluginProperties");
        pluginProperties.getProperties().forEach(p -> properties.put(p.getPropertyName(), p.getFirstValueObject()));

        DevicePlugin plugin = new DevicePlugin(entity.getInternalId(), controllerId, pluginId, name, properties);
        if(entity.hasProperty("devices")) {
            entity.getProperty("devices").getValues().forEach(v -> plugin.addDevice(v.toString()));
        }

        return plugin;
    }
}
