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
package net.onelitefeather.titan.app.bootstrap.reload;

/**
 * A cheap-to-compare snapshot of one configuration file's state, as {@link ConfigFileWatcher} polls
 * it - the same idea avaje-config's own {@code FileWatch} uses (existence, last-modified time,
 * size), applied to Titan's own file list instead.
 *
 * @param exists       whether the file exists at all
 * @param lastModified the file's last-modified time in epoch millis; {@code 0} when {@code !exists}
 * @param size         the file's size in bytes; {@code 0} when {@code !exists}
 */
public record FileStamp(boolean exists, long lastModified, long size) {

    /**
     * The stamp of a path that does not exist.
     */
    public static final FileStamp MISSING = new FileStamp(false, 0L, 0L);
}
