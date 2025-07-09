package vn.edu.fpt.gameproject.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import vn.edu.fpt.gameproject.R;
import vn.edu.fpt.gameproject.model.PieceType;

public class PromotionDialogFragment extends DialogFragment {
    public interface OnPieceSelectedListener {
        void onPieceSelected(PieceType piece);
    }

    private OnPieceSelectedListener listener;

    private ImageButton queenPromotion;
    private ImageButton rookPromotion;
    private ImageButton knightPromotion;
    private ImageButton bishopPromotion;
    private ImageButton archbishopPromotion;
    private ImageButton chancellorPromotion;

    public PromotionDialogFragment(OnPieceSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_promotion, null);

        queenPromotion = view.findViewById(R.id.btnQueen);
        rookPromotion = view.findViewById(R.id.btnRook);
        knightPromotion = view.findViewById(R.id.btnKnight);
        bishopPromotion = view.findViewById(R.id.btnBishop);
        archbishopPromotion = view.findViewById(R.id.btnBishopKnight);
        chancellorPromotion = view.findViewById(R.id.btnRookKnight);

        builder.setView(view);

        queenPromotion.setOnClickListener(v -> {
            listener.onPieceSelected(PieceType.QUEEN);
            dismiss();
        });

        rookPromotion.setOnClickListener(v -> {
            listener.onPieceSelected(PieceType.ROOK);
            dismiss();
        });

        bishopPromotion.setOnClickListener(v -> {
            listener.onPieceSelected(PieceType.BISHOP);
            dismiss();
        });

        knightPromotion.setOnClickListener(v -> {
            listener.onPieceSelected(PieceType.KNIGHT);
            dismiss();
        });

        chancellorPromotion.setOnClickListener(v -> {
            listener.onPieceSelected(PieceType.CHANCELLOR);
            dismiss();
        });

        archbishopPromotion.setOnClickListener(v -> {
            listener.onPieceSelected(PieceType.ARCHBISHOP);
            dismiss();
        });

        boolean fairyPiecesEnabled = SettingsFragment.getFairyPiecesEnabled(requireContext());

        if (!fairyPiecesEnabled) {
            archbishopPromotion.setVisibility(View.GONE);
            chancellorPromotion.setVisibility(View.GONE);
        } else {
            archbishopPromotion.setVisibility(View.VISIBLE);
            chancellorPromotion.setVisibility(View.VISIBLE);
        }

        Dialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        return dialog;
    }
}