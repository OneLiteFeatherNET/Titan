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
package net.onelitefeather.titan.buildsrc.architecture;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that a column's {@code package-info.java} describes the column: a Javadoc block sits
 * before the package declaration and is not just the license header. Deliberately coarse, as
 * design D8 says; the content stays a review question. A pure function over the file's text.
 */
public final class PackageDescription {

    private static final Pattern PACKAGE_DECLARATION = Pattern.compile("(?m)^package\\s");
    private static final Pattern JAVADOC_BLOCK = Pattern.compile("/\\*\\*(.*?)\\*/", Pattern.DOTALL);
    private static final Pattern LICENSE_HEADER = Pattern.compile("Copyright|Licensed under");

    private PackageDescription() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static boolean hasDescription(String packageInfoSource) {
        Matcher packageDeclaration = PACKAGE_DECLARATION.matcher(packageInfoSource);
        if (!packageDeclaration.find()) {
            return false;
        }
        Matcher block = JAVADOC_BLOCK.matcher(packageInfoSource.substring(0, packageDeclaration.start()));
        while (block.find()) {
            if (!LICENSE_HEADER.matcher(block.group(1)).find()) {
                return true;
            }
        }
        return false;
    }
}
