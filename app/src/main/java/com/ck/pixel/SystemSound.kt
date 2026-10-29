package com.ck.pixel

import android.content.Intent
import android.os.Build
import android.provider.Settings

fun systemSoundIntent(): Intent {
    val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Settings.Panel.ACTION_VOLUME
    } else {
        Settings.ACTION_SOUND_SETTINGS
    }
    return Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
