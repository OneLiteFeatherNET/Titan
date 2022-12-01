package net.onelitefeather.titan.service

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import net.onelitefeather.titan.config.WorldConfig
import java.nio.file.Path




fun loadConfig(configPath: Path): WorldConfig? {

    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory::class.java).build()
    val jsonAdapter = moshi.adapter(WorldConfig::class.java)
    return jsonAdapter.fromJson(configPath.toFile().readText())
}