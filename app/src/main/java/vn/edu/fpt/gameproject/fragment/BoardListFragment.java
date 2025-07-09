package vn.edu.fpt.gameproject.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.gameproject.CustomBoardActivity;
import vn.edu.fpt.gameproject.R;
import vn.edu.fpt.gameproject.model.BoardState;
import vn.edu.fpt.gameproject.model.Constants;

public class BoardListFragment extends Fragment {
    private List<BoardState> savedBoards = new ArrayList<>();
    private BoardListAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_board_list, container, false);

        Button createNewBoardBtn = view.findViewById(R.id.create_new_board_btn);
        createNewBoardBtn.setOnClickListener(v -> showCreateBoardDialog());

        RecyclerView recyclerView = view.findViewById(R.id.board_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BoardListAdapter();
        recyclerView.setAdapter(adapter);

        loadSavedBoards();

        return view;
    }

    private void showCreateBoardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Create New Board");

        // Inflate custom layout
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_board_size, null);
        SeekBar sliderBoardSize = dialogView.findViewById(R.id.sliderBoardSize);
        TextView boardSizeValue = dialogView.findViewById(R.id.boardSizeValue);
        Button createBtn = dialogView.findViewById(R.id.create_board_btn);

        sliderBoardSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int boardSize = Math.max(6, progress);
                boardSizeValue.setText(boardSize + "x" + boardSize);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        createBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CustomBoardActivity.class);
            intent.putExtra("BOARD_SIZE", sliderBoardSize.getProgress());
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();
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
        adapter.notifyDataSetChanged();
    }

    private void showRenameDialog(BoardState board, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Rename Board");

        final EditText input = new EditText(getActivity());
        input.setText(board.getName());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                renameBoard(board, newName, position);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void renameBoard(BoardState board, String newName, int position) {
        File dir = new File(requireActivity().getFilesDir(), Constants.SAVED_BOARDS_DIR);
        File oldFile = new File(dir, board.getName() + ".json");
        File newFile = new File(dir, newName + ".json");

        if (oldFile.exists() && oldFile.renameTo(newFile)) {
            board.setName(newName);
            adapter.notifyItemChanged(position);
            Toast.makeText(getActivity(), "Board renamed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Failed to rename board", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation(BoardState board, int position) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Delete Board")
                .setMessage("Are you sure you want to delete this board?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteBoard(board, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedBoards();
    }

    private void deleteBoard(BoardState board, int position) {
        File dir = new File(requireActivity().getFilesDir(), Constants.SAVED_BOARDS_DIR);
        File file = new File(dir, board.getName() + ".json");
        if (file.exists()) {
            if (file.delete()) {
                savedBoards.remove(position);
                adapter.notifyItemRemoved(position);
                Toast.makeText(getActivity(), "Board deleted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class BoardListAdapter extends RecyclerView.Adapter<BoardListAdapter.BoardViewHolder> {

        @NonNull
        @Override
        public BoardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_board, parent, false);
            return new BoardViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BoardViewHolder holder, int position) {
            BoardState board = savedBoards.get(position);
            holder.nameText.setText(board.getName());
            holder.sizeText.setText(board.getBoardSize() + "x" + board.getBoardSize());

            holder.itemView.setOnClickListener(v -> {
                showRenameDialog(board, position);
            });

            holder.editBtn.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), CustomBoardActivity.class);
                intent.putExtra("BOARD_STATE", new Gson().toJson(board));
                startActivity(intent);
            });

            holder.deleteBtn.setOnClickListener(v -> {
                showDeleteConfirmation(board, position);
            });
        }

        @Override
        public int getItemCount() {
            return savedBoards.size();
        }

        class BoardViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            TextView sizeText;
            ImageButton editBtn;
            ImageButton deleteBtn;

            BoardViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.board_name);
                sizeText = itemView.findViewById(R.id.board_size);
                editBtn = itemView.findViewById(R.id.edit_btn);
                deleteBtn = itemView.findViewById(R.id.delete_btn);
            }
        }
    }
}