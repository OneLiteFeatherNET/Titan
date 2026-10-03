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
package net.onelitefeather.titan.feature.jumprun;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

/** The lines one class logs while it is open; each test owns its own, so none sees another's. */
final class CapturedLog implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

    CapturedLog(Class<?> source) {
        this.logger = (Logger) LoggerFactory.getLogger(source);
        this.lines.start();
        this.logger.addAppender(this.lines);
    }

    List<ILoggingEvent> warnings() {
        return this.lines.list.stream().filter(line -> line.getLevel() == Level.WARN).toList();
    }

    /** The value of the key-value pair {@code key} on {@code line}, or {@code null}. */
    static Object valueOf(ILoggingEvent line, String key) {
        return line.getKeyValuePairs() == null ? null : line.getKeyValuePairs().stream().filter(pair -> pair.key.equals(key)).map((KeyValuePair pair) -> pair.value).findFirst().orElse(null);
    }

    /** The exception {@code line} carries. */
    static Throwable causeOf(ILoggingEvent line) {
        IThrowableProxy proxy = line.getThrowableProxy();
        if (!(proxy instanceof ThrowableProxy thrown)) {
            throw new AssertionError("the line carries no exception: " + line.getFormattedMessage());
        }
        return thrown.getThrowable();
    }

    @Override
    public void close() {
        this.logger.detachAppender(this.lines);
        this.lines.stop();
    }
}
