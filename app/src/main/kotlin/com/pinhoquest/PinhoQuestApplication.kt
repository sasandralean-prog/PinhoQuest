package com.pinhoquest

import android.app.Application

class PinhoQuestApplication : Application() {
    val graph: PinhoQuestAppGraph by lazy {
        PinhoQuestAppGraph(this)
    }
}
