package com.ck.pixel.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ck.pixel.R
import com.ck.pixel.systemSoundIntent

/**
 * Quick Settings tile that opens the system volume panel.
 */
class SoundManagerTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        unlockAndRun { openSystemSoundControls() }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.label = getString(R.string.qs_sound_manager_label)
        tile.state = Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_sound)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(R.string.qs_sound_manager_subtitle)
        }
        tile.updateTile()
    }

    private fun openSystemSoundControls() {
        val intent = systemSoundIntent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_OPEN_VOLUME,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private companion object {
        const val REQUEST_OPEN_VOLUME = 1
    }
}
