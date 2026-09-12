package com.dino.message.corefeature.domain.resource

interface StringResolver {
    fun resolve(value: String): String = value
    fun resolveStringId(id: Int): String
    fun resolveStringId(id: Int, vararg args: Any): String
}
