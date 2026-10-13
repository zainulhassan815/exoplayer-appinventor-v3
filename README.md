# ExoPlayer for App Inventor (v3)

A video and audio player extension for MIT App Inventor and App Inventor based
platforms, built on [AndroidX Media3](https://developer.android.com/media/media3).

The successor of [exoplayer-appinventor](https://github.com/zainulhassan815/exoplayer-appinventor),
built with [Rush](https://github.com/zainulhassan815/rush-cli), which ships Media3 as
real AARs with their resources.

<p>
  <img src="docs/screenshots/fullscreen.jpg" alt="Fullscreen playback with controls" width="49%">
  <img src="docs/screenshots/settings.jpg" alt="Settings sheet with quality, speed, subtitles and audio track" width="49%">
</p>
<p>
  <img src="docs/screenshots/inline.jpg" alt="Player inside an arrangement" width="32%">
  <img src="docs/screenshots/thumbnails.jpg" alt="Thumbnail preview and chapter title while scrubbing" width="32%">
</p>

## Getting started

1. Download `dev.dreamerslab.media3.aix` from the
   [latest release](https://github.com/zainulhassan815/exoplayer-appinventor-v3/releases/latest)
   and import it in App Inventor (Palette → Extension → Import extension).
2. Add an arrangement, for example a VerticalArrangement with Width *Fill parent* and a
   fixed Height, and a **Media3Player** component.
3. In `Screen1.Initialize`, call `Media3Player1.Create(VerticalArrangement1)`.
4. Call `Play(source, title)`, and `AddToQueue(source, title)` for more media. Sources
   are URLs or names of media assets uploaded to the project.
5. Build an APK to try it. The AI Companion can't load the Media3 libraries the
   extension ships, so the player doesn't work in it.

Each release also has `Media3Sample.aia`, a ready-made project to import (My Projects →
Import project), and `dev.dreamerslab.media3.md`, the documentation of every block.

Requirements: Android 6.0 or newer (`CaptureFrame` needs Android 7.0). The extension
adds about 12 MB to apps.

## Features

- Modern player UI: auto-hiding controls, seek bar with buffering and chapters,
  previous / next, ±10s, speed chip, subtitles and settings
- Gestures: double tap to seek, hold for 2× speed, pinch to zoom, swipe for brightness
  and volume in fullscreen, each of which can be turned off
- D-pad and remote navigation: every control and the seek bar take focus, and the first
  key shows hidden controls
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

## Blocks

| Functions | |
|---|---|
| `Create(arrangement)` | Shows the player inside the arrangement |
| `Play(source, title)` / `PlayFrom(source, title, positionMs)` | Plays media, replacing the queue |
| `AddToQueue(source, title)` / `InsertAt(index, source, title)` | Adds media to the queue |
| `RemoveAt(index)` / `MoveItem(from, to)` | Edits the queue |
| `AddSubtitle(source, language, label)` | Adds a subtitle file to the most recently added media |
| `ClipMedia(startMs, endMs)` | Plays only part of the most recently added media |
| `SetThumbnailTrack(vttUrl)` | Shows thumbnails from a WebVTT thumbnail track (like `storyboard.vtt`) while scrubbing the most recently added media |
| `SetThumbnails(spriteUrl, columns, rows, intervalSeconds)` | Shows thumbnails from a single sprite sheet grid while scrubbing the most recently added media |
| `SetDrm(scheme, licenseUrl, headers)` | Plays the most recently added media with Widevine, ClearKey or PlayReady DRM |
| `SetClearKeys(keys)` | Plays the most recently added media with ClearKey DRM from known keys (key ID → key, in hex), without a license server |
| `SetMediaType(type)` | Sets the format (HLS, DASH, SmoothStreaming, RTSP, Progressive) of the most recently added media |
| `SetMetadata(artist, description, artworkUrl)` | Sets details of the most recently added media |
| `SetHeader(name, value)` / `ClearHeaders()` | HTTP headers sent with every media request |
| `SeekToLiveEdge()` | Jumps to the live edge of a live stream |
| `Download(source, title)` / `RemoveDownload(source)` / `RemoveAllDownloads()` | Downloads media for offline playback |
| `IsDownloaded(source)` / `DownloadProgress(source)` | Download status |
| `ClearCache()` | Deletes cached media |
| `IncreaseDeviceVolume()` / `DecreaseDeviceVolume()` | Changes the phone's media volume by one step |
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
`VideoSizeChanged`, `FirstFrameRendered`, `SpeedChanged`, `FullscreenChanged`, `DialogClosed`, `DownloadChanged`, `FrameCaptured`, `SubtitleChanged`,
`DeviceVolumeChanged`,
`PlayerError(message, errorCode, category)` with category Network, NotFound, Format, Decoder,
DRM or Other. An index outside the queue raises `Screen.ErrorOccurred`
with error number 30101, one outside a track list with 30102, and a malformed ClearKey key with 30105.

Properties, grouped in the designer:

- Appearance: `ShowControls`, `ShowPreviousNext`, `ShowSeekButtons`, `ShowSpeedButton`,
  `ShowSubtitlesButton`, `ShowSettingsButton`, `ShowFullscreenButton`, `ShowLockButton`,
  `ShowTitle`, `ShowTimeBar`, `BackgroundColor`, `ControlsColor`, `ScrimColor`,
  `SeekBarTrackColor`, `SeekBarBufferedColor`, `CornerRadius`, `PosterImage`,
  `ShowRemainingTime`, `AccentColor`, `ResizeMode` (Fit, Fill, Zoom, Fixed width, Fixed height),
  `FullscreenOrientation` (Auto, Landscape, Portrait, Sensor), `SubtitleSize`,
  `SubtitleTextColor`, `SubtitleBackgroundColor`, `SubtitleEdge`, `SubtitleBottomMargin`
- Behavior: `SeekIncrement`, `ControlsTimeout`, `DoubleTapToSeek`, `HoldForSpeed`,
  `SwipeForBrightness`, `SwipeForVolume`, `PinchToZoom`, `KeepScreenOn`, `BackgroundPlayback`, `AudioFocus`,
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
  `EqualizerMinLevel`, `EqualizerMaxLevel`, `CacheUsed`, `Downloads`, `DeviceMinVolume`,
  `DeviceMaxVolume`
- Phone volume: `DeviceVolume`, `DeviceMuted`

## Several players

Players pause when other audio starts, including another player in the app, as Android
media players should, so starting one pauses the others. Turn `AudioFocus` off on players
that should play on their own. Muted players never pause others or get paused.

## Publishing on Google Play

For `BackgroundPlayback` and `Download`, the extension declares two foreground services
in every app that uses it: media playback and data sync, with their permissions. Google
Play asks apps to explain each foreground service type they declare in the Play Console
(App content → Foreground service permissions). Describe them as playing media in the
background and downloading media for offline playback.

## FAQ

**Does it work in the AI Companion?** No. The Companion can't load the Media3 libraries the
extension ships. Build an APK to test.

**Can it play YouTube or Google Drive links?** No. A YouTube page isn't a media URL, and
playing YouTube outside its official players is against YouTube's terms. A Drive share link
opens a web page; use a direct download link to the file instead.

**Can it cast to a TV, or play in picture-in-picture?** No. Casting needs Google Play
Services, and picture-in-picture needs a setting on the screen's activity that extensions
can't change.

**Why can't I seek in some MKV files?** The file has no seek index (cues). That's a
property of the file; remuxing it, for example with `ffmpeg -i in.mkv -c copy out.mkv`,
adds one.

**Why doesn't a video play while others do?** Codecs like HEVC 10-bit, AC-3/E-AC-3 and DTS
depend on the phone's decoders. Check `PlayerError`: category Decoder or Format means the
device can't decode that file.

**Does it play live streams and internet radio?** Yes: HLS, DASH and RTSP live streams, and
radio streams with their titles in `StreamTitleChanged`.

## Building

Requires [Rush 2.2](https://github.com/zainulhassan815/rush-cli) or newer and JDK 17.

```sh
rush build
python3 samples/package.py
```

The extension and its block documentation are generated in the `out` directory, and the
second command packs the sample project with it. `design/player-mockup.html` is the
interactive mockup the player UI is built from.

GitHub Actions builds every push. To release, update `version` in `rush.yml`, commit, and
push a tag with the same version, like `v3.0.0`: the release is published with the
extension, its documentation and the sample project.

## License

[GPL-2.0](LICENSE)
