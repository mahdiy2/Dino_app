package com.dino.message.corefeature.data.resource

import android.content.Context
import com.dino.message.corefeature.domain.resource.StringResolver

class AndroidStringResolver(
    private val context: Context,
) : StringResolver {
    override fun resolveStringId(id: Int): String = context.getString(id)

    override fun resolveStringId(id: Int, vararg args: Any): String = context.getString(id, *args)
}
