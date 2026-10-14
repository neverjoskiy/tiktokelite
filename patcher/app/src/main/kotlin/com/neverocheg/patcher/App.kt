package com.neverocheg.patcher

import android.app.Application
import com.neverocheg.patcher.work.News
import com.neverocheg.patcher.work.Notify

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val words = Strings.of(this)
        Notify.channels(this, words.workChannel, words.newsChannel)
        News.schedule(this)
    }
}
