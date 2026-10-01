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
package net.onelitefeather.titan.core.portal;

import org.jetbrains.annotations.Nullable;

/**
 * Where a label takes its player count from. Provider-neutral: the system that supplies the
 * numbers maps task, group and service to its own concepts. Plain data, checked by
 * {@link PortalValidator}.
 */
public sealed interface LabelSource {

    /** Sum over all servers of one task. */
    record Task(@Nullable String name) implements LabelSource {
    }

    /** Sum over all servers of one group. */
    record Group(@Nullable String name) implements LabelSource {
    }

    /** Exactly one named server instance. */
    record Service(@Nullable String name) implements LabelSource {
    }

    /** Only the players connected to this lobby. */
    record Local() implements LabelSource {
    }

    /** A {@code type} outside the fixed vocabulary, kept so the validator can name it. */
    record Unknown(@Nullable String type) implements LabelSource {
    }
}
