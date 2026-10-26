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
package net.onelitefeather.titan.common.deliver;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross-classloader bridge for starting and stopping CloudNet services. The CloudNet provider runs
 * in the bridge extension's own classloader, so this holder lives on the shared application
 * classloader and exchanges only JDK types. Until the bridge installs a {@link ServiceControl},
 * nothing can be read and every request is refused.
 */
public final class TitanServiceControl {

    private static final Logger LOG = LoggerFactory.getLogger(TitanServiceControl.class);

    private static volatile ServiceControl control;

    private TitanServiceControl() {
    }

    public static void setControl(ServiceControl serviceControl) {
        control = serviceControl;
    }

    public static boolean isInstalled() {
        return control != null;
    }

    /** The rows of the task's services, see {@link ServiceControl}; empty when unreadable. */
    public static Optional<List<Map<String, Object>>> servicesByTask(String task) {
        ServiceControl current = control;
        if (current == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(current.servicesByTask(task));
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Services of task {} cannot be read from CloudNet", task, e);
            return Optional.empty();
        }
    }

    public static boolean startTask(String task) {
        ServiceControl current = control;
        if (current == null) {
            return false;
        }
        try {
            return current.start(task);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Service of task {} cannot be started through CloudNet", task, e);
            return false;
        }
    }

    public static boolean stopService(String serviceName) {
        ServiceControl current = control;
        if (current == null) {
            return false;
        }
        try {
            return current.stop(serviceName);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Service {} cannot be stopped through CloudNet", serviceName, e);
            return false;
        }
    }
}
