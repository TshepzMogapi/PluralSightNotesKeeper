package com.example.android.pluralsightnoteskeeper;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import java.util.List;
import com.example.android.pluralsightnoteskeeper.NoteKeeperProviderContract.Notes;
import static com.example.android.pluralsightnoteskeeper.NoteKeeperDatabaseContract.*;

public class NoteActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ORIGINAL_NOTE_COURSE_ID = "com.example.android.pluralsightnoteskeeper.ORIGINAL_NOTE_COURSE_ID";
    public static final String ORIGINAL_NOTE_TITLE = "com.example.android.pluralsightnoteskeeper.ORIGINAL_NOTE_TITLE";
    public static final String ORIGINAL_NOTE_TEXT = "com.example.android.pluralsightnoteskeeper.ORIGINAL_NOTE_TEXT";

    public static final String NOTE_ID = "com.example.android.pluralsightnoteskeeper.NOTE_ID";
    public static final int ID_NOT_SET = -1;
    public static final int LOADER_NOTES = 0;
    public static final int LOADER_COURSES = 1;
    private NoteInfo mNote = new NoteInfo(DataManager.getInstance().getCourses().get(0), "", "");
    private boolean mIsNewNote;
    private Spinner mSpinnerCourses;
    private EditText mTextNoteTitle;
    private EditText mTextNoteText;
    private int mNoteId;
    private boolean mIsCancelling;
    private String mOriginalNoteCourseId;
    private String mOriginalNoteTitle;
    private String mOriginalNoteText;
    private NoteKeeperOpenHelper mDbOpenHelper;
    private Cursor mNoteCursor;
    private int mCourseIdPos;
    private int mNoteTitlePos;
    private int mNoteTextPos;
    private SimpleCursorAdapter mAdapterCourses;
    private boolean mCoursesQueryFinished;
    private boolean mNotesQueryFinished;
    private Uri mNoteUri;

    private final String TAG = getClass().getSimpleName();



    @Override
    protected void onDestroy() {
        mDbOpenHelper.close();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDbOpenHelper = new NoteKeeperOpenHelper(this);

        mSpinnerCourses = (Spinner) findViewById(R.id.spinner_courses);

        mAdapterCourses = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
                new String[]{CourseInfoEntry.COLUMN_COURSE_TITLE},
                new int[]{android.R.id.text1}, 0);

        mAdapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCourses.setAdapter(mAdapterCourses);


        getLoaderManager().initLoader(LOADER_COURSES, null,this);

        readDisplayStateValues();

        if (savedInstanceState == null) {
            saveOriginalValues();
        } else {
            restoreOriginalNotesValues(savedInstanceState);
        }

        mTextNoteTitle = (EditText) findViewById(R.id.text_note_title);

        mTextNoteText = (EditText) findViewById(R.id.text_note_text);

        if (!mIsNewNote) {
//            loadNoteData();
            getLoaderManager().initLoader(LOADER_NOTES, null, this);
        }

    }

    private void loadCourseData() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        String[] courseColumns = {
                CourseInfoEntry.COLUMN_COURSE_TITLE,
                CourseInfoEntry.COLUMN_COURSE_ID,
                CourseInfoEntry._ID
        };

        Cursor cursor = db.query(CourseInfoEntry.TABLE_NAME, courseColumns,
                null, null, null, null, CourseInfoEntry.COLUMN_COURSE_TITLE);

        mAdapterCourses.changeCursor(cursor);
    }

    private void loadNoteData() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

        String courseId = "android_intents";

        String titleStart = "dynamic";

        String selection = NoteInfoEntry._ID + " = ? ";

        String[] selectionArgs = {Integer.toString(mNoteId)};

        String[] noteColumns = {
                NoteInfoEntry.COLUMN_COURSE_ID,
                NoteInfoEntry.COLUMN_NOTE_TITLE,
                NoteInfoEntry.COLUMN_NOTE_TEXT
        };

        mNoteCursor = db.query(NoteInfoEntry.TABLE_NAME, noteColumns,
                selection, selectionArgs, null, null, null);

        mCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);


        mNoteCursor.moveToNext();
        displayNote();
    }

    private void restoreOriginalNotesValues(Bundle savedInstanceState) {
        mOriginalNoteCourseId = savedInstanceState.getString(ORIGINAL_NOTE_COURSE_ID);
        mOriginalNoteTitle = savedInstanceState.getString(ORIGINAL_NOTE_TITLE);
        mOriginalNoteText = savedInstanceState.getString(ORIGINAL_NOTE_TEXT);
    }

    private void saveOriginalValues() {
        if (mIsNewNote)
            return;

        mOriginalNoteCourseId = mNote.getCourse().getCourseId();

        mOriginalNoteTitle = mNote.getTitle();

        mOriginalNoteText = mNote.getText();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ORIGINAL_NOTE_COURSE_ID, mOriginalNoteCourseId);
        outState.putString(ORIGINAL_NOTE_TITLE, mOriginalNoteTitle);
        outState.putString(ORIGINAL_NOTE_TEXT, mOriginalNoteText);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsCancelling) {
            if (mIsNewNote) {
                deleteNoteFromDatabase();
            } else {
                storePreviousNoteValues();
            }

        } else {

            saveNote();

        }
    }

    private void deleteNoteFromDatabase() {
        final String selection = NoteInfoEntry._ID + " = ?";

        final String[] selectionArgs ={Integer.toString(mNoteId)};

        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {

                SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();

                db.delete(NoteInfoEntry.TABLE_NAME, selection, selectionArgs);
                return null;
            }
        };

        task.execute();



    }

    private void storePreviousNoteValues() {
        CourseInfo course = DataManager.getInstance().getCourse(mOriginalNoteCourseId);
        mNote.setCourse(course);
        mNote.setTitle(mOriginalNoteTitle);
        mNote.setText(mOriginalNoteText);
    }

    private void saveNote() {

        String courseId = selectedCourseId();

        String noteTitle = mTextNoteTitle.getText().toString();

        String noteText = mTextNoteText.getText().toString();

        saveNoteToDatabase(courseId, noteTitle, noteText);


    }

    private String selectedCourseId() {

        int selectedPosition = mSpinnerCourses.getSelectedItemPosition();

        Cursor cursor = mAdapterCourses.getCursor();

        cursor.moveToPosition(selectedPosition);

        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);

        String courseId = cursor.getString(courseIdPos);

        return courseId;

    }

    private void saveNoteToDatabase(String courseId, String noteTitle, String noteText){
        String selection = NoteInfoEntry._ID + " = ?";
        String[] selectionArgs = {Integer.toString(mNoteId)};

        ContentValues values = new ContentValues();

        values.put(NoteInfoEntry.COLUMN_COURSE_ID, courseId);
        values.put(NoteInfoEntry.COLUMN_NOTE_TITLE, noteTitle);
        values.put(NoteInfoEntry.COLUMN_NOTE_TEXT, noteText);

        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();

        db.update(NoteInfoEntry.TABLE_NAME, values, selection, selectionArgs);



    }

    private void displayNote() {
        String courseId = mNoteCursor.getString(mCourseIdPos);
        String noteTitle = mNoteCursor.getString(mNoteTitlePos);
        String noteText = mNoteCursor.getString(mNoteTextPos);


        int courseIndex = getIndexOfCourseId(courseId);

        mSpinnerCourses.setSelection(courseIndex);

        mTextNoteTitle.setText(noteTitle);

        mTextNoteText.setText(noteText);
    }

    private int getIndexOfCourseId(String courseId) {
        Cursor cursor = mAdapterCourses.getCursor();
        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        int courseRowIndex = 0;
        boolean more = cursor.moveToFirst();

        while (more) {
            String cursorCourseId = cursor.getString(courseIdPos);

            if (courseId.equals(cursorCourseId))
                break;

            courseRowIndex++;
            more = cursor.moveToNext();
        }
        return courseRowIndex;
    }

    private void readDisplayStateValues() {
        Intent intent = getIntent();

        mNoteId = intent.getIntExtra(NOTE_ID, ID_NOT_SET);

        mIsNewNote = mNoteId == ID_NOT_SET;

        if (mIsNewNote) {

            createNewNote();
        }

//        mNote = DataManager.getInstance().getNotes().get(mNoteId);


    }

    private void createNewNote() {

        AsyncTask<ContentValues, Integer, Uri> task =
                new AsyncTask<ContentValues, Integer, Uri>() {

            private ProgressBar mProgressBar;

                    @Override
                    protected void onPreExecute() {
                        mProgressBar =findViewById(R.id.progress_bar);
                        mProgressBar.setVisibility(View.VISIBLE);

                        mProgressBar.setProgress(1);
                    }

                    @Override
            protected Uri doInBackground(ContentValues... params) {
                ContentValues insertValues = params[0];
                Uri rowUri = getContentResolver()
                        .insert(Notes.CONTENT_URI, insertValues);

                        simulateLongRunningWork();
                        publishProgress(2);

                        simulateLongRunningWork();
                        publishProgress(3);

                        return rowUri;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                        int progressValue = values[0];
                mProgressBar.setProgress(progressValue);


                super.onProgressUpdate(values);
            }

                    @Override
            protected void onPostExecute(Uri uri) {
                Log.d(TAG, "onPostExecute - thread: " + Thread.currentThread().getId());

                mNoteUri = uri;

                displaySnackbar(mNoteUri.toString());
                mProgressBar.setVisibility(View.GONE);
            }
        };

        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, "");
        values.put(Notes.COLUMN_NOTE_TITLE, "");
        values.put(Notes.COLUMN_NOTE_TEXT, "");

        Log.d(TAG, "Call to execute - thread : " + Thread.currentThread().getId());

        task.execute(values);

    }

    private void displaySnackbar(String message) {
        View view = findViewById(R.id.spinner_courses);
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    private void simulateLongRunningWork() {
        try {
            Thread.sleep(2000);
        } catch(Exception ex) {}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_Send_email) {

            sendEmail();
            return true;
        } else if (id == R.id.action_cancel) {

            mIsCancelling = true;
            finish();
        } else if (id == R.id.action_next) {

            moveNext();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_next);

        int lastNoteIndex = DataManager.getInstance().getNotes().size() - 1;

        item.setEnabled(mNoteId < lastNoteIndex);
        return super.onPrepareOptionsMenu(menu);
    }

    private void moveNext() {
        saveNote();

        ++mNoteId;
        mNote = DataManager.getInstance().getNotes().get(mNoteId);

        saveOriginalValues();
        displayNote();
        invalidateOptionsMenu();
    }

    private void sendEmail() {
        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();

        String text = "Stuff that I Learned on PluralsSight course " +
                course.getTitle() + "\n" + mTextNoteText.getText().toString();

        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("message/rfc2822");

        intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        intent.putExtra(Intent.EXTRA_TEXT, text);

        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        CursorLoader loader = null;

        if (id == LOADER_NOTES){
            loader = createLoaderNotes();
        } else if (id == LOADER_COURSES) {
            loader = createLoaderCourses();

        }

        return loader;
    }

    private CursorLoader createLoaderCourses() {
        mCoursesQueryFinished = false;

        return new CursorLoader(this) {
            @Override
            public Cursor loadInBackground() {
                SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
                String[] courseColumns = {
                        CourseInfoEntry.COLUMN_COURSE_TITLE,
                        CourseInfoEntry.COLUMN_COURSE_ID,
                        CourseInfoEntry._ID
                };

               return db.query(CourseInfoEntry.TABLE_NAME, courseColumns,
                        null, null, null, null, CourseInfoEntry.COLUMN_COURSE_TITLE);

            }
        };
    }

    private CursorLoader createLoaderNotes() {
        mNotesQueryFinished = false;

        return new CursorLoader(this) {
            @Override
            public Cursor loadInBackground() {

                SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();


                String selection = NoteInfoEntry._ID + " = ? ";

                String[] selectionArgs = {Integer.toString(mNoteId)};

                String[] noteColumns = {
                        NoteInfoEntry.COLUMN_COURSE_ID,
                        NoteInfoEntry.COLUMN_NOTE_TITLE,
                        NoteInfoEntry.COLUMN_NOTE_TEXT
                };

                return db.query(NoteInfoEntry.TABLE_NAME, noteColumns,
                        selection, selectionArgs, null, null, null);


            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (loader.getId() == LOADER_NOTES) {
            loadFinishedNotes(data);
        } else if (loader.getId() == LOADER_COURSES) {
            mAdapterCourses.changeCursor(data);

            mCoursesQueryFinished = true;

            displayNoteWhenQueriesFinished();
        }


    }

    private void loadFinishedNotes(Cursor data) {
        mNoteCursor = data;

        mCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);


        mNoteCursor.moveToNext();

        mNotesQueryFinished = true;
        
        displayNoteWhenQueriesFinished();
    }

    private void displayNoteWhenQueriesFinished() {

        if (mNotesQueryFinished && mCoursesQueryFinished)
            displayNote();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == LOADER_NOTES) {
            if (mNoteCursor != null) {
                mNoteCursor.close();
            }
        } else if (loader.getId() == LOADER_COURSES) {
            mAdapterCourses.changeCursor(null);
        }

    }
}
