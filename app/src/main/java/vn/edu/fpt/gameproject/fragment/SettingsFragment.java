package vn.edu.fpt.gameproject.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import vn.edu.fpt.gameproject.R;

public class SettingsFragment extends Fragment {
    private static final String PREFS_NAME = "ChessSettings";
    private static final String KEY_FAIRY_PIECES = "fairy_pieces";
    private static final String KEY_EN_PASSANT = "en_passant";
    private static final String KEY_PROMOTION = "promotion";
    private static final String KEY_CASTLING = "castling";
    private static final String KEY_RIVER = "river";
    private static final String KEY_MUSIC_VOLUME = "background";
    private static final String KEY_SOUND_VOLUME = "effect";

    private Switch switchFairyPieces;
    private Switch switchEnPassant;
    private Switch switchPromotion;
    private Switch switchCastling;
    private Switch switchRiver;
    private SeekBar seekBarMusicVolume;
    private SeekBar seekBarSoundVolume;
    private Button btnSave;
    private Button btnDefault;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        switchFairyPieces = view.findViewById(R.id.switch_fairy_pieces);
        switchEnPassant = view.findViewById(R.id.switch_en_passant);
        switchPromotion = view.findViewById(R.id.switch_promotion);
        switchCastling = view.findViewById(R.id.switch_castling);
        switchRiver = view.findViewById(R.id.switch_river);

        //volume
        seekBarSoundVolume = view.findViewById(R.id.seekbar_effect_volume);
        seekBarMusicVolume = view.findViewById(R.id.seekbar_background_volume);

        btnSave = view.findViewById(R.id.btn_save);
        btnDefault = view.findViewById(R.id.btn_default);

        // Load saved settings
        loadSettings();

        // Set up button click listeners
        btnSave.setOnClickListener(v -> saveSettings());
        btnDefault.setOnClickListener(v -> restoreDefaultSettings());
    }

    private void loadSettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load settings with default values (all true)
        switchFairyPieces.setChecked(prefs.getBoolean(KEY_FAIRY_PIECES, true));
        switchEnPassant.setChecked(prefs.getBoolean(KEY_EN_PASSANT, true));
        switchPromotion.setChecked(prefs.getBoolean(KEY_PROMOTION, true));
        switchCastling.setChecked(prefs.getBoolean(KEY_CASTLING, true));
        switchRiver.setChecked(prefs.getBoolean(KEY_RIVER, false));

        //load volume
        seekBarMusicVolume.setProgress(prefs.getInt(KEY_MUSIC_VOLUME, 50));
        seekBarSoundVolume.setProgress(prefs.getInt(KEY_SOUND_VOLUME, 70));
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit();

        editor.putBoolean(KEY_FAIRY_PIECES, switchFairyPieces.isChecked());
        editor.putBoolean(KEY_EN_PASSANT, switchEnPassant.isChecked());
        editor.putBoolean(KEY_PROMOTION, switchPromotion.isChecked());
        editor.putBoolean(KEY_CASTLING, switchCastling.isChecked());
        editor.putBoolean(KEY_RIVER, switchRiver.isChecked());

        editor.putInt(KEY_MUSIC_VOLUME, seekBarMusicVolume.getProgress());
        editor.putInt(KEY_SOUND_VOLUME, seekBarSoundVolume.getProgress());
        editor.apply();

        Toast.makeText(getContext(), "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void restoreDefaultSettings() {
        // Set all switches to true (default)
        switchFairyPieces.setChecked(true);
        switchEnPassant.setChecked(true);
        switchPromotion.setChecked(true);
        switchCastling.setChecked(true);
        switchRiver.setChecked(false);

        seekBarMusicVolume.setProgress(50);
        seekBarSoundVolume.setProgress(70);
        Toast.makeText(getContext(), "Default settings restored", Toast.LENGTH_SHORT).show();
    }

    // Helper method to get the current settings
    public static boolean getFairyPiecesEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_FAIRY_PIECES, true);
    }

    public static boolean getEnPassantEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_EN_PASSANT, true);
    }

    public static boolean getPromotionEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PROMOTION, true);
    }

    public static boolean getCastlingEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_CASTLING, true);
    }

    public static boolean getRiverEnabled(Context context){
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_RIVER, false);
    }

    public static int getMusicVolume(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_MUSIC_VOLUME, 50);
    }

    public static int getSoundVolume(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SOUND_VOLUME, 70);
    }
}