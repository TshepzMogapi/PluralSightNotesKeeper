package com.example.android.pluralsightnoteskeeper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by tshepisomogapi on 2019/01/24.
 */
public class DataManagerTest {

    static DataManager sDataManager;

    @BeforeClass
    public static void classSetUp() throws Exception {
        sDataManager = DataManager.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        sDataManager.getNotes().clear();
        sDataManager.initializeExampleNotes();
    }
    @Test
    public void createNewNote() throws Exception {

        final CourseInfo courseInfo = sDataManager.getCourse("android_async");
        final String noteTitle = "Test note title";
        final String noteText = "More text to test the note creation";


        int noteIndex = sDataManager.createNewNote();

        NoteInfo newNote = sDataManager.getNotes().get(noteIndex);

        newNote.setCourse(courseInfo);
        newNote.setTitle(noteTitle);
        newNote.setText(noteText);

        NoteInfo compareNote = sDataManager.getNotes().get(noteIndex);

        assertEquals(courseInfo, compareNote.getCourse());
        assertEquals(noteTitle, compareNote.getTitle());
        assertEquals(noteText, compareNote.getText());


    }

}