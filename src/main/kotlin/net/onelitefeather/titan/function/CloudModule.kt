package net.onelitefeather.titan.function

import com.google.inject.AbstractModule
import com.google.inject.Provides
import net.onelitefeather.titan.deliver.CloudNetDeliver
import net.onelitefeather.titan.deliver.Deliver
import net.onelitefeather.titan.deliver.DummyDeliver

class CloudModule: AbstractModule() {

    @Provides
    fun cloudSupport(): Deliver {
        try {
            Class.forName("eu.cloudnetservice.wrapper.Main")
            return CloudNetDeliver()
        } catch (e: ClassNotFoundException) {
            return DummyDeliver()
        }
    }
}