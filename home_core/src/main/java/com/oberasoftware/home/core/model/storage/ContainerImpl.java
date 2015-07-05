package com.oberasoftware.home.core.model.storage;

import com.oberasoftware.home.api.model.storage.Container;
import com.oberasoftware.jasdb.api.entitymapper.annotations.Id;
import com.oberasoftware.jasdb.api.entitymapper.annotations.JasDBEntity;
import com.oberasoftware.jasdb.api.entitymapper.annotations.JasDBProperty;

/**
 * @author renarj
 */
@JasDBEntity(bagName = "containers")
public class ContainerImpl implements Container {

    private String id;
    private String name;

    private String parentContainerId;

    public ContainerImpl(String id, String name, String parentContainerId) {
        this.id = id;
        this.name = name;
        this.parentContainerId = parentContainerId;
    }

    public ContainerImpl() {
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
    public String getParentContainerId() {
        return parentContainerId;
    }

    public void setParentContainerId(String parentContainerId) {
        this.parentContainerId = parentContainerId;
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
    public String toString() {
        return "Container{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", parentContainerId='" + parentContainerId + '\'' +
                '}';
    }
}