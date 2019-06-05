package com.itsajackson.notekeeper;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.List;

public class NoteActivity extends AppCompatActivity {

    private final String LOG_TAG = getClass().getSimpleName();

    public static final String NOTE_POSITION = "com.itsajackson.notekeeper.NOTE_POSITION";
    public static final String ORIGINAL_NOTE_COURSE_ID = "com.itsajackson.notekeeper.ORIGINAL_NOTE_COURSE_ID";
    public static final String ORIGINAL_NOTE_TITLE = "com.itsajackson.notekeeper.ORIGINAL_NOTE_TITLE";
    public static final String ORIGINAL_NOTE_TEXT = "com.itsajackson.notekeeper.ORIGINAL_NOTE_TEXT";

    public static final int POSITION_NOT_SET = -1;
    private NoteInfo note;
    private boolean isNewNote;
    private Spinner spinnerCourses;
    private EditText textNoteTitle;
    private EditText textNoteText;
    private int notePosition;
    private boolean isCancelling;
    private String originalNoteCourseId;
    private String originalNoteTitle;
    private String originalNoteText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        spinnerCourses = findViewById(R.id.spinner_courses);

        List<CourseInfo> courses = DataManager.getInstance().getCourses();
        ArrayAdapter<CourseInfo> adapterCourses = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courses);
        adapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourses.setAdapter(adapterCourses);

        readDisplayStateValues();
        if (savedInstanceState == null) {
            saveOriginalNoteValues();
        } else {
            restoreOriginalNoteValues(savedInstanceState);
        }

        textNoteTitle = findViewById(R.id.text_note_title);
            textNoteText = findViewById(R.id.text_note_text);

        if (!isNewNote) {
            displayNote(spinnerCourses, textNoteTitle, textNoteText);
        }
    }

    private void restoreOriginalNoteValues(Bundle savedInstanceState) {
        originalNoteCourseId = savedInstanceState.getString(ORIGINAL_NOTE_COURSE_ID);
        originalNoteTitle = savedInstanceState.getString(ORIGINAL_NOTE_TITLE);
        originalNoteText = savedInstanceState.getString(ORIGINAL_NOTE_TEXT);
    }

    private void saveOriginalNoteValues() {
        if (isNewNote)
            return;
        originalNoteCourseId = note.getCourse().getCourseId();
        originalNoteTitle = note.getTitle();
        originalNoteText = note.getText();
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "onPause");
        super.onPause();
        if (isCancelling) {
            Log.i(LOG_TAG, "Cancelling note at position: " + notePosition);
            if (isNewNote) {
                DataManager dm = DataManager.getInstance();
                dm.removeNote(notePosition);
            }
            else {
                storePreciousNoteValues();
            }
        } else {
            saveNote();
        }
    }

    private void storePreciousNoteValues() {
        note.setCourse(DataManager.getInstance().getCourse(originalNoteCourseId));
        note.setTitle(originalNoteTitle);
        note.setText(originalNoteText);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ORIGINAL_NOTE_COURSE_ID, originalNoteCourseId);
        outState.putString(ORIGINAL_NOTE_TITLE, originalNoteTitle);
        outState.putString(ORIGINAL_NOTE_TEXT, originalNoteText);
    }

    private void saveNote() {
        note.setCourse((CourseInfo) spinnerCourses.getSelectedItem());
        note.setTitle(textNoteTitle.getText().toString());
        note.setText(textNoteText.getText().toString());
    }

    private void displayNote(Spinner spinnerCourses, EditText textNoteTitle, EditText textNoteText) {
        List<CourseInfo> courses = DataManager.getInstance().getCourses();
        int courseIndex = courses.indexOf(note.getCourse());

        spinnerCourses.setSelection(courseIndex);
        textNoteTitle.setText(note.getTitle());
        textNoteText.setText(note.getText());
    }

    private void readDisplayStateValues() {
        Intent intent = getIntent();
        notePosition = intent.getIntExtra(NOTE_POSITION, POSITION_NOT_SET);
        isNewNote = notePosition == POSITION_NOT_SET;
        if (isNewNote) {
            createNewNote();
        }

        Log.i(LOG_TAG, "Reading note at position " + notePosition);
        note = DataManager.getInstance().getNotes().get(notePosition);
    }

    private void createNewNote() {
        DataManager dm = DataManager.getInstance();
        notePosition = dm.createNewNote();
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
        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        } else if (id == R.id.action_cancel) {
            isCancelling = true;
            finish();
        } else if (id == R.id.action_previous) {
            movePrevious();
        } else if (id == R.id.action_next) {
            moveNext();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem menuItemPrevious = menu.findItem(R.id.action_previous);
        menuItemPrevious.setEnabled(notePosition > 0);

        MenuItem menuItemNext = menu.findItem(R.id.action_next);
        int lastNoteIndex = DataManager.getInstance().getNotes().size() - 1;
        menuItemNext.setEnabled(notePosition < lastNoteIndex);

        return super.onPrepareOptionsMenu(menu);
    }

    private void movePrevious() {
        saveNote();
        notePosition--;
        note = DataManager.getInstance().getNotes().get(notePosition);
        saveOriginalNoteValues();
        displayNote(spinnerCourses, textNoteTitle, textNoteText);
        invalidateOptionsMenu();
    }

    private void moveNext() {
        saveNote();
        notePosition++;
        note = DataManager.getInstance().getNotes().get(notePosition);
        saveOriginalNoteValues();
        displayNote(spinnerCourses, textNoteTitle, textNoteText);
        invalidateOptionsMenu();
    }

    private void sendEmail() {
        CourseInfo course = (CourseInfo) spinnerCourses.getSelectedItem();
        String subject = textNoteTitle.getText().toString();
        String text = "Checkout what I learned in the Pluralsight course \"" +
                course.getTitle() + "\"\n" + textNoteText.getText().toString();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }
}
