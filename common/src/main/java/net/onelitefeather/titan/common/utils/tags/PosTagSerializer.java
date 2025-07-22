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
package net.onelitefeather.titan.common.utils.tags;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PosTagSerializer implements TagSerializer<Point> {

    private static final Tag<Double> X = Tag.Double("X");
    private static final Tag<Double> Y = Tag.Double("Y");
    private static final Tag<Double> Z = Tag.Double("Z");
    private static final Tag<Float> YAW = Tag.Float("Yaw");
    private static final Tag<Float> PITCH = Tag.Float("Pitch");

    @Override
    public @Nullable Point read(@NotNull TagReadable reader) {
        if (reader.hasTag(X) && reader.hasTag(Y) && reader.hasTag(Z)) {
            double x = reader.getTag(X);
            double y = reader.getTag(Y);
            double z = reader.getTag(Z);
            if (reader.hasTag(YAW) && reader.hasTag(PITCH)) {
                float yaw = reader.getTag(YAW);
                float pitch = reader.getTag(PITCH);
                return new Pos(x, y, z, yaw, pitch);
            }
            return new Pos(x, y, z);
        }
        return Pos.ZERO;
    }

    @Override
    public void write(@NotNull TagWritable writer, @NotNull Point value) {
        writer.setTag(X, value.x());
        writer.setTag(Y, value.y());
        writer.setTag(Z, value.z());
        if (value instanceof Pos) {
            writer.setTag(YAW, ((Pos) value).yaw());
            writer.setTag(PITCH, ((Pos) value).pitch());
        }
    }
}
