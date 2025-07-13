package vn.edu.fpt.gameproject.manager;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.SparseIntArray;

import vn.edu.fpt.gameproject.R;
import vn.edu.fpt.gameproject.fragment.SettingsFragment;

public class AudioManager {
    private MediaPlayer backgroundMusic;
    private SoundPool soundPool;
    private SparseIntArray soundMap;
    private Context context;

    public static final int SOUND_MOVE = R.raw.move_sound;
    public static final int SOUND_CAPTURE = R.raw.capture_sound;
    public static final int SOUND_CHECK = R.raw.check_sound;
    public static final int SOUND_PROMOTE = R.raw.promote_sound;
    public static final int SOUND_WIN = R.raw.win_sound;
    public static final int SOUND_LOSE = R.raw.lose_sound;

    // Volume control
    private float musicVolume = 0.5f;
    private float soundVolume = 0.7f;

    public AudioManager(Context context) {
        this.context = context;
        initializeSoundPool();
        loadSounds();
        updateSettings();
    }

    private void initializeSoundPool() {
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .build();
        soundMap = new SparseIntArray();
    }

    private void loadSounds() {
        soundMap.put(SOUND_MOVE, soundPool.load(context, SOUND_MOVE, 1));
        soundMap.put(SOUND_CAPTURE, soundPool.load(context, SOUND_CAPTURE, 1));
        soundMap.put(SOUND_CHECK, soundPool.load(context, SOUND_CHECK, 1));
        soundMap.put(SOUND_PROMOTE, soundPool.load(context, SOUND_PROMOTE, 1));
        soundMap.put(SOUND_WIN, soundPool.load(context, SOUND_WIN, 1));
        soundMap.put(SOUND_LOSE, soundPool.load(context, SOUND_LOSE, 1));
    }

    public void initializeBackgroundMusic() {
        backgroundMusic = MediaPlayer.create(context, R.raw.background_music);
        backgroundMusic.setLooping(true);
    }

    public void updateSettings() {
        musicVolume = SettingsFragment.getMusicVolume(context) / 100f;
        soundVolume = SettingsFragment.getSoundVolume(context) / 100f;

        if (backgroundMusic != null) {
            backgroundMusic.setVolume(musicVolume, musicVolume);

            // Auto-play/pause based on volume
            if (musicVolume > 0 && !backgroundMusic.isPlaying()) {
                backgroundMusic.start();
            } else if (musicVolume <= 0 && backgroundMusic.isPlaying()) {
                backgroundMusic.pause();
            }
        }
    }

    public void playSound(int soundResId) {
        if (soundVolume > 0 && soundPool != null) {
            soundPool.play(soundMap.get(soundResId), soundVolume, soundVolume, 1, 0, 1);
        }
    }

    public void release() {
        if (backgroundMusic != null) {
            backgroundMusic.release();
            backgroundMusic = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}