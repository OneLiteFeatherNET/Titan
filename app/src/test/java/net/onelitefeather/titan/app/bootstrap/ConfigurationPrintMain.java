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
package net.onelitefeather.titan.app.bootstrap;

import io.avaje.config.Configuration;

/**
 * The child process entry point {@link ConfigurationPrecedenceTest} launches: runs the exact same
 * migrate-then-load sequence production runs, through the very same {@link ConfigurationLoader}
 * {@code PlatformBeans} uses, then prints one {@code key=value} line per requested key to stdout -
 * {@code <absent>} if the key resolves to nothing at all - so the parent test process, which cannot
 * reach into this JVM's memory, can assert on what the real {@code avaje-config} pipeline resolved,
 * migration included.
 *
 * <p>Deliberately tiny: every argument is a configuration key to print, in order, nothing else.
 * {@link ConfigurationPrecedenceTest} controls this process's working directory, environment and
 * system properties via {@link ProcessBuilder} before launching it, so what this class prints is
 * exactly what a real lobby process would resolve under the same conditions.
 */
public final class ConfigurationPrintMain {

    private ConfigurationPrintMain() {
    }

    public static void main(String[] args) {
        Configuration configuration = new ConfigurationLoader().load();
        for (String key : args) {
            System.out.println(key + "=" + configuration.get(key, "<absent>"));
        }
    }
}
