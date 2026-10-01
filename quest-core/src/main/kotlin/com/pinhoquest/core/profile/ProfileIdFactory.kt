package com.pinhoquest.core.profile

import com.pinhoquest.domain.profile.ProfileId
import java.util.UUID

fun interface ProfileIdFactory {
    fun newId(): ProfileId
}

object RandomProfileIdFactory : ProfileIdFactory {
    override fun newId(): ProfileId = ProfileId(UUID.randomUUID().toString())
}
