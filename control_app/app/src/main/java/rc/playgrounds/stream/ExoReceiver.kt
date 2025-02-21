package rc.playgrounds.stream

import android.app.Activity
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.TimestampAdjuster
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.UdpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.PlayerView

@UnstableApi
class ExoReceiver(
    private val activity: Activity,
    private val playerView: PlayerView,
    private val uri: Uri,
) : StreamReceiver {
    //Create a default TrackSelector
    private val trackSelector = DefaultTrackSelector(activity, AdaptiveTrackSelection.Factory())


    //Create the player
    private val player: ExoPlayer = ExoPlayer.Builder(activity)
//        .setTrackSelector(trackSelector)

        .build()

    override fun play() {
        playerView.player = player
        playerView.requestFocus()

        val factory: DataSource.Factory = DataSource.Factory {
            UdpDataSource(1000, 65536)  // Lower buffer size and timeout
        }

        val tsExtractorFactory = ExtractorsFactory {
            arrayOf(
                TsExtractor(TsExtractor.MODE_HLS, TimestampAdjuster(0), DefaultTsPayloadReaderFactory())
            )
        }

        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(factory, tsExtractorFactory)
            .createMediaSource(MediaItem.fromUri(uri))

        player.setMediaSource(mediaSource)
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)  // Faster seeking
        player.setForegroundMode(true)  // Prioritize playback over background tasks
        player.setPlaybackSpeed(1.05f)  // Slight speed boost to reduce drift
        player.playWhenReady = true
        player.prepare()
        player.play()
    }

    override fun release() {
        player.release()
    }
}