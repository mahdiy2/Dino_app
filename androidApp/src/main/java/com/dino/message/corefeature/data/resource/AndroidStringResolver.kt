package com.dino.message.corefeature.data.resource

import android.content.Context
import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.platform.resource.toAndroidStringResId

class AndroidStringResolver(
    private val context: Context,
) : StringResolver {
    override fun resolve(key: AppStringKey, vararg args: Any): String =
        context.getString(key.toAndroidStringResId(), *args)
}
