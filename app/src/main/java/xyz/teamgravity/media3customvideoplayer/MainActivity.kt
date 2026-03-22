package xyz.teamgravity.media3customvideoplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import kotlinx.coroutines.delay
import xyz.teamgravity.media3customvideoplayer.ui.theme.Media3CustomVideoPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Media3CustomVideoPlayerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { padding ->
                    val context = LocalContext.current
                    val player = retain {
                        ExoPlayer.Builder(context.applicationContext).build()
                    }

                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia(),
                        onResult = { uri ->
                            if (uri == null) return@rememberLauncherForActivityResult
                            player.setMediaItem(MediaItem.fromUri(uri))
                            player.prepare()
                            player.play()
                        }
                    )

                    var isPlaying by retain { mutableStateOf(false) }
                    var currentPosition by retain { mutableLongStateOf(0L) }
                    var duration by retain { mutableLongStateOf(0L) }
                    var isSeeking by retain { mutableStateOf(false) }
                    var isBuffering by retain { mutableStateOf(false) }
                    var isPlayerVisible by retain { mutableStateOf(false) }

                    RetainedEffect(
                        key1 = player,
                        effect = {
                            val listener = object : Player.Listener {
                                override fun onIsPlayingChanged(playing: Boolean) {
                                    super.onIsPlayingChanged(playing)
                                    isPlaying = playing
                                }

                                override fun onPlaybackStateChanged(playbackState: Int) {
                                    super.onPlaybackStateChanged(playbackState)
                                    isBuffering = playbackState == Player.STATE_BUFFERING
                                    if (playbackState == Player.STATE_READY) {
                                        duration = player.duration.coerceAtLeast(0)
                                    }
                                }
                            }
                            player.addListener(listener)

                            onRetire {
                                player.removeListener(listener)
                                player.release()
                            }
                        }
                    )

                    LaunchedEffect(
                        key1 = isPlayerVisible,
                        key2 = isSeeking,
                        key3 = isPlaying,
                        block = {
                            if (!isPlayerVisible) return@LaunchedEffect
                            delay(5_000L)
                            if (!isSeeking) isPlayerVisible = false
                        }
                    )

                    LaunchedEffect(
                        key1 = player,
                        key2 = isPlaying,
                        key3 = isSeeking,
                        block = {
                            while (isPlaying) {
                                if (!isSeeking) currentPosition = player.currentPosition.coerceAtLeast(0)
                                delay(16L)
                            }
                        }
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            space = 32.dp,
                            alignment = Alignment.CenterVertically
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(32.dp)
                    ) {
                        Button(
                            onClick = {
                                launcher.launch(
                                    PickVisualMediaRequest(
                                        mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly
                                    )
                                )
                            }
                        ) {
                            Text(
                                text = "Pick video"
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1F)
                        ) {
                            ContentFrame(
                                player = player,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                        onClick = {
                                            isPlayerVisible = !isPlayerVisible
                                        }
                                    )
                            )
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AnimatedVisibility(
                                    visible = isPlayerVisible,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    VideoPlayer(
                                        isPlaying = isPlaying,
                                        isBuffering = isBuffering,
                                        currentPosition = currentPosition,
                                        duration = duration,
                                        onSeekBarPositionChange = {
                                            isSeeking = true
                                            currentPosition = it
                                        },
                                        onSeekBarPositionChangeFinished = {
                                            player.seekTo(it)
                                            isSeeking = false
                                        },
                                        onPlayPauseClick = {
                                            when {
                                                !isPlaying && player.playbackState == Player.STATE_ENDED -> {
                                                    player.seekTo(0)
                                                    player.play()
                                                }

                                                !isPlaying -> player.play()
                                                else -> player.pause()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}