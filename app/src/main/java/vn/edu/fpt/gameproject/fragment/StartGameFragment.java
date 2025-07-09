package vn.edu.fpt.gameproject.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.gameproject.CustomBoardActivity;
import vn.edu.fpt.gameproject.PlayActivity;
import vn.edu.fpt.gameproject.R;
import vn.edu.fpt.gameproject.RoomListActivity;
import vn.edu.fpt.gameproject.fragment.BoardSelectionFragment;
import vn.edu.fpt.gameproject.model.BoardState;
import vn.edu.fpt.gameproject.model.Constants;

public class StartGameFragment extends Fragment
        implements BoardSelectionFragment.OnBoardSelectedListener {

    private SeekBar sliderBoardSize;
    private TextView boardSizeValue;
    private RadioGroup gameModeGroup;
    private RadioGroup difficultyGroup;
    private List<BoardState> savedBoards = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_start_game, container, false);

        // Initialize views
        sliderBoardSize = view.findViewById(R.id.sliderBoardSize);
        boardSizeValue = view.findViewById(R.id.boardSizeValue);
        gameModeGroup = view.findViewById(R.id.gameModeGroup);
        difficultyGroup = view.findViewById(R.id.difficultyGroup);
        Button playButton = view.findViewById(R.id.play_btn);

        // Update size text when slider changes
        sliderBoardSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int boardSize = Math.max(6, progress);
                boardSizeValue.setText(boardSize + "x" + boardSize);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Show/hide difficulty options
        gameModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            difficultyGroup.setVisibility(checkedId == R.id.rbVsAI ? View.VISIBLE : View.GONE);
        });

        playButton.setOnClickListener(btnView -> showBoardSelection());

        return view;
    }

    private void loadSavedBoards() {
        savedBoards.clear();
        File dir = new File(requireActivity().getFilesDir(), Constants.SAVED_BOARDS_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                Gson gson = new Gson();
                for (File file : files) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.FileReader(file))) {

                        StringBuilder content = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line);
                        }

                        BoardState boardState = gson.fromJson(content.toString(), BoardState.class);
                        savedBoards.add(boardState);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String getSelectedGameMode() {
        int selectedId = gameModeGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.rbVsAI) return "ai";
        if (selectedId == R.id.rbWifi1v1) return "wifi";
        return "local";
    }

    private String getSelectedDifficulty() {
        int difficultyId = difficultyGroup.getCheckedRadioButtonId();
        if (difficultyId == R.id.rbEasy) return "easy";
        if (difficultyId == R.id.rbHard) return "hard";
        return "medium";
    }

    private void showBoardSelection() {
        loadSavedBoards();
        String gameMode = getSelectedGameMode();

        if ("wifi".equals(gameMode)) {
            // Skip board selection for WiFi mode
            onDefaultGameSelected();
        } else {
            if (savedBoards.isEmpty()) {
                onDefaultGameSelected();
            } else {
                BoardSelectionFragment fragment = BoardSelectionFragment.newInstance(savedBoards);
                fragment.setListener(this);
                FragmentManager fragmentManager = getParentFragmentManager();
                fragment.show(fragmentManager, "boardSelection");
            }
        }
    }

    @Override
    public void onBoardSelected(BoardState board) {
        String gameMode = getSelectedGameMode();

        if ("wifi".equals(gameMode)) {
            Intent intent = new Intent(getActivity(), RoomListActivity.class);
            intent.putExtra("CUSTOM_BOARD", new Gson().toJson(board));
            startActivity(intent);
        } else {
            Intent intent = new Intent(getActivity(), PlayActivity.class);
            intent.putExtra("BOARD_STATE", new Gson().toJson(board));
            intent.putExtra("GAME_MODE", gameMode);
            intent.putExtra("AI_DIFFICULTY", getSelectedDifficulty());
            startActivity(intent);
        }
    }

    @Override
    public void onDefaultGameSelected() {
        String gameMode = getSelectedGameMode();

        Intent intent;
        if ("wifi".equals(gameMode)) {
            intent = new Intent(getActivity(), RoomListActivity.class);
        } else {
            intent = new Intent(getActivity(), PlayActivity.class);
            intent.putExtra("BOARD_SIZE", sliderBoardSize.getProgress());
            intent.putExtra("GAME_MODE", gameMode);
            intent.putExtra("AI_DIFFICULTY", getSelectedDifficulty());
        }
        startActivity(intent);
    }
}