# Roadmap

Planned improvements to Media3Player, worked through in order. Items marked
**+dep** add a Media3 module and increase the extension's size.

## 1. Designer organization

- [x] Group properties into categories (App Inventor only offers fixed ones: Appearance
      and Behavior)
- [x] Dropdowns: resize mode (Fit, Fill, Zoom, Fixed width, Fixed height), repeat mode
      (Off, One, All), fullscreen orientation (Auto, Landscape, Portrait, Sensor)
- [x] Getter blocks for every designer property
- [x] Descriptions for every block and dropdown option

## 2. Playback control

- [x] Repeat Off / One / All (replaces `Loop`) and shuffle
- [x] Volume and mute
- [x] Pitch and skip silence
- [x] Autoplay on or off, start position for `Play`
- [x] `Stop`, `Replay`, `PlayAt(index)`, seek to a position in another item
- [x] Clipping: play only a part of a media between start and end times
- [x] Seek mode: exact or nearest keyframe

## 3. Media sources and formats

- [x] Custom HTTP headers and user agent
- [x] DRM (Widevine) with license URL and headers
- [x] Live streams: jump to live edge, target live offset, DVR seeking, `IsLive`
- [x] RTSP **+dep** and SmoothStreaming **+dep**
- [x] MIME type hint for URLs without an extension; local files and content URIs
- [x] Per-item metadata: artist, description, artwork URL

## 4. Queue and playlist

- [x] Insert, remove and move items; queue length; queue as a list
- [x] `CurrentIndex`, `HasNext`, `HasPrevious`; queue changed event
- [x] Up next countdown on or off and its length

## 5. Tracks, quality and subtitles

- [x] Blocks to list video qualities, audio and subtitle languages and select them
- [x] Preferred audio and subtitle language, subtitles on by default
- [x] Data saver: maximum resolution and bitrate
- [x] Subtitle styling: size, text color, background, edge style, position

## 6. Player UI customization

- [ ] Show or hide each control: previous/next, ±seek, speed chip, subtitles, settings,
      fullscreen, lock, title, time bar
- [ ] Colors: background, control tint, seek bar played / buffered / unplayed, scrim
- [ ] Poster image before playback; audio-only mode with artwork
- [ ] Remaining time instead of duration; corner radius
- [ ] Each gesture separately: double tap seek, hold for 2×, brightness, volume, pinch to zoom
- [ ] Keep the screen on while playing
- [ ] No built-in controls, so apps can build their own UI with blocks
- [ ] Move the player to another arrangement; show it as a dialog

## 7. Background playback and system integration

- [x] Audio focus: pause or lower volume for calls and other audio
- [x] Pause when headphones are unplugged
- [ ] Background audio with a media notification and lock screen controls **+dep**
      (media3-session)
- [ ] Headset and Bluetooth media buttons (through the session)

## 8. Events and data

- [ ] Periodic position update event; buffered position; buffering started and ended
- [ ] Video size changed, first frame rendered, tracks changed, speed changed
- [ ] Stream metadata: ICY and ID3 titles from internet radio
- [ ] Error codes by category (network, not found, decoder, DRM…) besides the message
- [ ] Statistics: bitrate, resolution, dropped frames, bandwidth estimate

## 9. Audio

- [ ] Equalizer presets and custom bands, bass boost, loudness enhancer
- [ ] Mono and channel balance

## 10. Caching and offline

- [ ] Disk cache for streamed media with a size limit
- [ ] Preload the next item in the queue
- [ ] Downloads for offline playback with progress events **+dep**

## 11. Advanced

- [ ] Capture the current frame to an image file
- [ ] Several players on one screen (verify)
- [ ] Thumbnail previews while scrubbing (sprite sheets)

## Not possible in App Inventor

- Picture-in-picture: needs an attribute on the Screen activity, which extensions can't set.
- Jetpack Compose and XML layouts: App Inventor's AndroidX is too old and extensions get
  no R class.
- Casting and IMA ads: need Google Play Services libraries and setup App Inventor doesn't
  provide.
