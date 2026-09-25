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

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The child process entry point {@code ConfigReloadPrecedenceTest} launches: touches the static
 * {@link Config} facade exactly the way {@link net.onelitefeather.titan.app.Titan}'s constructor
 * does (the same "before" state a running lobby would have live), optionally mutates one
 * configuration file, then builds a production {@link ConfigReloader} - the real
 * {@link AvajeConfigSnapshotSource}/{@link AvajeLiveConfig}, a {@link ModuleRestarter} that knows
 * no modules at all (this class prints resulting <em>values</em>, not module restart behaviour -
 * that is {@link ConfigReloaderTest}'s and {@code ModuleRestartTest}'s job) - and triggers exactly
 * one reload.
 *
 * <p>Mirrors {@code ConfigurationPrintMain}: the parent test process controls this one's working
 * directory, environment and system properties via {@link ProcessBuilder}, and asserts on what
 * this process printed to stdout, since it cannot reach into this JVM's memory.
 *
 * <p><b>No Minestom server.</b> Both of {@link ConfigReloader}'s executors are direct
 * ({@code Runnable::run}, i.e. this main thread) - there is no tick thread to keep unblocked here,
 * so {@link java.util.concurrent.CompletableFuture#join()} on the reload's own result is exactly
 * as safe as it is forbidden in a real server (see {@code ConfigReloadBootstrap}, which never does
 * this).
 *
 * <p><b>Arguments.</b> {@code args[0]} is either {@value #NO_MUTATION} (no file mutation before
 * reloading - covers a case such as "an env override alone, no file involved") or the path
 * (relative
 * to the working directory) of a file to write <em>before</em> the reload is triggered; if so,
 * {@code args[1]} is either {@value #DELETE_FILE} (delete that file instead of writing it - covers
 * "a key disappears because its file is gone") or the exact content to write to it. Every
 * remaining argument is a configuration key to print, in order, after the reload - {@code
 * Configuration.get(key, "<absent>")}, exactly {@code ConfigurationPrintMain}'s plain print mode.
 */
public final class ConfigReloadPrintMain {

    private static final String NO_MUTATION = "--none";
    private static final String DELETE_FILE = "--delete--";

    private ConfigReloadPrintMain() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (ExceptionInInitializerError error) {
            printCauseChain(error);
            System.exit(1);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void run(String[] args) throws IOException {
        // First touch of the static facade - same place Titan()'s own javadoc documents this
        // happening in production - so the values below are exactly what a running lobby would
        // already have live before anything in this process mutates a file.
        Config.asConfiguration();

        List<String> keys = applyMutation(args);

        ConfigReloader reloader = new ConfigReloader(
                new AvajeConfigSnapshotSource(), new AvajeLiveConfig(), new NoOpModuleRestarter(), Runnable::run, Runnable::run);
        ReloadResult result = reloader.reload().join();

        System.out.println("RESULT=" + result.getClass().getSimpleName());
        if (result instanceof ReloadResult.Failed failed) {
            System.out.println("FILE=" + failed.file());
            System.out.println("DETAIL=" + failed.detail());
        }

        Configuration configuration = Config.asConfiguration();
        for (String key : keys) {
            System.out.println(key + "=" + configuration.get(key, "<absent>"));
        }
    }

    private static List<String> applyMutation(String[] args) throws IOException {
        String mutationFile = args[0];
        if (NO_MUTATION.equals(mutationFile)) {
            return List.of(args).subList(1, args.length);
        }

        String content = args[1];
        Path target = Path.of(mutationFile);
        if (DELETE_FILE.equals(content)) {
            Files.deleteIfExists(target);
        } else {
            Files.writeString(target, content);
        }
        return List.of(args).subList(2, args.length);
    }

    /**
     * See {@code ConfigurationPrintMain#printCauseChain} - identical shape, own copy: this class
     * must not depend on {@code app/src/test/.../bootstrap/ConfigurationPrintMain}, a different
     * test source file in a different package that a different task may still be evolving.
     */
    private static void printCauseChain(Throwable throwable) {
        String message = throwable.getMessage();
        System.out.println("ERROR: " + (message != null ? message : throwable.toString()));
        for (Throwable cause = throwable.getCause(); cause != null; cause = cause.getCause()) {
            System.out.println("Caused by: " + cause.getMessage());
        }
    }

    /**
     * Knows no modules at all: {@link #moduleOrder()} is empty, so {@link ConfigReloader} never
     * calls {@link #restart(String)} for any affected id (see its own javadoc) - this class is
     * only interested in resulting configuration values, not module restart behaviour.
     */
    private static final class NoOpModuleRestarter implements ModuleRestarter {

        @Override
        public ModuleRestartOutcome restart(String moduleId) {
            return new ModuleRestartOutcome.Restarted();
        }

        @Override
        public List<String> moduleOrder() {
            return List.of();
        }
    }
}
