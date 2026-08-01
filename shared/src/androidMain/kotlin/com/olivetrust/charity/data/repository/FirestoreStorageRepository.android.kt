package com.olivetrust.charity.data.repository

import dev.gitlive.firebase.storage.Data

actual fun toFirebaseData(data: ByteArray): Data = Data(data)
