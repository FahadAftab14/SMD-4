package com.example.taskflow.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.taskflow.R;

/**
 * Asks the user to type the attachment's name before it is removed.
 * The attachment is removed ONLY when the typed name matches (surrounding spaces are
 * ignored) and Remove is pressed. A wrong name shows an error and keeps the dialog open.
 * If the dialog closes any other way (Cancel, tapping outside, Back), onKept runs so the
 * caller can put a swiped row back.
 */
public final class RemoveAttachmentDialog {

    public interface OnConfirmed {
        void onConfirmed();
    }

    private RemoveAttachmentDialog() {
    }

    /** Version without a "kept" callback (used by the remove icon on a row). */
    public static void show(Context context, String attachmentName, OnConfirmed onConfirmed) {
        show(context, attachmentName, onConfirmed, null);
    }

    /** @param onKept runs when the dialog closes WITHOUT removing the attachment (may be null) */
    public static void show(Context context, final String attachmentName, final OnConfirmed onConfirmed,
                            final Runnable onKept) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_remove_attachment, null);
        TextView tvMessage = view.findViewById(R.id.tvRemoveMessage);
        final EditText etName = view.findViewById(R.id.etRemoveName);
        TextView btnCancel = view.findViewById(R.id.btnRemoveCancel);
        TextView btnRemove = view.findViewById(R.id.btnRemoveConfirm);

        tvMessage.setText("To remove this attachment, type its name exactly:\n\n" + attachmentName);

        final boolean[] removed = {false};
        final AlertDialog dialog = new AlertDialog.Builder(context).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String typed = etName.getText().toString().trim();
                if (!typed.equals(attachmentName)) {
                    etName.setError("That is not the attachment name");
                    return;
                }
                removed[0] = true;
                dialog.dismiss();
                onConfirmed.onConfirmed();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                if (!removed[0] && onKept != null) {
                    onKept.run();
                }
            }
        });
        dialog.show();
    }
}
