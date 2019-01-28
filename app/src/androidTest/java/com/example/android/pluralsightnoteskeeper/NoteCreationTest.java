package com.example.android.pluralsightnoteskeeper;

import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;

import static org.junit.Assert.*;

/**
 * Created by tshepisomogapi on 2019/01/28.
 */
@RunWith(AndroidJUnit4.class)
public class NoteCreationTest {

    @Rule
    public ActivityTestRule<NoteListActivity> mNoteListActivityRule =
            new ActivityTestRule<>(NoteListActivity.class);


    @Test
    public void createNewNote() {


        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.text_note_title)).perform(typeText("Test note title"));
        onView(withId(R.id.text_note_text)).perform(typeText("Body of test note"),
                closeSoftKeyboard());


    }
}