package com.ck.pixel

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ck.pixel.assist.AssistiveDialService
import com.ck.pixel.tile.SoundManagerTileService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarPadding()

        findViewById<Button>(R.id.open_sound_controls).setOnClickListener {
            startActivity(systemSoundIntent())
        }

        val addTile = findViewById<Button>(R.id.add_sound_tile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addTile.setOnClickListener { requestSoundTile() }
        } else {
            addTile.isEnabled = false
        }

        findViewById<Button>(R.id.enable_assistive_dial).setOnClickListener {
            openAssistiveDialSettings()
        }
    }

    private fun applySystemBarPadding() {
        val content = findViewById<android.view.View>(R.id.content)
        val extra = (24 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                extra + bars.left,
                extra + bars.top,
                extra + bars.right,
                extra + bars.bottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    override fun onResume() {
        super.onResume()
        val status = findViewById<TextView>(R.id.assistive_status)
        status.setText(
            if (AssistiveDialService.isEnabled(this)) {
                R.string.assistive_dial_on
            } else {
                R.string.assistive_dial_off
            },
        )
    }

    private fun openAssistiveDialSettings() {
        val component = ComponentName(this, AssistiveDialService::class.java).flattenToString()
        val args = Bundle().apply { putString(EXTRA_FRAGMENT_ARG_KEY, component) }
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(EXTRA_FRAGMENT_ARG_KEY, component)
            putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args)
        }
        runCatching { startActivity(intent) }
            .onFailure { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    private fun requestSoundTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val statusBar = getSystemService(StatusBarManager::class.java) ?: return
        statusBar.requestAddTileService(
            ComponentName(this, SoundManagerTileService::class.java),
            getString(R.string.qs_sound_manager_label),
            Icon.createWithResource(this, R.drawable.ic_qs_sound),
            mainExecutor,
        ) { result ->
            val message = when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                    R.string.tile_added
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                    R.string.tile_already_added
                else -> R.string.tile_not_added
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
        const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"
    }
}
