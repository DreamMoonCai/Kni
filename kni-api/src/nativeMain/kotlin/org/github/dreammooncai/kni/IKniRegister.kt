package org.github.dreammooncai.kni

import org.github.dreammooncai.kni.factory.KniDslMarker

interface IKniRegister {
    @KniDslMarker
    fun KniRegister.onRegister() {}
}