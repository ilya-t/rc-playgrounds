package com.testspace

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.TimestampAdjuster
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.UdpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.extractor.ts.TsExtractor.MODE_SINGLE_PMT
import androidx.media3.ui.PlayerView
import com.testspace.core.ExperimentActivity


class MainActivity : ExperimentActivity() {
    override fun createExperiment() = CurrentExperiment(this)

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the PlayerView
        val playerView: PlayerView = findViewById(R.id.player_view)
//
//        // Initialize ExoPlayer
//        val player: ExoPlayer = ExoPlayer.Builder(this).build()
//        playerView.player = player
//
//        // Set up the media source for the UDP stream
//        val udpUri = "udp://192.168.1.50:12345" // Replace with your stream URI
//        val mediaItem = MediaItem.fromUri(udpUri)
//        player.setMediaItem(mediaItem)
//
//
//        // Prepare and play the stream
//        player.prepare()
//        player.play()
//////


        val renderersFactory =
            DefaultRenderersFactory(this).forceEnableMediaCodecAsynchronousQueueing()
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(30_000, 60_000, 3_000, 6_000)
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setLoadControl(loadControl)
            .build()
        playerView.setPlayer(player)

        val mediaItem = MediaItem.Builder()
            .setUri("udp://192.168.1.50:12345")
            .build()
        val mediaSource = ProgressiveMediaSource.Factory{UdpDataSource()}
            .createMediaSource(mediaItem)

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }
}
