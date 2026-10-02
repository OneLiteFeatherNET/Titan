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

/** A {@link SpaceProbe} that counts how often it is asked, so a test can bound the work done. */
final class CountingProbe implements SpaceProbe {

    private final SpaceProbe delegate;
    private long calls;

    CountingProbe(SpaceProbe delegate) {
        this.delegate = delegate;
    }

    /** Questions asked so far, of either kind. */
    long calls() {
        return calls;
    }

    @Override
    public boolean isAir(BlockPos pos) {
        calls++;
        return delegate.isAir(pos);
    }

    @Override
    public boolean inBounds(BlockPos pos) {
        calls++;
        return delegate.inBounds(pos);
    }
}
