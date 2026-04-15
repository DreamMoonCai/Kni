package org.github.dreammooncai.kni

import org.github.dreammooncai.util.DreamNativeLoader

object KniLoader {
    private var libName: String = "KDreamKniLib"

    fun init(libName: String) {
        this.libName = libName
    }

    fun loader() = DreamNativeLoader.loadLibrary(libName)
}