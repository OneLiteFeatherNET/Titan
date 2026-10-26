/**
 * Copyright 2025 OneLiteFeather Network
 *
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
package net.onelitefeather.titan.bridge;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.onelitefeather.titan.common.deliver.ServiceControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CloudNet side of {@link ServiceControl}. Reads the services of a task synchronously, on the
 * navigator's read thread, like the player counts. Start and stop only send their requests through
 * the async API and return at once; a failure arrives later and is logged.
 */
final class CloudNetServiceControl implements ServiceControl {

    private static final Logger LOG = LoggerFactory.getLogger(CloudNetServiceControl.class);

    @Override
    public List<Map<String, Object>> servicesByTask(String task) {
        Collection<ServiceInfoSnapshot> services = provider().servicesByTask(task);
        return ServiceListing.withPhases(services.stream().map(CloudNetServiceControl::reading).toList());
    }

    @Override
    public boolean start(String task) {
        CloudServiceProvider provider = provider();
        ServiceTaskProvider tasks = InjectionLayer.ext().instance(ServiceTaskProvider.class);
        tasks.serviceTaskAsync(task).thenCompose(serviceTask -> createService(task, serviceTask)).thenCompose(result -> startCreated(provider, task, result)).whenComplete((ignored, failure) -> logFailure(failure, "start a service of task", task));
        return true;
    }

    @Override
    public boolean stop(String serviceName) {
        CloudServiceProvider provider = provider();
        provider.serviceByNameAsync(serviceName).thenCompose(service -> stopService(provider, service)).whenComplete((ignored, failure) -> logFailure(failure, "stop service", serviceName));
        return true;
    }

    /**
     * Prepares the service; a stopped one is deleted, since a prepared one would count as starting.
     */
    private static CompletableFuture<ServiceCreateResult> createService(String task, ServiceTask serviceTask) {
        if (serviceTask == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no task named " + task));
        }
        return ServiceConfiguration.builder(serviceTask).autoDeleteOnStop(true).build().createNewServiceAsync();
    }

    // Creating only prepares the service; it is started as a second step.
    private static CompletableFuture<Void> startCreated(CloudServiceProvider provider, String task, ServiceCreateResult result) {
        if (result == null || result.state() != ServiceCreateResult.State.CREATED) {
            return CompletableFuture.failedFuture(new IllegalStateException("creating a service of task " + task + " ended without a service"));
        }
        return provider.serviceProvider(result.serviceInfo().serviceId().uniqueId()).startAsync();
    }

    // A service that is already gone has nothing left to stop.
    private static CompletableFuture<Void> stopService(CloudServiceProvider provider, ServiceInfoSnapshot service) {
        if (service == null) {
            return CompletableFuture.completedFuture(null);
        }
        return provider.serviceProvider(service.serviceId().uniqueId()).stopAsync();
    }

    private static void logFailure(Throwable failure, String action, String target) {
        if (failure != null) {
            LOG.warn("CloudNet could not {} {}", action, target, failure);
        }
    }

    /** The reading of one service; a missing lifecycle or limit reads as starting and as zero. */
    static ServiceReading reading(ServiceInfoSnapshot service) {
        String lifeCycle = service.lifeCycle() == null ? null : service.lifeCycle().name();
        return new ServiceReading(service.name(), ServiceStateMapping.phase(lifeCycle), count(service.readProperty(BridgeDocProperties.ONLINE_COUNT)), count(service.readProperty(BridgeDocProperties.MAX_PLAYERS)));
    }

    private static int count(Integer value) {
        return value == null ? 0 : value;
    }

    // Resolved per call: CloudNet binds its provider in the extension layer, not in the registry.
    private static CloudServiceProvider provider() {
        return InjectionLayer.ext().instance(CloudServiceProvider.class);
    }
}
