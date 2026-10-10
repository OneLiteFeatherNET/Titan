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
package net.onelitefeather.titan.platform.cloudnet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.onelitefeather.titan.common.deliver.DebugDeliver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CloudNetDeliverSelectionTest {

    @TempDir
    Path serviceDirectory;

    @DisplayName("A CloudNet service directory with a .wrapper folder selects the message channel deliver")
    @Test
    void wrapperDirectorySelectsMessageChannel() throws IOException {
        Files.createDirectory(this.serviceDirectory.resolve(".wrapper"));

        Assertions.assertInstanceOf(MessageChannelDeliver.class, CloudNetDeliverSelection.select(this.serviceDirectory));
    }

    @DisplayName("Without a .wrapper folder the debug deliver is selected")
    @Test
    void missingWrapperDirectorySelectsDebugDeliver() {
        Assertions.assertInstanceOf(DebugDeliver.class, CloudNetDeliverSelection.select(this.serviceDirectory));
    }
}
