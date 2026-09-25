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
 * A {@link ConfigSnapshotSource} could not build a fresh configuration snapshot - e.g. a
 * syntactically
 * broken {@code application.yaml}. Carries the file and the location within it (line/column or an
 * equivalent human-readable position) a broken load failed at, so a {@link ConfigReloader} can
 * report
 * {@link ReloadResult.Failed} without applying anything.
 *
 * <p>Not a record: {@code java.lang.Record} cannot extend {@link RuntimeException}. The production
 * adapter (wired in a later wave) is expected to build one of these from the same
 * {@code ExceptionInInitializerError} cause chain {@code ConfigurationPrintMain#printCauseChain}
 * already walks for a broken load - see
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1.
 */
public final class ConfigSnapshotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String file;
    private final String detail;

    public ConfigSnapshotException(String file, String detail) {
        this(file, detail, null);
    }

    public ConfigSnapshotException(String file, String detail, Throwable cause) {
        super(file + ": " + detail, cause);
        this.file = file;
        this.detail = detail;
    }

    /**
     * @return the configuration file the broken load was found in, e.g. {@code application.yaml}
     */
    public String file() {
        return file;
    }

    /**
     * @return a human-readable location of the problem within {@link #file()}, e.g. a parser's
     *         line/column message
     */
    public String detail() {
        return detail;
    }
}
