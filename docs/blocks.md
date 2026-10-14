# Blocks

Every block of Media3Player 3.2.0. Each image contains its block, so you can drag it straight into your workspace.

- [Functions: Setup and playback](#functions-setup-and-playback)
- [Functions: Queue](#functions-queue)
- [Functions: Media items](#functions-media-items)
- [Functions: Fullscreen, dialog and frames](#functions-fullscreen-dialog-and-frames)
- [Functions: Tracks, chapters and requests](#functions-tracks-chapters-and-requests)
- [Functions: Audio and volume](#functions-audio-and-volume)
- [Functions: Downloads and cache](#functions-downloads-and-cache)
- [Events](#events)
- [Properties: Appearance](#properties-appearance)
- [Properties: Behavior](#properties-behavior)
- [Properties: Advanced](#properties-advanced)
- [Properties: Phone volume](#properties-phone-volume)
- [Properties: Read-only](#properties-read-only)
- [Dropdowns](#dropdowns)

## Functions: Setup and playback

Shows the player and controls playback.

![Create](images/blocks/Media3Player1-Create.png)

**Create:** Shows the player inside the given arrangement.

![Play](images/blocks/Media3Player1-Play.png)

**Play:** Plays the media at the given url or asset name, replacing the queue.

![PlayFrom](images/blocks/Media3Player1-PlayFrom.png)

**PlayFrom:** Plays the media at the given url or asset name from the given position in milliseconds, replacing the queue.

![Resume](images/blocks/Media3Player1-Resume.png)

**Resume:** Resumes playback.

![Pause](images/blocks/Media3Player1-Pause.png)

**Pause:** Pauses playback, keeping the position and the buffered media. Resume plays on.

![Stop](images/blocks/Media3Player1-Stop.png)

**Stop:** Stops playback and releases the media, keeping the queue. Resume plays it again.

![Replay](images/blocks/Media3Player1-Replay.png)

**Replay:** Plays the current media again from the start.

![SeekTo](images/blocks/Media3Player1-SeekTo.png)

**SeekTo:** Seeks to the given position in milliseconds in the current media.

![SeekToLiveEdge](images/blocks/Media3Player1-SeekToLiveEdge.png)

**SeekToLiveEdge:** Jumps to the live edge of a live stream.

## Functions: Queue

Builds and moves through the queue. Indexes start at 1.

![AddToQueue](images/blocks/Media3Player1-AddToQueue.png)

**AddToQueue:** Adds the media at the given url or asset name to the end of the queue.

![InsertAt](images/blocks/Media3Player1-InsertAt.png)

**InsertAt:** Inserts the media at the given url or asset name at the given position in the queue. Index starts at 1; one more than the queue length adds it to the end.

![RemoveAt](images/blocks/Media3Player1-RemoveAt.png)

**RemoveAt:** Removes the media at the given position from the queue. Index starts at 1.

![MoveItem](images/blocks/Media3Player1-MoveItem.png)

**MoveItem:** Moves the media at one position in the queue to another. Indexes start at 1.

![PlayAt](images/blocks/Media3Player1-PlayAt.png)

**PlayAt:** Plays the media at the given position in the queue. Index starts at 1.

![SeekToItem](images/blocks/Media3Player1-SeekToItem.png)

**SeekToItem:** Plays the media at the given position in the queue from the given position in milliseconds. Index starts at 1.

![Next](images/blocks/Media3Player1-Next.png)

**Next:** Plays the next media in the queue.

![Previous](images/blocks/Media3Player1-Previous.png)

**Previous:** Plays the previous media in the queue, or restarts the current one.

![ClearQueue](images/blocks/Media3Player1-ClearQueue.png)

**ClearQueue:** Removes all media from the queue and stops playback.

## Functions: Media items

Media items describe one media and how to play it, as a dictionary with keys from the field dropdowns. See [Media items](../README.md#media-items).

![SetMediaItems](images/blocks/Media3Player1-SetMediaItems.png)

**SetMediaItems:** Plays the given media items, replacing the queue. Each media item is a dictionary whose keys are MediaItemField choices; only Uri is required.

![AddMediaItem](images/blocks/Media3Player1-AddMediaItem.png)

**AddMediaItem:** Adds a media item to the end of the queue. The media item is a dictionary whose keys are MediaItemField choices; only Uri is required.

![AddMediaItems](images/blocks/Media3Player1-AddMediaItems.png)

**AddMediaItems:** Adds a list of media items to the end of the queue, like AddMediaItem for each.

![ReplaceMediaItem](images/blocks/Media3Player1-ReplaceMediaItem.png)

**ReplaceMediaItem:** Replaces the media item at the given position in the queue with another, which plays from the start if it was the current media. Index starts at 1.

![GetMediaItemAt](images/blocks/Media3Player1-GetMediaItemAt.png)

**GetMediaItemAt:** The media item at the given position in the queue, as a dictionary. Index starts at 1.

![SetMediaItemField](images/blocks/Media3Player1-SetMediaItemField.png)

**SetMediaItemField:** Changes one field of the media item at the given position in the queue. Index starts at 1.

![SetDrmConfigurationField](images/blocks/Media3Player1-SetDrmConfigurationField.png)

**SetDrmConfigurationField:** Changes one DRM setting of the media item at the given position in the queue. Index starts at 1.

![SetSubtitleConfigurationField](images/blocks/Media3Player1-SetSubtitleConfigurationField.png)

**SetSubtitleConfigurationField:** Changes one setting of a subtitle of the media item at the given position in the queue. Both indexes start at 1.

![SetClippingConfigurationField](images/blocks/Media3Player1-SetClippingConfigurationField.png)

**SetClippingConfigurationField:** Changes where the media item at the given position in the queue starts or ends. Index starts at 1.

![SetThumbnailsField](images/blocks/Media3Player1-SetThumbnailsField.png)

**SetThumbnailsField:** Changes one thumbnails setting of the media item at the given position in the queue. Index starts at 1.

## Functions: Fullscreen, dialog and frames

Where the player is shown, and saving frames.

![EnterFullscreen](images/blocks/Media3Player1-EnterFullscreen.png)

**EnterFullscreen:** Shows the player fullscreen.

![ExitFullscreen](images/blocks/Media3Player1-ExitFullscreen.png)

**ExitFullscreen:** Leaves fullscreen.

![ShowAsDialog](images/blocks/Media3Player1-ShowAsDialog.png)

**ShowAsDialog:** Shows the player in a dialog over the screen. Tapping outside or pressing back closes it.

![CloseDialog](images/blocks/Media3Player1-CloseDialog.png)

**CloseDialog:** Closes the dialog shown by ShowAsDialog and returns the player to its arrangement.

![MoveTo](images/blocks/Media3Player1-MoveTo.png)

**MoveTo:** Moves the player into another arrangement, keeping what it plays.

![CaptureFrame](images/blocks/Media3Player1-CaptureFrame.png)

**CaptureFrame:** Saves the current video frame as a PNG image and fires FrameCaptured with its path. A file name without a folder is saved in the app's files folder. Protected (DRM) video can't be captured.

## Functions: Tracks, chapters and requests

Quality, audio and subtitle tracks, chapters, and HTTP headers.

![SelectQuality](images/blocks/Media3Player1-SelectQuality.png)

**SelectQuality:** Plays the video quality at the given position in VideoQualities. Index starts at 1; 1 is Auto.

![SelectAudioTrack](images/blocks/Media3Player1-SelectAudioTrack.png)

**SelectAudioTrack:** Plays the audio track at the given position in AudioTracks. Index starts at 1.

![SelectSubtitle](images/blocks/Media3Player1-SelectSubtitle.png)

**SelectSubtitle:** Shows the subtitles at the given position in SubtitleTracks. Index starts at 1; 1 turns subtitles off.

![AddChapter](images/blocks/Media3Player1-AddChapter.png)

**AddChapter:** Adds a chapter starting at the given second, shown on the seek bar while scrubbing.

![ClearChapters](images/blocks/Media3Player1-ClearChapters.png)

**ClearChapters:** Removes all chapters.

![SetHeader](images/blocks/Media3Player1-SetHeader.png)

**SetHeader:** Sends the given HTTP header with every media request.

![ClearHeaders](images/blocks/Media3Player1-ClearHeaders.png)

**ClearHeaders:** Stops sending the headers set with SetHeader.

## Functions: Audio and volume

The equalizer and the phone's media volume.

![UseEqualizerPreset](images/blocks/Media3Player1-UseEqualizerPreset.png)

**UseEqualizerPreset:** Uses the equalizer preset with the given name from EqualizerPresets.

![SetBandLevel](images/blocks/Media3Player1-SetBandLevel.png)

**SetBandLevel:** Sets the level of an equalizer band in decibels, between EqualizerMinLevel and EqualizerMaxLevel. Band starts at 1, in the order of EqualizerBands.

![BandLevel](images/blocks/Media3Player1-BandLevel.png)

**BandLevel:** Level of an equalizer band in decibels. Band starts at 1.

![IncreaseDeviceVolume](images/blocks/Media3Player1-IncreaseDeviceVolume.png)

**IncreaseDeviceVolume:** Turns the phone's media volume up by one step.

![DecreaseDeviceVolume](images/blocks/Media3Player1-DecreaseDeviceVolume.png)

**DecreaseDeviceVolume:** Turns the phone's media volume down by one step.

## Functions: Downloads and cache

Offline playback and the stream cache.

![Download](images/blocks/Media3Player1-Download.png)

**Download:** Downloads the media at the given url for offline playback. Playing the same url plays the download, even without a connection.

![RemoveDownload](images/blocks/Media3Player1-RemoveDownload.png)

**RemoveDownload:** Deletes the download of the media at the given url.

![RemoveAllDownloads](images/blocks/Media3Player1-RemoveAllDownloads.png)

**RemoveAllDownloads:** Deletes all downloads.

![IsDownloaded](images/blocks/Media3Player1-IsDownloaded.png)

**IsDownloaded:** Whether the media at the given url is fully downloaded.

![DownloadProgress](images/blocks/Media3Player1-DownloadProgress.png)

**DownloadProgress:** How much of the media at the given url has downloaded, from 0 to 100.

![ClearCache](images/blocks/Media3Player1-ClearCache.png)

**ClearCache:** Deletes the media stored by the cache.

## Events

Events fire on the main thread, after Screen.Initialize.

![PlaybackStateChanged](images/blocks/Media3Player1-PlaybackStateChanged.png)

**PlaybackStateChanged:** The playback state changed to IDLE, BUFFERING, READY or ENDED.

![IsPlayingChanged](images/blocks/Media3Player1-IsPlayingChanged.png)

**IsPlayingChanged:** Playback started or stopped.

![PlayerError](images/blocks/Media3Player1-PlayerError.png)

**PlayerError:** Playback failed.

![PositionChanged](images/blocks/Media3Player1-PositionChanged.png)

**PositionChanged:** Sent every PositionUpdateInterval milliseconds while media plays.

![BufferingChanged](images/blocks/Media3Player1-BufferingChanged.png)

**BufferingChanged:** The player started or stopped waiting for media to load.

![VideoSizeChanged](images/blocks/Media3Player1-VideoSizeChanged.png)

**VideoSizeChanged:** The size of the video changed, for example when another quality started playing.

![FirstFrameRendered](images/blocks/Media3Player1-FirstFrameRendered.png)

**FirstFrameRendered:** The first frame of the video appeared.

![SpeedChanged](images/blocks/Media3Player1-SpeedChanged.png)

**SpeedChanged:** The playback speed changed.

![RepeatModeChanged](images/blocks/Media3Player1-RepeatModeChanged.png)

**RepeatModeChanged:** The repeat mode changed, from blocks or from the loop setting in the player.

![ShuffleModeEnabledChanged](images/blocks/Media3Player1-ShuffleModeEnabledChanged.png)

**ShuffleModeEnabledChanged:** Shuffle was turned on or off.

![MetadataChanged](images/blocks/Media3Player1-MetadataChanged.png)

**MetadataChanged:** The title or artist of the current media changed.

![StreamTitleChanged](images/blocks/Media3Player1-StreamTitleChanged.png)

**StreamTitleChanged:** A stream announced what is playing now, like the song on an internet radio station. Either may be empty; radio stations often send "Artist - Song" as the title.

![MediaChanged](images/blocks/Media3Player1-MediaChanged.png)

**MediaChanged:** Another media in the queue started. Index starts at 1.

![TracksChanged](images/blocks/Media3Player1-TracksChanged.png)

**TracksChanged:** The video qualities, audio tracks or subtitles of the current media changed, for example once it loaded.

![QueueChanged](images/blocks/Media3Player1-QueueChanged.png)

**QueueChanged:** Media was added to, removed from, moved in or replaced in the queue.

![DownloadChanged](images/blocks/Media3Player1-DownloadChanged.png)

**DownloadChanged:** A download changed state: Queued, Downloading, Completed, Failed, Stopped, Removing, Restarting or Removed.

![SubtitleChanged](images/blocks/Media3Player1-SubtitleChanged.png)

**SubtitleChanged:** The subtitle on screen changed. Text holds its lines, or is empty when no subtitle shows. Useful to show subtitles your own way, for example with ShowControls off.

![DeviceVolumeChanged](images/blocks/Media3Player1-DeviceVolumeChanged.png)

**DeviceVolumeChanged:** The phone's media volume changed, from this app or outside it.

![FrameCaptured](images/blocks/Media3Player1-FrameCaptured.png)

**FrameCaptured:** CaptureFrame saved a video frame to the given path.

![DialogClosed](images/blocks/Media3Player1-DialogClosed.png)

**DialogClosed:** The dialog shown by ShowAsDialog was closed.

![FullscreenChanged](images/blocks/Media3Player1-FullscreenChanged.png)

**FullscreenChanged:** The player entered or left fullscreen.

## Properties: Appearance

Also in the designer, under Appearance.

![get ShowControls](images/blocks/Media3Player1-getShowControls.png) ![set ShowControls](images/blocks/Media3Player1-setShowControls.png)

**ShowControls** (read-write): Shows the built-in controls, gestures and overlays. Turn off to build your own controls with blocks.

![get ShowPreviousNext](images/blocks/Media3Player1-getShowPreviousNext.png) ![set ShowPreviousNext](images/blocks/Media3Player1-setShowPreviousNext.png)

**ShowPreviousNext** (read-write): Shows the previous and next buttons when the queue has more than one media.

![get ShowSeekButtons](images/blocks/Media3Player1-getShowSeekButtons.png) ![set ShowSeekButtons](images/blocks/Media3Player1-setShowSeekButtons.png)

**ShowSeekButtons** (read-write): Shows the rewind and forward buttons.

![get ShowSpeedButton](images/blocks/Media3Player1-getShowSpeedButton.png) ![set ShowSpeedButton](images/blocks/Media3Player1-setShowSpeedButton.png)

**ShowSpeedButton** (read-write): Shows the playback speed button.

![get ShowSubtitlesButton](images/blocks/Media3Player1-getShowSubtitlesButton.png) ![set ShowSubtitlesButton](images/blocks/Media3Player1-setShowSubtitlesButton.png)

**ShowSubtitlesButton** (read-write): Shows the subtitles button.

![get ShowSettingsButton](images/blocks/Media3Player1-getShowSettingsButton.png) ![set ShowSettingsButton](images/blocks/Media3Player1-setShowSettingsButton.png)

**ShowSettingsButton** (read-write): Shows the settings button.

![get ShowFullscreenButton](images/blocks/Media3Player1-getShowFullscreenButton.png) ![set ShowFullscreenButton](images/blocks/Media3Player1-setShowFullscreenButton.png)

**ShowFullscreenButton** (read-write): Shows the fullscreen button.

![get ShowLockButton](images/blocks/Media3Player1-getShowLockButton.png) ![set ShowLockButton](images/blocks/Media3Player1-setShowLockButton.png)

**ShowLockButton** (read-write): Shows the button that locks the controls.

![get ShowTitle](images/blocks/Media3Player1-getShowTitle.png) ![set ShowTitle](images/blocks/Media3Player1-setShowTitle.png)

**ShowTitle** (read-write): Shows the title of the media in fullscreen.

![get ShowTimeBar](images/blocks/Media3Player1-getShowTimeBar.png) ![set ShowTimeBar](images/blocks/Media3Player1-setShowTimeBar.png)

**ShowTimeBar** (read-write): Shows the seek bar.

![get BackgroundColor](images/blocks/Media3Player1-getBackgroundColor.png) ![set BackgroundColor](images/blocks/Media3Player1-setBackgroundColor.png)

**BackgroundColor** (read-write): Color shown around and behind the video.

![get ControlsColor](images/blocks/Media3Player1-getControlsColor.png) ![set ControlsColor](images/blocks/Media3Player1-setControlsColor.png)

**ControlsColor** (read-write): Color of the control icons and text.

![get ScrimColor](images/blocks/Media3Player1-getScrimColor.png) ![set ScrimColor](images/blocks/Media3Player1-setScrimColor.png)

**ScrimColor** (read-write): Color of the shading behind the controls.

![get SeekBarTrackColor](images/blocks/Media3Player1-getSeekBarTrackColor.png) ![set SeekBarTrackColor](images/blocks/Media3Player1-setSeekBarTrackColor.png)

**SeekBarTrackColor** (read-write): Color of the unplayed part of the seek bar.

![get SeekBarBufferedColor](images/blocks/Media3Player1-getSeekBarBufferedColor.png) ![set SeekBarBufferedColor](images/blocks/Media3Player1-setSeekBarBufferedColor.png)

**SeekBarBufferedColor** (read-write): Color of the buffered part of the seek bar.

![get CornerRadius](images/blocks/Media3Player1-getCornerRadius.png) ![set CornerRadius](images/blocks/Media3Player1-setCornerRadius.png)

**CornerRadius** (read-write): Roundness of the player's corners, in pixels.

![get PosterImage](images/blocks/Media3Player1-getPosterImage.png) ![set PosterImage](images/blocks/Media3Player1-setPosterImage.png)

**PosterImage** (read-write): Image shown before the video starts, and for audio without its own artwork. A URL or asset name.

![get ShowRemainingTime](images/blocks/Media3Player1-getShowRemainingTime.png) ![set ShowRemainingTime](images/blocks/Media3Player1-setShowRemainingTime.png)

**ShowRemainingTime** (read-write): Shows the time left instead of the duration next to the position.

![get AccentColor](images/blocks/Media3Player1-getAccentColor.png) ![set AccentColor](images/blocks/Media3Player1-setAccentColor.png)

**AccentColor** (read-write): Color of the seek bar, buttons and highlights.

![get ResizeMode](images/blocks/Media3Player1-getResizeMode.png) ![set ResizeMode](images/blocks/Media3Player1-setResizeMode.png)

**ResizeMode** (read-write): How the video fits the player.

![get FullscreenOrientation](images/blocks/Media3Player1-getFullscreenOrientation.png) ![set FullscreenOrientation](images/blocks/Media3Player1-setFullscreenOrientation.png)

**FullscreenOrientation** (read-write): Orientation of the screen in fullscreen.

![get SubtitleSize](images/blocks/Media3Player1-getSubtitleSize.png) ![set SubtitleSize](images/blocks/Media3Player1-setSubtitleSize.png)

**SubtitleSize** (read-write): Size of subtitles in percent of the default size.

![get SubtitleTextColor](images/blocks/Media3Player1-getSubtitleTextColor.png) ![set SubtitleTextColor](images/blocks/Media3Player1-setSubtitleTextColor.png)

**SubtitleTextColor** (read-write): Color of the subtitle text.

![get SubtitleBackgroundColor](images/blocks/Media3Player1-getSubtitleBackgroundColor.png) ![set SubtitleBackgroundColor](images/blocks/Media3Player1-setSubtitleBackgroundColor.png)

**SubtitleBackgroundColor** (read-write): Color behind the subtitle text.

![get SubtitleEdge](images/blocks/Media3Player1-getSubtitleEdge.png) ![set SubtitleEdge](images/blocks/Media3Player1-setSubtitleEdge.png)

**SubtitleEdge** (read-write): Edge drawn around the subtitle text, in black.

![get SubtitleBottomMargin](images/blocks/Media3Player1-getSubtitleBottomMargin.png) ![set SubtitleBottomMargin](images/blocks/Media3Player1-setSubtitleBottomMargin.png)

**SubtitleBottomMargin** (read-write): Space below the subtitles, in percent of the player's height.

## Properties: Behavior

Also in the designer, under Behavior.

![get BackgroundPlayback](images/blocks/Media3Player1-getBackgroundPlayback.png) ![set BackgroundPlayback](images/blocks/Media3Player1-setBackgroundPlayback.png)

**BackgroundPlayback** (read-write): Keeps playing when the app goes to the background, with a media notification and controls on the lock screen and for headsets.

![get KeepScreenOn](images/blocks/Media3Player1-getKeepScreenOn.png) ![set KeepScreenOn](images/blocks/Media3Player1-setKeepScreenOn.png)

**KeepScreenOn** (read-write): Keeps the screen on while media plays.

![get SeekIncrement](images/blocks/Media3Player1-getSeekIncrement.png) ![set SeekIncrement](images/blocks/Media3Player1-setSeekIncrement.png)

**SeekIncrement** (read-write): Seconds skipped by the rewind and forward buttons and double taps, at least 1.

![get ControlsTimeout](images/blocks/Media3Player1-getControlsTimeout.png) ![set ControlsTimeout](images/blocks/Media3Player1-setControlsTimeout.png)

**ControlsTimeout** (read-write): Milliseconds of inactivity after which the controls hide.

![get DoubleTapToSeek](images/blocks/Media3Player1-getDoubleTapToSeek.png) ![set DoubleTapToSeek](images/blocks/Media3Player1-setDoubleTapToSeek.png)

**DoubleTapToSeek** (read-write): Double tap the sides to rewind or forward, and the middle to play or pause.

![get HoldForSpeed](images/blocks/Media3Player1-getHoldForSpeed.png) ![set HoldForSpeed](images/blocks/Media3Player1-setHoldForSpeed.png)

**HoldForSpeed** (read-write): Touch and hold to play at 2× speed.

![get SwipeForBrightness](images/blocks/Media3Player1-getSwipeForBrightness.png) ![set SwipeForBrightness](images/blocks/Media3Player1-setSwipeForBrightness.png)

**SwipeForBrightness** (read-write): Swipe up or down on the left in fullscreen to change the brightness.

![get SwipeForVolume](images/blocks/Media3Player1-getSwipeForVolume.png) ![set SwipeForVolume](images/blocks/Media3Player1-setSwipeForVolume.png)

**SwipeForVolume** (read-write): Swipe up or down on the right in fullscreen to change the volume.

![get PinchToZoom](images/blocks/Media3Player1-getPinchToZoom.png) ![set PinchToZoom](images/blocks/Media3Player1-setPinchToZoom.png)

**PinchToZoom** (read-write): Pinch out to zoom the video to fill the player, and in to fit it.

![get RepeatMode](images/blocks/Media3Player1-getRepeatMode.png) ![set RepeatMode](images/blocks/Media3Player1-setRepeatMode.png)

**RepeatMode** (read-write): Whether the current media or the whole queue repeats.

![get Shuffle](images/blocks/Media3Player1-getShuffle.png) ![set Shuffle](images/blocks/Media3Player1-setShuffle.png)

**Shuffle** (read-write): Shuffles the order the queue plays in.

![get Autoplay](images/blocks/Media3Player1-getAutoplay.png) ![set Autoplay](images/blocks/Media3Player1-setAutoplay.png)

**Autoplay** (read-write): Starts playing as soon as Play, PlayFrom or SetMediaItems loads media.

![get Volume](images/blocks/Media3Player1-getVolume.png) ![set Volume](images/blocks/Media3Player1-setVolume.png)

**Volume** (read-write): Volume of the player, from 0 to 100.

![get AudioFocus](images/blocks/Media3Player1-getAudioFocus.png) ![set AudioFocus](images/blocks/Media3Player1-setAudioFocus.png)

**AudioFocus** (read-write): Pauses this player when other audio starts, like a call, another app or another player in this app, as media players should. Turn off to play independently, for example several players at once. Muted players never take part.

![get Muted](images/blocks/Media3Player1-getMuted.png) ![set Muted](images/blocks/Media3Player1-setMuted.png)

**Muted** (read-write): Silences the player without changing its volume.

![get Pitch](images/blocks/Media3Player1-getPitch.png) ![set Pitch](images/blocks/Media3Player1-setPitch.png)

**Pitch** (read-write): Pitch of the audio, from 0.5 to 2. 1 is the original pitch.

![get SkipSilence](images/blocks/Media3Player1-getSkipSilence.png) ![set SkipSilence](images/blocks/Media3Player1-setSkipSilence.png)

**SkipSilence** (read-write): Skips silent parts of the audio.

![get SeekMode](images/blocks/Media3Player1-getSeekMode.png) ![set SeekMode](images/blocks/Media3Player1-setSeekMode.png)

**SeekMode** (read-write): How precisely SeekTo and the seek bar seek.

![get PlaybackSpeed](images/blocks/Media3Player1-getPlaybackSpeed.png) ![set PlaybackSpeed](images/blocks/Media3Player1-setPlaybackSpeed.png)

**PlaybackSpeed** (read-write): Playback speed, from 0.25 to 2.

![get PreferredAudioLanguage](images/blocks/Media3Player1-getPreferredAudioLanguage.png) ![set PreferredAudioLanguage](images/blocks/Media3Player1-setPreferredAudioLanguage.png)

**PreferredAudioLanguage** (read-write): Language of the audio track to play when media has several, as a code like "en". Empty uses the device language.

![get PreferredSubtitleLanguage](images/blocks/Media3Player1-getPreferredSubtitleLanguage.png) ![set PreferredSubtitleLanguage](images/blocks/Media3Player1-setPreferredSubtitleLanguage.png)

**PreferredSubtitleLanguage** (read-write): Language of the subtitles to show, as a code like "en". Subtitles in this language turn on automatically.

![get ShowSubtitles](images/blocks/Media3Player1-getShowSubtitles.png) ![set ShowSubtitles](images/blocks/Media3Player1-setShowSubtitles.png)

**ShowSubtitles** (read-write): Turns on subtitles for every new media that has them.

![get MaxVideoHeight](images/blocks/Media3Player1-getMaxVideoHeight.png) ![set MaxVideoHeight](images/blocks/Media3Player1-setMaxVideoHeight.png)

**MaxVideoHeight** (read-write): Highest video height to play, like 720 for 720p, to save data. 0 has no limit.

![get MaxVideoBitrate](images/blocks/Media3Player1-getMaxVideoBitrate.png) ![set MaxVideoBitrate](images/blocks/Media3Player1-setMaxVideoBitrate.png)

**MaxVideoBitrate** (read-write): Highest video bitrate to play, in kilobits per second, to save data. 0 has no limit.

![get EqualizerEnabled](images/blocks/Media3Player1-getEqualizerEnabled.png) ![set EqualizerEnabled](images/blocks/Media3Player1-setEqualizerEnabled.png)

**EqualizerEnabled** (read-write): Turns on the equalizer.

![get BassBoost](images/blocks/Media3Player1-getBassBoost.png) ![set BassBoost](images/blocks/Media3Player1-setBassBoost.png)

**BassBoost** (read-write): Strength of the bass boost, from 0 (off) to 100.

![get LoudnessBoost](images/blocks/Media3Player1-getLoudnessBoost.png) ![set LoudnessBoost](images/blocks/Media3Player1-setLoudnessBoost.png)

**LoudnessBoost** (read-write): Makes quiet media louder, in decibels from 0 (off) to 20.

![get Mono](images/blocks/Media3Player1-getMono.png) ![set Mono](images/blocks/Media3Player1-setMono.png)

**Mono** (read-write): Plays stereo audio as mono, the same in both ears.

![get Balance](images/blocks/Media3Player1-getBalance.png) ![set Balance](images/blocks/Media3Player1-setBalance.png)

**Balance** (read-write): Balance between the left and right channels, from -100 (left only) to 100 (right only).

![get PreloadNext](images/blocks/Media3Player1-getPreloadNext.png) ![set PreloadNext](images/blocks/Media3Player1-setPreloadNext.png)

**PreloadNext** (read-write): Seconds of the next media in the queue to load in advance, so it starts without waiting. 0 loads nothing in advance.

![get AutoAdvance](images/blocks/Media3Player1-getAutoAdvance.png) ![set AutoAdvance](images/blocks/Media3Player1-setAutoAdvance.png)

**AutoAdvance** (read-write): Plays the next media when one ends. When off, the player stops after each media and offers to replay it or play the next one.

![get UpNextCountdown](images/blocks/Media3Player1-getUpNextCountdown.png) ![set UpNextCountdown](images/blocks/Media3Player1-setUpNextCountdown.png)

**UpNextCountdown** (read-write): Seconds the up next countdown runs before playing the next media. 0 plays it right away.

![get PositionUpdateInterval](images/blocks/Media3Player1-getPositionUpdateInterval.png) ![set PositionUpdateInterval](images/blocks/Media3Player1-setPositionUpdateInterval.png)

**PositionUpdateInterval** (read-write): Milliseconds between PositionChanged events while media plays. 0 sends none.

## Properties: Advanced

Also in the designer, under Advanced.

![get UserAgent](images/blocks/Media3Player1-getUserAgent.png) ![set UserAgent](images/blocks/Media3Player1-setUserAgent.png)

**UserAgent** (read-write): User agent sent with media requests. Leave empty for the default.

![get TargetLiveOffset](images/blocks/Media3Player1-getTargetLiveOffset.png) ![set TargetLiveOffset](images/blocks/Media3Player1-setTargetLiveOffset.png)

**TargetLiveOffset** (read-write): How far behind the live edge live streams play, in milliseconds. 0 uses the stream's own setting. Applies to media added afterwards.

![get CacheSize](images/blocks/Media3Player1-getCacheSize.png) ![set CacheSize](images/blocks/Media3Player1-setCacheSize.png)

**CacheSize** (read-write): Megabytes of streamed media kept on the device so it plays again without downloading. 0 turns the cache off. The size can't change while the app runs once media was cached.

## Properties: Phone volume

The phone's media volume, shared with other apps.

![get DeviceVolume](images/blocks/Media3Player1-getDeviceVolume.png) ![set DeviceVolume](images/blocks/Media3Player1-setDeviceVolume.png)

**DeviceVolume** (read-write): The phone's media volume, shared by all apps, between DeviceMinVolume and DeviceMaxVolume. Volume sets only this player's volume.

![get DeviceMuted](images/blocks/Media3Player1-getDeviceMuted.png) ![set DeviceMuted](images/blocks/Media3Player1-setDeviceMuted.png)

**DeviceMuted** (read-write): Whether the phone's media volume is muted. Muted silences only this player.

## Properties: Read-only

State of the player and the current media.

![get IsLive](images/blocks/Media3Player1-getIsLive.png)

**IsLive** (read-only): Whether the current media is a live stream.

![get LiveOffset](images/blocks/Media3Player1-getLiveOffset.png)

**LiveOffset** (read-only): How far behind the live edge the live stream plays, in milliseconds, or 0.

![get VideoQualities](images/blocks/Media3Player1-getVideoQualities.png)

**VideoQualities** (read-only): Video qualities of the current media, starting with Auto.

![get AudioTracks](images/blocks/Media3Player1-getAudioTracks.png)

**AudioTracks** (read-only): Audio tracks of the current media.

![get SubtitleTracks](images/blocks/Media3Player1-getSubtitleTracks.png)

**SubtitleTracks** (read-only): Subtitles of the current media, starting with Off.

![get EqualizerPresets](images/blocks/Media3Player1-getEqualizerPresets.png)

**EqualizerPresets** (read-only): Names of the equalizer presets the device offers, like Rock or Jazz.

![get EqualizerPreset](images/blocks/Media3Player1-getEqualizerPreset.png)

**EqualizerPreset** (read-only): Name of the equalizer preset in use, or empty after setting band levels.

![get EqualizerBands](images/blocks/Media3Player1-getEqualizerBands.png)

**EqualizerBands** (read-only): Center frequency of each equalizer band in hertz, from low to high.

![get EqualizerMinLevel](images/blocks/Media3Player1-getEqualizerMinLevel.png)

**EqualizerMinLevel** (read-only): Lowest level an equalizer band can be set to, in decibels.

![get EqualizerMaxLevel](images/blocks/Media3Player1-getEqualizerMaxLevel.png)

**EqualizerMaxLevel** (read-only): Highest level an equalizer band can be set to, in decibels.

![get Downloads](images/blocks/Media3Player1-getDownloads.png)

**Downloads** (read-only): Urls of the downloaded and downloading media.

![get CacheUsed](images/blocks/Media3Player1-getCacheUsed.png)

**CacheUsed** (read-only): Megabytes the cache currently uses on the device.

![get DeviceMinVolume](images/blocks/Media3Player1-getDeviceMinVolume.png)

**DeviceMinVolume** (read-only): Lowest value of DeviceVolume.

![get DeviceMaxVolume](images/blocks/Media3Player1-getDeviceMaxVolume.png)

**DeviceMaxVolume** (read-only): Highest value of DeviceVolume.

![get QueueLength](images/blocks/Media3Player1-getQueueLength.png)

**QueueLength** (read-only): Number of media in the queue.

![get Queue](images/blocks/Media3Player1-getQueue.png)

**Queue** (read-only): Titles of the media in the queue, in order.

![get CurrentIndex](images/blocks/Media3Player1-getCurrentIndex.png)

**CurrentIndex** (read-only): Position of the current media in the queue, starting at 1, or 0 if the queue is empty.

![get HasNext](images/blocks/Media3Player1-getHasNext.png)

**HasNext** (read-only): Whether there is media after the current one.

![get HasPrevious](images/blocks/Media3Player1-getHasPrevious.png)

**HasPrevious** (read-only): Whether there is media before the current one.

![get BufferedPosition](images/blocks/Media3Player1-getBufferedPosition.png)

**BufferedPosition** (read-only): Position up to which media has loaded, in milliseconds.

![get BufferedPercentage](images/blocks/Media3Player1-getBufferedPercentage.png)

**BufferedPercentage** (read-only): How much of the current media has loaded, from 0 to 100.

![get VideoBitrate](images/blocks/Media3Player1-getVideoBitrate.png)

**VideoBitrate** (read-only): Bitrate of the playing video in kilobits per second, or 0 if unknown.

![get VideoWidth](images/blocks/Media3Player1-getVideoWidth.png)

**VideoWidth** (read-only): Width of the playing video in pixels.

![get VideoHeight](images/blocks/Media3Player1-getVideoHeight.png)

**VideoHeight** (read-only): Height of the playing video in pixels.

![get DroppedFrames](images/blocks/Media3Player1-getDroppedFrames.png)

**DroppedFrames** (read-only): Number of video frames dropped because the device couldn't keep up.

![get BandwidthEstimate](images/blocks/Media3Player1-getBandwidthEstimate.png)

**BandwidthEstimate** (read-only): Estimated network speed in kilobits per second, or 0 if unknown.

![get Artist](images/blocks/Media3Player1-getArtist.png)

**Artist** (read-only): Artist of the current media.

![get IsPlaying](images/blocks/Media3Player1-getIsPlaying.png)

**IsPlaying** (read-only): Whether media is playing.

![get PlaybackState](images/blocks/Media3Player1-getPlaybackState.png)

**PlaybackState** (read-only): The playback state: IDLE, BUFFERING, READY or ENDED.

![get IsDialogShowing](images/blocks/Media3Player1-getIsDialogShowing.png)

**IsDialogShowing** (read-only): Whether the player is shown in a dialog.

![get IsFullscreen](images/blocks/Media3Player1-getIsFullscreen.png)

**IsFullscreen** (read-only): Whether the player is fullscreen.

![get Position](images/blocks/Media3Player1-getPosition.png)

**Position** (read-only): Current position in milliseconds.

![get Duration](images/blocks/Media3Player1-getDuration.png)

**Duration** (read-only): Duration of the current media in milliseconds, or 0 if unknown.

![get Title](images/blocks/Media3Player1-getTitle.png)

**Title** (read-only): Title of the current media.

## Dropdowns

Choices for blocks and media item fields.

![ClippingConfigurationField](images/blocks/helper-ClippingConfigurationField.png)

**ClippingConfigurationField:** StartPositionMs, EndPositionMs

![DrmConfigurationField](images/blocks/helper-DrmConfigurationField.png)

**DrmConfigurationField:** Scheme, LicenseUri, LicenseRequestHeaders, ClearKeys

![DrmScheme](images/blocks/helper-DrmScheme.png)

**DrmScheme:** Widevine, ClearKey, PlayReady

![FullscreenOrientation](images/blocks/helper-FullscreenOrientation.png)

**FullscreenOrientation:** Auto, Landscape, Portrait, Sensor

![MediaItemField](images/blocks/helper-MediaItemField.png)

**MediaItemField:** Uri, Title, Artist, Description, ArtworkUri, MimeType, DrmConfiguration, SubtitleConfigurations, ClippingConfiguration, Thumbnails

![MediaType](images/blocks/helper-MediaType.png)

**MediaType:** Auto, Hls, Dash, SmoothStreaming, Rtsp, Progressive

![RepeatMode](images/blocks/helper-RepeatMode.png)

**RepeatMode:** Off, One, All

![ResizeMode](images/blocks/helper-ResizeMode.png)

**ResizeMode:** Fit, Fill, Zoom, FixedWidth, FixedHeight

![SeekMode](images/blocks/helper-SeekMode.png)

**SeekMode:** Exact, ClosestKeyframe

![SubtitleConfigurationField](images/blocks/helper-SubtitleConfigurationField.png)

**SubtitleConfigurationField:** Uri, Language, Label

![SubtitleEdge](images/blocks/helper-SubtitleEdge.png)

**SubtitleEdge:** None, Outline, DropShadow, Raised, Depressed

![ThumbnailsField](images/blocks/helper-ThumbnailsField.png)

**ThumbnailsField:** Uri, Columns, Rows, IntervalMs

## Deprecated

Hidden from the blocks drawer and kept working for existing projects. They change the most recently added media; use media items instead: `AddSubtitle`, `ClipMedia`, `SetClearKeys`, `SetDrm`, `SetMediaType`, `SetMetadata`, `SetThumbnailTrack`, `SetThumbnails`.
