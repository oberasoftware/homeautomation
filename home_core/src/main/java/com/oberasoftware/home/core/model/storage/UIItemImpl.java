package com.oberasoftware.home.core.model.storage;

import com.oberasoftware.home.api.model.storage.UIItem;
import com.oberasoftware.jasdb.api.entitymapper.annotations.Id;
import com.oberasoftware.jasdb.api.entitymapper.annotations.JasDBEntity;
import com.oberasoftware.jasdb.api.entitymapper.annotations.JasDBProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author renarj
 */
@JasDBEntity(bagName = "widgets")
public class UIItemImpl implements UIItem {
    private String id;
    private String name;
    private String uiType;

    private String containerId;

    private String itemId;

    private long weight = 0;

    private Map<String, String> properties = new HashMap<>();

    public UIItemImpl(String id, String name, String containerId, String uiType, String itemId, Map<String, String> properties, long weight) {
        this.id = id;
        this.name = name;
        this.containerId = containerId;
        this.uiType = uiType;
        this.itemId = itemId;
        this.properties = properties;
        this.weight = weight;
    }

    public UIItemImpl() {
    }

    @Override
    @Id
    @JasDBProperty
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    @JasDBProperty
    public long getWeight() {
        return weight;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }

    @Override
    @JasDBProperty
    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    @Override
    @JasDBProperty
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JasDBProperty
    public String getUiType() {
        return uiType;
    }

    public void setUiType(String uiType) {
        this.uiType = uiType;
    }

    @Override
    @JasDBProperty
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    @Override
    @JasDBProperty
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void addProperty(String property, String value) {
        this.properties.put(property, value);
    }

    @Override
    public String toString() {
        return "UIItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", uiType='" + uiType + '\'' +
                ", containerId='" + containerId + '\'' +
                ", itemId='" + itemId + '\'' +
                '}';
    }
}
