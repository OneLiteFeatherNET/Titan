package net.onelitefeather.titan.function

import net.minestom.server.entity.Player
import org.togglz.core.user.FeatureUser
import org.togglz.core.user.SimpleFeatureUser

interface TitanFunction {

    fun initialize()
    fun terminate()

    fun Player.toFeatureUser(): FeatureUser {
        return SimpleFeatureUser(this.username)
    }
}