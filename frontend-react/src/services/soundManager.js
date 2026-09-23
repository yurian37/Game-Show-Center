/**
 * Dedicated Sound Manager for Online Mode (Game Show Center).
 * Acts as the centralized, dedicated sound controller ensuring strict mutual exclusion:
 * - NO two sounds can ever play simultaneously.
 * - Any new sound/track request immediately stops and clears any currently playing audio.
 * - Switching games or navigating screens immediately halts all ongoing audio/music.
 */

class SoundManager {
  constructor() {
    this.activeAudio = null;
    this.activeTrackConfig = null;
  }

  /**
   * Immediately stops any currently playing sound or song and cleans up listeners.
   */
  stopAll() {
    if (this.activeAudio) {
      try {
        this.activeAudio.pause();
        this.activeAudio.ontimeupdate = null;
        this.activeAudio.onended = null;
        this.activeAudio.onerror = null;
        this.activeAudio.onplay = null;
        this.activeAudio.onpause = null;
        this.activeAudio.currentTime = 0;
        this.activeAudio.src = '';
      } catch (err) {
        console.warn('[SoundManager] Error while stopping audio:', err);
      }
      this.activeAudio = null;
      this.activeTrackConfig = null;
    }
  }

  /**
   * Plays a single sound effect (e.g. roulette spin, win fanfare, coin flip).
   * Stops any other active sound or song immediately.
   * 
   * @param {string} src - Audio source URL or imported asset
   * @param {number} volume - Volume between 0.0 and 1.0
   * @returns {HTMLAudioElement|null}
   */
  playSfx(src, volume = 1.0) {
    if (!src) return null;

    // 1. Stop any currently playing audio immediately
    this.stopAll();

    try {
      const audio = new Audio(src);
      audio.volume = Math.max(0, Math.min(1, volume));
      this.activeAudio = audio;

      audio.onended = () => {
        if (this.activeAudio === audio) {
          this.activeAudio = null;
        }
      };

      audio.onerror = (err) => {
        console.warn('[SoundManager] SFX Play Error:', err);
        if (this.activeAudio === audio) {
          this.activeAudio = null;
        }
      };

      const playPromise = audio.play();
      if (playPromise !== undefined) {
        playPromise.catch((err) => {
          console.warn('[SoundManager] Autoplay prevented or error:', err.message);
          if (this.activeAudio === audio) {
            this.activeAudio = null;
          }
        });
      }

      return audio;
    } catch (e) {
      console.error('[SoundManager] Could not instantiate audio:', e);
      return null;
    }
  }

  /**
   * Plays a musical track or segment with optional start/end boundaries and callbacks.
   * Stops any other active sound or song immediately.
   * 
   * @param {string} src - Audio source URL
   * @param {Object} options - Options { startTime, endTime, volume, onTimeUpdate, onEnded, onError, onPlay, onPause }
   * @returns {Promise<HTMLAudioElement>}
   */
  async playTrack(src, options = {}) {
    if (!src) return null;

    // 1. Stop previous audio immediately
    this.stopAll();

    const {
      startTime = 0,
      endTime = null,
      volume = 0.8,
      onTimeUpdate = null,
      onEnded = null,
      onError = null,
      onPlay = null,
      onPause = null
    } = options;

    try {
      const audio = new Audio(src);
      audio.volume = Math.max(0, Math.min(1, volume));
      this.activeAudio = audio;
      this.activeTrackConfig = { src, startTime, endTime, volume };

      audio.currentTime = startTime;

      audio.ontimeupdate = () => {
        if (this.activeAudio !== audio) return;

        if (onTimeUpdate) {
          onTimeUpdate(audio.currentTime, audio.duration);
        }

        // Enforce end time constraint if specified
        if (endTime !== null && audio.currentTime >= endTime) {
          audio.pause();
          audio.currentTime = startTime;
          if (onEnded) onEnded();
        }
      };

      audio.onended = () => {
        if (this.activeAudio !== audio) return;
        audio.currentTime = startTime;
        if (onEnded) onEnded();
      };

      audio.onerror = (e) => {
        if (this.activeAudio !== audio) return;
        console.warn('[SoundManager] Track audio error:', e);
        if (onError) onError(e);
      };

      if (onPlay) audio.onplay = onPlay;
      if (onPause) audio.onpause = onPause;

      await audio.play();
      return audio;
    } catch (err) {
      console.warn('[SoundManager] Error playing track:', err.message);
      if (onError) onError(err);
      throw err;
    }
  }

  /**
   * Pauses the currently active audio.
   */
  pause() {
    if (this.activeAudio && !this.activeAudio.paused) {
      this.activeAudio.pause();
    }
  }

  /**
   * Resumes playback of the active audio.
   */
  async resume() {
    if (this.activeAudio && this.activeAudio.paused) {
      try {
        await this.activeAudio.play();
      } catch (err) {
        console.warn('[SoundManager] Resume playback error:', err.message);
      }
    }
  }

  /**
   * Checks if audio is currently playing.
   */
  isPlaying() {
    return !!(this.activeAudio && !this.activeAudio.paused && !this.activeAudio.ended);
  }

  /**
   * Adjusts volume of the active audio element.
   */
  setVolume(volume) {
    const v = Math.max(0, Math.min(1, parseFloat(volume) || 0));
    if (this.activeAudio) {
      this.activeAudio.volume = v;
    }
    if (this.activeTrackConfig) {
      this.activeTrackConfig.volume = v;
    }
  }

  /**
   * Seeks active audio to a specific time.
   */
  seek(timeInSeconds) {
    if (this.activeAudio) {
      this.activeAudio.currentTime = Math.max(0, parseFloat(timeInSeconds) || 0);
    }
  }

  /**
   * Returns currently active Audio instance.
   */
  getActiveAudio() {
    return this.activeAudio;
  }
}

// Singleton dedicated sound manager instance
export const soundManager = new SoundManager();
export default soundManager;
