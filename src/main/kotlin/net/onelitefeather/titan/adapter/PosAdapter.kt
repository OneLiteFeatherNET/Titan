package net.onelitefeather.titan.adapter

import com.squareup.moshi.*
import net.minestom.server.coordinate.Pos

class PosAdapter : JsonAdapter<Pos>() {
    @ToJson
    override fun toJson(writer: JsonWriter, value: Pos?) {
        value?: return
        writer.use {
            it.isLenient = true
            it.beginObject()
            it.value(value.x())
            it.value(value.y())
            it.value(value.z())
            it.value(value.yaw())
            it.value(value.pitch())
            it.endObject()
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): Pos {
        reader.use {
            it.beginObject()
            val x = it.nextDouble()
            val y = it.nextDouble()
            val z = it.nextDouble()
            val yaw = it.nextDouble()
            val pitch = it.nextDouble()
            it.endObject()
            return Pos(x, y, z, yaw.toFloat(), pitch.toFloat())
        }
    }

}