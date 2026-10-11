# ExoPlayer for App Inventor (v3)

A video and audio player extension for MIT App Inventor and App Inventor based
platforms, built on [AndroidX Media3](https://developer.android.com/media/media3).

The successor of [exoplayer-appinventor](https://github.com/zainulhassan815/exoplayer-appinventor),
built with [Rush 2.0](https://github.com/zainulhassan815/rush-cli), which ships Media3
as real AARs with their resources.

## Features

- Modern player UI: auto-hiding controls, seek bar with buffering and chapters,
  previous / next, ±10s, speed chip, subtitles and settings
- Gestures: double tap to seek, hold for 2× speed, pinch to zoom, swipe for brightness
  and volume in fullscreen, each of which can be turned off
- Fullscreen with landscape or portrait orientation, matching the video
- Settings sheet: quality, playback speed, subtitles, audio track, zoom and loop
- Downloads for offline playback, a disk cache for streams and preloading of the next media
- Equalizer, bass boost, loudness boost, mono and left/right balance
- Background playback with a media notification, lock screen and headset controls
- Queue with an up next countdown, replay, error and retry states, and a controls lock
- MP4, HLS, DASH, SmoothStreaming and RTSP, live streams with a LIVE indicator, DRM
  (Widevine, ClearKey, PlayReady), custom HTTP headers; side-loaded WebVTT, SRT, TTML
  and SSA subtitles
- Accent color, resize mode, fullscreen orientation, repeat mode and behavior
  configurable from the designer

## Usage

1. Add an arrangement (e.g. a VerticalArrangement with Width *Fill parent* and a
   fixed Height) and a **Media3Player** component.
2. In `Screen1.Initialize`, call `Media3Player1.Create(VerticalArrangement1)`.
3. Call `Play(source, title)`, and `AddToQueue(source, title)` for more media.
   Sources are URLs or names of media assets uploaded to the project.

### Blocks

| Functions | |
|---|---|
| `Create(arrangement)` | Shows the player inside the arrangement |
| `Play(source, title)` / `PlayFrom(source, title, positionMs)` | Plays media, replacing the queue |
| `AddToQueue(source, title)` / `InsertAt(index, source, title)` | Adds media to the queue |
| `RemoveAt(index)` / `MoveItem(from, to)` | Edits the queue |
| `AddSubtitle(source, language, label)` | Adds a subtitle file to the most recently added media |
| `ClipMedia(startMs, endMs)` | Plays only part of the most recently added media |
| `SetThumbnails(spriteUrl, columns, rows, intervalSeconds)` | Shows thumbnails from a sprite sheet while scrubbing the most recently added media |
| `SetDrm(scheme, licenseUrl, headers)` | Plays the most recently added media with Widevine, ClearKey or PlayReady DRM |
| `SetMediaType(type)` | Sets the format (HLS, DASH, SmoothStreaming, RTSP, Progressive) of the most recently added media |
| `SetMetadata(artist, description, artworkUrl)` | Sets details of the most recently added media |
| `SetHeader(name, value)` / `ClearHeaders()` | HTTP headers sent with every media request |
| `SeekToLiveEdge()` | Jumps to the live edge of a live stream |
| `Download(source, title)` / `RemoveDownload(source)` / `RemoveAllDownloads()` | Downloads media for offline playback |
| `IsDownloaded(source)` / `DownloadProgress(source)` | Download status |
| `ClearCache()` | Deletes cached media |
| `UseEqualizerPreset(name)` / `SetBandLevel(band, decibels)` / `BandLevel(band)` | Equalizer |
| `SelectQuality(index)` / `SelectAudioTrack(index)` / `SelectSubtitle(index)` | Chooses from `VideoQualities`, `AudioTracks` and `SubtitleTracks` |
| `AddChapter(startSeconds, title)` / `ClearChapters()` | Chapters shown while scrubbing |
| `Resume()` / `Pause()` / `Stop()` / `Replay()` / `SeekTo(positionMs)` | Playback control |
| `Next()` / `Previous()` / `PlayAt(index)` / `SeekToItem(index, positionMs)` / `ClearQueue()` | Queue control |
| `EnterFullscreen()` / `ExitFullscreen()` | Fullscreen control |
| `ShowAsDialog()` / `CloseDialog()` | Shows the player in a dialog over the screen |
| `MoveTo(arrangement)` | Moves the player into another arrangement |
| `CaptureFrame(fileName)` | Saves the current video frame as a PNG image |

Events: `PlaybackStateChanged`, `IsPlayingChanged`, `BufferingChanged`, `PositionChanged`,
`MediaChanged`, `MetadataChanged`, `StreamTitleChanged`, `QueueChanged`, `TracksChanged`,
`VideoSizeChanged`, `FirstFrameRendered`, `SpeedChanged`, `FullscreenChanged`, `DialogClosed`, `DownloadChanged`, `FrameCaptured`,
`PlayerError(message, errorCode, category)` with category Network, NotFound, Format, Decoder,
DRM or Other. An index outside the queue raises `Screen.ErrorOccurred`
with error number 30101, and one outside a track list with 30102.

Properties, grouped in the designer:

- Appearance: `ShowControls`, `ShowPreviousNext`, `ShowSeekButtons`, `ShowSpeedButton`,
  `ShowSubtitlesButton`, `ShowSettingsButton`, `ShowFullscreenButton`, `ShowLockButton`,
  `ShowTitle`, `ShowTimeBar`, `BackgroundColor`, `ControlsColor`, `ScrimColor`,
  `SeekBarTrackColor`, `SeekBarBufferedColor`, `CornerRadius`, `PosterImage`,
  `ShowRemainingTime`, `AccentColor`, `ResizeMode` (Fit, Fill, Zoom, Fixed width, Fixed height),
  `FullscreenOrientation` (Auto, Landscape, Portrait, Sensor), `SubtitleSize`,
  `SubtitleTextColor`, `SubtitleBackgroundColor`, `SubtitleEdge`, `SubtitleBottomMargin`
- Behavior: `SeekIncrement`, `ControlsTimeout`, `DoubleTapToSeek`, `HoldForSpeed`,
  `SwipeForBrightness`, `SwipeForVolume`, `PinchToZoom`, `KeepScreenOn`, `BackgroundPlayback`,
  `PositionUpdateInterval`, `PreloadNext`, `EqualizerEnabled`, `BassBoost`, `LoudnessBoost`, `Mono`,
  `Balance`, `RepeatMode`
  (Off, One, All), `Shuffle`, `Autoplay`, `Volume`, `Muted`, `Pitch`, `SkipSilence`,
  `SeekMode` (Exact, Closest keyframe), `PlaybackSpeed`, `AutoAdvance`, `UpNextCountdown`, `PreferredAudioLanguage`,
  `PreferredSubtitleLanguage`, `ShowSubtitles`, `MaxVideoHeight`, `MaxVideoBitrate`
- Advanced: `UserAgent`, `TargetLiveOffset`, `CacheSize`
- Read-only: `IsPlaying`, `IsFullscreen`, `IsLive`, `LiveOffset`, `Position`, `Duration`,
  `Title`, `Artist`, `Queue`, `QueueLength`, `CurrentIndex`, `HasNext`, `HasPrevious`,
  `VideoQualities`, `AudioTracks`, `SubtitleTracks`, `IsDialogShowing`, `BufferedPosition`,
  `BufferedPercentage`, `VideoWidth`, `VideoHeight`, `VideoBitrate`, `DroppedFrames`,
  `BandwidthEstimate`, `EqualizerPresets`, `EqualizerPreset`, `EqualizerBands`,
  `EqualizerMinLevel`, `EqualizerMaxLevel`, `CacheUsed`, `Downloads`

## Testing

Build an APK to test the extension. The AI Companion can't load the Media3 libraries
and resources the extension ships, so the player doesn't work in it.

## Building

Requires [Rush 2.0](https://github.com/zainulhassan815/rush-cli) and JDK 17+.

```sh
rush build
```

The extension is generated in the `out` directory. `design/player-mockup.html` is
the interactive mockup the player UI is built from.

## License

[GPL-2.0](LICENSE)
