# ExoPlayer for App Inventor (v3)

A video and audio player extension for MIT App Inventor and App Inventor based
platforms, built on [AndroidX Media3](https://developer.android.com/media/media3).

The successor of [exoplayer-appinventor](https://github.com/zainulhassan815/exoplayer-appinventor),
built with [Rush 2.0](https://github.com/zainulhassan815/rush-cli), which ships Media3
as real AARs with their resources.

## Features

- Modern player UI: auto-hiding controls, seek bar with buffering and chapters,
  previous / next, ±10s, speed chip, subtitles and settings
- Gestures: double tap to seek, hold for 2× speed, swipe for brightness and volume
  in fullscreen
- Fullscreen with landscape or portrait orientation, matching the video
- Settings sheet: quality, playback speed, subtitles, audio track, zoom and loop
- Queue with an up next countdown, replay, error and retry states, and a controls lock
- MP4, HLS and DASH; side-loaded WebVTT, SRT, TTML and SSA subtitles
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
| `Play(source, title)` | Plays media, replacing the queue |
| `AddToQueue(source, title)` | Adds media to the end of the queue |
| `AddSubtitle(source, language, label)` | Adds a subtitle file to the most recently added media |
| `AddChapter(startSeconds, title)` / `ClearChapters()` | Chapters shown while scrubbing |
| `Resume()` / `Pause()` / `SeekTo(positionMs)` | Playback control |
| `Next()` / `Previous()` / `ClearQueue()` | Queue control |
| `EnterFullscreen()` / `ExitFullscreen()` | Fullscreen control |

Events: `PlaybackStateChanged`, `IsPlayingChanged`, `MediaChanged`,
`FullscreenChanged`, `PlayerError`.

Properties, grouped in the designer:

- Appearance: `AccentColor`, `ResizeMode` (Fit, Fill, Zoom, Fixed width, Fixed height),
  `FullscreenOrientation` (Auto, Landscape, Portrait, Sensor)
- Behavior: `SeekIncrement`, `ControlsTimeout`, `GesturesEnabled`, `RepeatMode`
  (Off, One, All), `PlaybackSpeed`
- Read-only: `IsPlaying`, `IsFullscreen`, `Position`, `Duration`, `Title`

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
