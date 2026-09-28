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
package net.onelitefeather.titan.runtime.variant;

import io.avaje.inject.spi.AvajeModule;
import io.avaje.inject.spi.InjectExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The columns actually on {@code loader}, discovered the same built-in way Avaje itself finds its
 * modules: {@link ServiceLoader#load(Class, ClassLoader)} over {@link InjectExtension}. A column's
 * generated Avaje module is named {@code <Name>ColumnModule} from its {@code @InjectModule(name =
 * "<name>Column", ...)} (see docs/lobby-modules.md, "Wie eine Column Plattform-Beans bekommt");
 * this
 * strips the {@value #SUFFIX} suffix and lowercases the first letter to recover {@code <name>}.
 */
public final class LoadedColumns {

    private static final String SUFFIX = "ColumnModule";

    private LoadedColumns() {
    }

    public static List<String> discover(ClassLoader loader) {
        List<String> names = new ArrayList<>();
        for (InjectExtension extension : ServiceLoader.load(InjectExtension.class, loader)) {
            if (extension instanceof AvajeModule) {
                String simpleName = extension.getClass().getSimpleName();
                if (simpleName.endsWith(SUFFIX)) {
                    String columnName = simpleName.substring(0, simpleName.length() - SUFFIX.length());
                    names.add(Character.toLowerCase(columnName.charAt(0)) + columnName.substring(1));
                }
            }
        }
        return List.copyOf(names);
    }
}
