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
package net.onelitefeather.titan.buildsrc.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

/**
 * The short HEAD commit of the checkout, or absent when git is missing or the directory is not a
 * checkout (a source tarball). A ValueSource rather than providers.exec, because exec fails the
 * build when the git binary cannot even be started.
 */
public abstract class GitCommitValueSource implements ValueSource<String, GitCommitValueSource.Parameters> {

    public interface Parameters extends ValueSourceParameters {
        DirectoryProperty getWorkingDirectory();
    }

    @Override
    public String obtain() {
        try {
            Process process = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                    .directory(getParameters().getWorkingDirectory().get().getAsFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            String commit = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return process.waitFor() == 0 && !commit.isEmpty() ? commit : null;
        } catch (IOException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
