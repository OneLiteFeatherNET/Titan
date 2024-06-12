package net.onelitefeather.titan.featureflag

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

class FeatureService {

    private val togglePropertiesPath = Paths.get("flags.properties")

    private val flags: Properties = Properties()

    init {
        if (Files.exists(togglePropertiesPath)) {
            flags.load(Files.newBufferedReader(togglePropertiesPath))
        }
    }

    fun isFeatureEnabled(feature: Feature): Boolean {
        return flags.containsKey(feature.name)
    }

}