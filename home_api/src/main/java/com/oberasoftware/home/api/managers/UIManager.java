package com.oberasoftware.home.api.managers;

import com.oberasoftware.home.api.storage.model.Container;
import com.oberasoftware.home.api.storage.model.UIItem;

import java.util.List;

/**
 * @author renarj
 */
public interface UIManager {
    List<Container> getRootContainers();

    List<Container> getAllContainers();

    List<Container> getChildren(String containerId);

    Container getContainer(String containerId);

    List<UIItem> getItems(String containerId);

    void deleteContainer(String containerId);

    void delete(String itemId);

    UIItem store(UIItem item);

    Container store(Container container);
}
