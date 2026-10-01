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
package net.onelitefeather.titan.runtime.bootstrap;

import java.util.ArrayList;
import java.util.List;

/**
 * The Avaje Inject profiles the bean scope is built with: the configured {@code avaje.profiles}
 * plus {@value #CLOUDNET} when running as a CloudNet service. Profiles set on the builder replace
 * the configured ones, so those are carried over here.
 */
public final class BeanProfiles {

    /** Beans that only make sense as a CloudNet service. */
    public static final String CLOUDNET = "cloudnet";

    private BeanProfiles() {
    }

    public static String[] active(List<String> configured, boolean cloudNet) {
        List<String> profiles = new ArrayList<>(configured);
        if (cloudNet && !profiles.contains(CLOUDNET)) {
            profiles.add(CLOUDNET);
        }
        return profiles.toArray(String[]::new);
    }
}
