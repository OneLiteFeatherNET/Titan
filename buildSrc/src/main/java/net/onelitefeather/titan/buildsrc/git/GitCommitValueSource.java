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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

/**
 * The short HEAD commit of the checkout, or absent when git is missing or the directory is not a
 * checkout (a source tarball). A ValueSource rather than providers.exec, because exec fails the
 * build when the git binary cannot even be started.
 */
public abstract class GitCommitValueSource implements ValueSource<String, GitCommitValueSource.Parameters> {

    private static final long TIMEOUT_SECONDS = 10;

    public interface Parameters extends ValueSourceParameters {
        DirectoryProperty getWorkingDirectory();
    }

    @Override
    public String obtain() {
        Path root = getParameters().getWorkingDirectory().get().getAsFile().toPath();
        // git walks up to a parent repository, so a source tree unpacked inside another checkout
        // would report that checkout's HEAD. Only a root with its own .git (a directory, or a file
        // in a worktree) names its own commit.
        if (!Files.exists(root.resolve(".git"))) {
            return null;
        }
        Process process = null;
        try {
            process = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                    .directory(root.toFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return null;
            }
            String commit = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return commit.isEmpty() ? null : commit;
        } catch (IOException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }
}
