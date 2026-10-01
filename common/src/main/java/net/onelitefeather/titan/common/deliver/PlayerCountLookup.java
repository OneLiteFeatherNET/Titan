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

/**
 * Reads player counts from CloudNet. Implemented in the CloudNet bridge extension realm and
 * invoked from the application via {@link TitanPlayerCountLookup}. Only JDK types cross the
 * classloader boundary; {@code type} is {@code task}, {@code group} or {@code service}.
 */
public interface PlayerCountLookup {

    boolean supports(String type);

    /** {@code {online, max}}, or {@code null} when nothing of that name runs. */
    int[] lookup(String type, String name);
}
