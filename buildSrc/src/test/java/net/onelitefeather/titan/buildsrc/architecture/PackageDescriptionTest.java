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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Plain-Java unit coverage for {@link PackageDescription}: no file system needed (F.I.R.S.T.). */
class PackageDescriptionTest {

    private static final String LICENSE = """
            /**
             * Copyright 2025 OneLiteFeather Network
             *
             * Licensed under the Apache License, Version 2.0 (the "License");
             */
            """;

    @DisplayName("A package-info with a Javadoc description before the package is accepted")
    @Test
    void aJavadocDescriptionBeforeThePackageIsAccepted() {
        String source = LICENSE + """
                /**
                 * The {@code sit} column.
                 */
                @InjectModule(name = "sitColumn")
                package net.onelitefeather.titan.feature.sit;
                """;

        Assertions.assertTrue(PackageDescription.hasDescription(source), "a Javadoc description should be accepted");
    }

    @DisplayName("A package-info with only the license header is rejected")
    @Test
    void aLicenseHeaderIsNotADescription() {
        String source = LICENSE + "package net.onelitefeather.titan.feature.sit;\n";

        Assertions.assertFalse(PackageDescription.hasDescription(source), "a license header alone is not a description");
    }

    @DisplayName("A package-info with only a line comment is rejected")
    @Test
    void aLineCommentIsNotADescription() {
        String source = LICENSE + """
                // The sit column.
                package net.onelitefeather.titan.feature.sit;
                """;

        Assertions.assertFalse(PackageDescription.hasDescription(source), "a // comment is not a Javadoc description");
    }

    @DisplayName("A package-info without a package declaration is rejected")
    @Test
    void aFileWithoutPackageDeclarationIsRejected() {
        Assertions.assertFalse(PackageDescription.hasDescription("/** A description. */\n"), "no package, no description");
    }
}
