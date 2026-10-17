package app.brainbox

import android.app.Application
import app.brainbox.data.player.PlayerRepository

/** Un seul PlayerRepository pour toute l'app (même verrou de synchronisation partout). */
class BrainBoxApplication : Application() {
    val playerRepository by lazy { PlayerRepository(this) }
}
