package com.oberasoftware.home.storage.jasdb;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.oberasoftware.home.api.model.storage.Container;
import com.oberasoftware.home.api.model.storage.ControllerItem;
import com.oberasoftware.home.api.model.storage.DeviceItem;
import com.oberasoftware.home.api.model.storage.GroupItem;
import com.oberasoftware.home.api.model.storage.Item;
import com.oberasoftware.home.api.model.storage.PluginItem;
import com.oberasoftware.home.api.model.storage.UIItem;
import com.oberasoftware.home.api.storage.HomeDAO;
import com.oberasoftware.home.core.model.storage.ContainerImpl;
import com.oberasoftware.home.core.model.storage.ControllerItemImpl;
import com.oberasoftware.home.core.model.storage.DeviceItemImpl;
import com.oberasoftware.home.core.model.storage.GroupItemImpl;
import com.oberasoftware.home.core.model.storage.PluginItemImpl;
import com.oberasoftware.home.core.model.storage.UIItemImpl;
import com.oberasoftware.jasdb.api.entitymapper.EntityManager;
import nl.renarj.jasdb.api.DBSession;
import nl.renarj.jasdb.api.query.QueryBuilder;
import nl.renarj.jasdb.core.exceptions.JasDBStorageException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
@Component
public class JasDBDAO implements HomeDAO {
    private static final Logger LOG = getLogger(JasDBDAO.class);

    @Autowired
    private JasDBSessionFactory sessionFactory;

    @Override
    public <T extends Item> Optional<T> findItem(Class<T> type, String id) {
        try {
            DBSession session = sessionFactory.createSession();
            EntityManager entityManager = session.getEntityManager();

            T result = entityManager.findEntity(type, id);
            return Optional.of(result);
        } catch(JasDBStorageException e) {
            LOG.error("Unable to load item", e);
            return empty();
        }
    }

    @Override
    public Optional<Container> findContainer(String id) {
        try {
            DBSession session = sessionFactory.createSession();
            EntityManager entityManager = session.getEntityManager();

            ContainerImpl container = entityManager.findEntity(ContainerImpl.class, id);
            return Optional.of(container);
        } catch(JasDBStorageException e) {
            LOG.error("Unable to load container", e);
            return empty();
        }
    }

    @Override
    public List<Container> findRootContainers() {
        return newArrayList(findItems(ContainerImpl.class, new ImmutableMap.Builder<String, String>()
                .put("parentContainerId", "").build()));
    }

    @Override
    public List<Container> findContainers() {
        return newArrayList(findItems(ContainerImpl.class, new ImmutableMap.Builder<String, String>()
                .build()));
    }

    @Override
    public List<Container> findContainers(String parentId) {
        return newArrayList(findItems(ContainerImpl.class, new ImmutableMap.Builder<String, String>()
                .put("parentContainerId", parentId).build()));
    }

    @Override
    public List<UIItem> findUIItems(String containerId) {
        return newArrayList(findItems(UIItemImpl.class, new ImmutableMap.Builder<String, String>()
                .put("containerId", containerId).build(), newArrayList("weight")));
    }

    @Override
    public Optional<ControllerItem> findController(String controllerId) {
        ControllerItemImpl controllerItem = findItem(ControllerItemImpl.class, new ImmutableMap.Builder<String, String>()
                .put("controllerId", controllerId).build());
        return ofNullable(controllerItem);
    }

    @Override
    public Optional<PluginItem> findPlugin(String controllerId, String pluginId) {
        PluginItemImpl pluginItem = findItem(PluginItemImpl.class, new ImmutableMap.Builder<String, String>()
                .put("controllerId", controllerId)
                .put("pluginId", pluginId).build());
        return ofNullable(pluginItem);
    }

    @Override
    public List<ControllerItem> findControllers() {
        return newArrayList(findItems(ControllerItemImpl.class, new ImmutableMap.Builder<String, String>().build()));
    }

    @Override
    public List<PluginItem> findPlugins(String controllerId) {
        List<PluginItemImpl> pluginItems = findItems(PluginItemImpl.class, new ImmutableMap.Builder<String, String>()
                .put("controllerId", controllerId).build());

        return pluginItems.stream().map(p -> (PluginItem) p).collect(Collectors.toList());
    }

    @Override
    public List<DeviceItem> findDevices(String controllerId, String pluginId) {
        return newArrayList(findItems(DeviceItemImpl.class, new ImmutableMap.Builder<String, String>()
                .put("controllerId", controllerId)
                .put("pluginId", pluginId).build()));
    }

    @Override
    public Optional<DeviceItem> findDevice(String controllerId, String pluginId, String deviceId) {
        DeviceItemImpl deviceItem = findItem(DeviceItemImpl.class, new ImmutableMap.Builder<String, String>()
                .put("controllerId", controllerId)
                .put("pluginId", pluginId)
                .put("deviceId", deviceId).build());
        return ofNullable(deviceItem);
    }

    @Override
    public List<DeviceItem> findDevices() {
        return newArrayList(findItems(DeviceItemImpl.class, new ImmutableMap.Builder<String, String>()
                .build()));
    }

    @Override
    public List<GroupItem> findGroups() {
        return newArrayList(findItems(GroupItemImpl.class, new HashMap<>()));
    }

    @Override
    public List<GroupItem> findGroups(String controllerId) {
        return newArrayList(findItems(GroupItemImpl.class, new ImmutableMap.Builder<String, String>()
                .put("controllerId", controllerId)
                .build()));
    }

    private <T> T findItem(Class<T> type, Map<String, String> properties) {
        List<T> items = findItems(type, properties);
        return Iterables.getFirst(items, null);
    }

    private <T> List<T> findItems(Class<T> type, Map<String, String> properties) {
        return findItems(type, properties, new ArrayList<>());
    }

    private <T> List<T> findItems(Class<T> type, Map<String, String> properties, List<String> orderedBy) {
        List<T> results = new ArrayList<>();

        try {
            DBSession session = sessionFactory.createSession();
            EntityManager entityManager = session.getEntityManager();

            QueryBuilder queryBuilder = QueryBuilder.createBuilder();
            properties.forEach((k, v) -> queryBuilder.field(k).value(v));
            orderedBy.forEach(queryBuilder::sortBy);

            return entityManager.findEntities(type, queryBuilder);
        } catch (JasDBStorageException e) {
            LOG.error("Unable to query JasDB", e);
        }
        return results;
    }
}
