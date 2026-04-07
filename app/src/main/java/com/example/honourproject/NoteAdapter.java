package com.example.honourproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

//Class Summary
//Gets a list of notes and displays one in each row
//Shows the title, and a preview of note content

//Display list of notes in recyclerview, shows 1 note per row
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private List<Note> notes; //list of notes to display
    private OnNoteClickListener listener; //handles when notes are clicked

    //interface for clicking on a note
    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    //note constructor
    public NoteAdapter(List<Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    //creates a view holder
    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    //fills a row with data from the note
    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        //sets note title
        holder.titleTextView.setText(note.noteTitle);

        //shows first 100 characters of a note
        String contentPreview = note.noteContent.length() > 100 ?
                note.noteContent.substring(0, 100) + "..." : note.noteContent;
        holder.contentTextView.setText(contentPreview);

        //formats date and shows creation date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.dateTextView.setText(sdf.format(new Date(note.noteCreated)));

        //shows lock icon if note is encrypted
        holder.lockIcon.setVisibility(note.isEncrypted ? View.VISIBLE : View.GONE);

        //show self destruct countdown
        if (note.noteDelete > 0) {
            long timeLeft = note.noteDelete - System.currentTimeMillis();
            if (timeLeft > 0) {
                String timeText = formatTimeLeft(timeLeft);
                holder.deleteTimeTextView.setText("Deletes in: " + timeText);
                holder.deleteTimeTextView.setVisibility(View.VISIBLE);
            } else {
                holder.deleteTimeTextView.setVisibility(View.GONE);
            }
        } else {
            holder.deleteTimeTextView.setVisibility(View.GONE);
        }

        //handles clicking on  anote
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note);
            }
        });
    }

    //convers milliseconds to readable time
    private String formatTimeLeft(long millis) {
        if (millis < 60000) {
            return millis / 1000 + "s";
        } else if (millis < 3600000) {
            return millis / 60000 + "m";
        } else if (millis < 86400000) {
            return millis / 3600000 + "h";
        } else {
            return millis / 86400000 + "d";
        }
    }

    //returns how many notes in the list
    @Override
    public int getItemCount() {
        return notes.size();
    }

    //updates list and refreshes display
    public void updateNotes(List<Note> newNotes) {
        notes = newNotes;
        notifyDataSetChanged();
    }

    //holds the view for one note
    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, contentTextView, dateTextView, deleteTimeTextView;
        ImageView lockIcon;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.note_title);
            contentTextView = itemView.findViewById(R.id.note_content);
            dateTextView = itemView.findViewById(R.id.note_date);
            deleteTimeTextView = itemView.findViewById(R.id.delete_time);
            lockIcon = itemView.findViewById(R.id.lock_icon);
        }
    }
}
