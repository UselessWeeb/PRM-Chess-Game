package vn.edu.fpt.gameproject.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import vn.edu.fpt.gameproject.R;
import vn.edu.fpt.gameproject.model.BoardState;

public class BoardSelectionFragment extends DialogFragment {
    private List<BoardState> savedBoards;
    private OnBoardSelectedListener listener;

    public List<BoardState> getSavedBoards() {
        return savedBoards;
    }

    public void setSavedBoards(List<BoardState> savedBoards) {
        this.savedBoards = savedBoards;
    }

    public OnBoardSelectedListener getListener() {
        return listener;
    }

    public void setListener(OnBoardSelectedListener listener) {
        this.listener = listener;
    }

    public interface OnBoardSelectedListener {
        void onBoardSelected(BoardState board);
        void onDefaultGameSelected();
    }

    public static BoardSelectionFragment newInstance(List<BoardState> savedBoards) {
        BoardSelectionFragment fragment = new BoardSelectionFragment();
        Bundle args = new Bundle();
        // Pass the saved boards using Gson
        Gson gson = new Gson();
        args.putString("savedBoards", gson.toJson(savedBoards));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String json = getArguments().getString("savedBoards");
            Gson gson = new Gson();
            Type type = new TypeToken<List<BoardState>>(){}.getType();
            savedBoards = gson.fromJson(json, type);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_board_selection, null);

        LinearLayout container = view.findViewById(R.id.boards_container);
        Button defaultGameButton = view.findViewById(R.id.default_game_button);

        // Instead of removing all views, we'll remove just the dynamically added ones
        // Keep the button by only removing views after it (if any)
        int childCount = container.getChildCount();
        if (childCount > 1) { // If there's more than just the button
            container.removeViews(1, childCount - 1); // Keep first view (button), remove others
        }

        // Add each board as a clickable TextView
        for (int i = 0; i < savedBoards.size(); i++) {
            BoardState board = savedBoards.get(i);

            // Create item view
            TextView boardItem = new TextView(getContext());
            boardItem.setText(board.getName());
            boardItem.setTextSize(18);
            boardItem.setPadding(16, 16, 16, 16);
            boardItem.setOnClickListener(v -> {
                listener.onBoardSelected(board);
                dismiss();
            });

            // Set proper layout params
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            boardItem.setLayoutParams(params);

            container.addView(boardItem);

            // Add divider after each item except the last one
            if (i < savedBoards.size() - 1) {
                View divider = new View(getContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                ));
                divider.setBackgroundColor(Color.LTGRAY);
                container.addView(divider);
            }
        }

        defaultGameButton.setOnClickListener(v -> {
            listener.onDefaultGameSelected();
            dismiss();
        });

        builder.setView(view)
                .setTitle("Choose Board Configuration");
        return builder.create();
    }
}