package com.example.android.pluralsightnoteskeeper;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by tshepisomogapi on 2019/02/08.
 */

public class NoteRecyclerAdapdter  extends RecyclerView.Adapter<NoteRecyclerAdapdter.ViewHolder>{

    private final Context mContext;
    private final List<NoteInfo> mNotes;

    private final LayoutInflater mLyoutInflater;

    public NoteRecyclerAdapdter(Context mContext, List<NoteInfo> mNotes) {
        this.mContext = mContext;

        mLyoutInflater = LayoutInflater.from(mContext);

        this.mNotes = mNotes;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mLyoutInflater.inflate(R.layout.item_note_list, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        NoteInfo noteInfo = mNotes.get(position);

        holder.mTextCourse.setText(noteInfo.getCourse().getTitle());

        holder.mTextTitle.setText((noteInfo.getTitle()));
    }

    @Override
    public int getItemCount() {
        return mNotes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView mTextCourse;
        public final TextView mTextTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            mTextCourse = (TextView) itemView.findViewById(R.id.text_course);
            mTextTitle = (TextView) itemView.findViewById(R.id.text_note_title);
        }
    }
}
