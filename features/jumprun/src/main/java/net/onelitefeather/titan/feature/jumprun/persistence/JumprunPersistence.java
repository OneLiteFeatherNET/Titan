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
package net.onelitefeather.titan.feature.jumprun.persistence;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import java.util.List;
import net.onelitefeather.titan.persistence.PersistenceUnit;

/**
 * What the column hands to the runtime: the shared database's table.
 */
@Factory
final class JumprunPersistence {

    @Bean
    PersistenceUnit jumprunUnit() {
        return new PersistenceUnit("jumprun", List.of(JumprunRunEntity.class));
    }
}
