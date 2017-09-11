/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.cluster.coordinator.rdbms.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy;
import org.wso2.carbon.cluster.coordinator.commons.internal.ClusterCoordinationServiceDataHolder;
import org.wso2.carbon.cluster.coordinator.rdbms.RDBMSCoordinationStrategy;
import org.wso2.carbon.cluster.coordinator.rdbms.exception.RDBMSClusterCoordinatorConfiurationException;
import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.kernel.configprovider.CarbonConfigurationException;
import org.wso2.carbon.kernel.configprovider.ConfigProvider;

import java.util.Map;

/**
 * RDBMS cluster coordinator data service.
 */
@Component(
        name = "org.wso2.carbon.cluster.coordinator.rdbms.internal.RDBMSCoordinationServiceComponent",
        immediate = true
)
public class RDBMSCoordinationServiceComponent {

    private static final Log log = LogFactory.getLog(RDBMSCoordinationServiceComponent.class);

    /**
     * This is the activation method of RDBMSCoordinationServiceComponent. This will be called when its references are
     * satisfied.
     *
     * @param bundleContext the bundle context instance of this bundle.
     */
    @Activate
    protected void start(BundleContext bundleContext) {

        Map<String, Object> clusterConfiguration = null;
        try {
            clusterConfiguration = ClusterCoordinationServiceDataHolder.getConfigProvider().
                    getConfigurationMap("ha.config");
            ClusterCoordinationServiceDataHolder.setClusterConfiguration(clusterConfiguration);
        } catch (CarbonConfigurationException e) {
            throw new RDBMSClusterCoordinatorConfiurationException("Configurations for RDBMS based HA deployment" +
                    " is not available in deployment.yaml");
        }
        if ((boolean) clusterConfiguration.get("enabled") &&
                clusterConfiguration.get("coordination.strategy.class").equals("RDBMSCoordinationStrategy")) {
            bundleContext.registerService(CoordinationStrategy.class, new RDBMSCoordinationStrategy(), null);
            log.info("RDBMS Coordination Service Component Activated");
        } else {
            throw new RDBMSClusterCoordinatorConfiurationException("HA deployment using RDBMSCoordinationStrategy " +
                    "has been disabled. Enable it in deployment.yaml or remove" +
                    " org.wso2.carbon.cluster.coordinator.rdbms.jar from the lib folder");
        }
    }

    /**
     * This is the deactivation method of RDBMSCoordinationServiceComponent. This will be called when this component
     * is being stopped or references are satisfied during runtime.
     */
    @Deactivate
    protected void stop() {
        ClusterCoordinationServiceDataHolder.setClusterConfiguration(null);
    }

    @Reference(
            name = "carbon.config.provider",
            service = ConfigProvider.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterConfigProvider"
    )
    protected void registerConfigProvider(ConfigProvider configProvider) throws CarbonConfigurationException {
        ClusterCoordinationServiceDataHolder.setConfigProvider(configProvider);
        String nodeId = (String) configProvider.getConfigurationMap("wso2.carbon").get("id");
        ClusterCoordinationServiceDataHolder.setNodeId(nodeId);
    }

    protected void unregisterConfigProvider(ConfigProvider configProvider) {
        ClusterCoordinationServiceDataHolder.setConfigProvider(null);
    }

    @Reference(
            name = "org.wso2.carbon.datasource.DataSourceService",
            service = DataSourceService.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterDataSourceListener"
    )
    protected void registerDataSourceListener(DataSourceService dataSourceService) {
        RDBMSClusterCoordinatorServiceHolder.setDataSourceService(dataSourceService);
    }

    protected void unregisterDataSourceListener(DataSourceService dataSourceService) {
        RDBMSClusterCoordinatorServiceHolder.setDataSourceService(null);
    }
}
