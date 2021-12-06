package com.aspegren.vinka;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.aspegren.vinka.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_VIDEO_REQUEST = 1;
    private static final String TAG = "UPLOAD_VIDEO";
    private static final int ACTION_TAKE_VIDEO = 2;
    private static final int OTHER = 3;
    private static final String FAMILY = "Arja Nilsson";

    private Button selectButton;
    private Button uploadButton;
    private Button recordButton;

    private ProgressBar progressBarCircle;
    private EditText videoName;
    private Spinner spinnerCategory;
    private Spinner spinnerFamily;

    private TextView tvIntro; // "Spela in en hälsning ..."
    private VideoView videoView;
    private Uri videoUri;
    private MediaController mediacontroller;

    // Use firebase storage to store the video clips
    private StorageReference fbStorageReference;
    // Use FireBase database to keep track of the videos
    private DatabaseReference fbVideoDatabaseReference;
    // Use FireBase database to handle family details
    private DatabaseReference fbFamDatabaseReference;
    ArrayList<Family> families = new ArrayList<>();

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        // initialise views
        selectButton = findViewById(R.id.btnSelect);
        recordButton = findViewById(R.id.buttonRecord);
        uploadButton = findViewById(R.id.btnUpload);

        uploadButton.setEnabled(false);
        videoView = findViewById(R.id.videoView);
        progressBarCircle = findViewById(R.id.progressBarCircle);

        tvIntro = findViewById(R.id.txtIntro);
        tvIntro.setGravity(Gravity.CENTER);

        // video clips are stored in firebase storage, and links in realtime database
        fbVideoDatabaseReference = FirebaseDatabase.getInstance().getReference("videos");
        fbStorageReference = FirebaseStorage.getInstance().getReference("videos/Johan");
        // video clips are stored in firebase storage, and links in realtime database
        fbFamDatabaseReference = FirebaseDatabase.getInstance().getReference("families");
        readData(fbFamDatabaseReference);

//        Family family = createFamily();

        // start up the mediacontroller to handle videos
        mediacontroller = new MediaController(this);
        videoView.setMediaController(mediacontroller);
        mediacontroller.setAnchorView(videoView);
        videoView.start();

        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "videoView click3ed");
                mediacontroller.setVisibility(View.VISIBLE);
            }
        });

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "record click3ed");
                recordVideo();
            }
        });

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "select click3ed");
                selectVideo();
            }
        });
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "upload click3ed");
                uploadVideo();
            }
        });
    }

    private void readData(DatabaseReference ref) {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Iterable<DataSnapshot> stuff = dataSnapshot.getChildren();
                for(DataSnapshot s : stuff){
                    Family f = s.getValue(Family.class);
                    Log.d(TAG, "Family is: " + f.toString());
                    families.add(f);
                }
                if(families.isEmpty()){
                    Log.d(TAG, "No families available: " );
                    Toast.makeText(getApplicationContext(), "No families available", Toast.LENGTH_SHORT).show();
                }else{
                    Log.d(TAG, "Families available: " );
                    setupSpinners(families.get(0));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }


    private void setupSpinners(Family family) {
        // spinner for category
        ArrayList<String> categories = family.getCategories();
        ArrayList<String> authors = family.getAuthors();

        spinnerCategory = (Spinner)findViewById(R.id.spinCategory);
        spinnerFamily = (Spinner)findViewById(R.id.spinFamilies);

        ArrayAdapter<String>adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_item, categories);
        ArrayAdapter<String>adapterFamily = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_item, authors);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterFamily.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, (String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerFamily.setAdapter(adapterFamily);
        spinnerFamily.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, (String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    private Family createFamily() {
        ArrayList<String> categories = new ArrayList<>();
        categories.add("Hälsning");
        categories.add("Påminnelse");

        ArrayList<String> authors = new ArrayList<>();
        authors.add("Johan");
        authors.add("Aziza");
        authors.add("Jossan");
        authors.add("Maria");

        Family f = new Family("Arja Nilsson", categories, authors);
        //fbFamiliesDatabaseReference.child("families").child(f.getName()).setValue(f);
        return f;
    }

    private void recordVideo() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
    }

    private void selectVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
        Log.d(TAG, "clicked select");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onactivityResult");
        uploadButton.setEnabled(false);

        if(requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK
                && data != null){
            tvIntro.setVisibility(View.GONE);
            if(data.getData() != null){
                videoUri = data.getData();
                mediacontroller.setVisibility(View.GONE);
                videoView.setVideoURI(videoUri);
                int startPosition = 1;
                videoView.seekTo(startPosition);
                videoView.requestFocus();
                videoView.start();
                Log.d(TAG, "onactivityResult PICK_VIDEO " + videoUri.toString());
                // enable upload
                uploadButton.setEnabled(true);
            }
      }
        if(requestCode == ACTION_TAKE_VIDEO && resultCode == RESULT_OK
                && data != null){
            if(data.getData() != null){
                videoUri = data.getData();
                mediacontroller.setVisibility(View.GONE);
                videoView.setVideoURI(videoUri);
                int startPosition = 1;
                videoView.seekTo(startPosition);
                videoView.requestFocus();
                //videoView.start();
                    Log.d(TAG, "onactivityResult TAKE_VIDEO " + videoUri.toString());
                // enable upload
                uploadButton.setEnabled(true);
            }
        }
    }

    private String getFileExt(Uri videoUri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(videoUri));

    }

    private void uploadVideo() {
        progressBarCircle.setVisibility(View.VISIBLE);
        if(videoUri != null){
            Toast.makeText(getApplicationContext(), "Laddar upp ...", Toast.LENGTH_SHORT).show();

            String strStorageReference = System.currentTimeMillis() + "." + getFileExt(videoUri);
            StorageReference reference = fbStorageReference.child(strStorageReference);
            reference.putFile(videoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    progressBarCircle.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(), "Upload successful",    Toast.LENGTH_SHORT).show();

                    // make a note in realtime database
                    //int videoCategory = spinner.getSelectedItem().toString().trim();
                    String videoClipName = "New video";
                    int videoClipCategory = getCategory();
                    String videoclipUri = taskSnapshot.getUploadSessionUri().toString();
                    //String author = "Johan";
                    String author = spinnerFamily.getSelectedItem().toString().trim();

                    long timeStamp = System.currentTimeMillis();

                    VideoClip videoClip = new VideoClip(
                            videoClipName,
                            videoclipUri,
                            author,
                            videoClipCategory,
                            timeStamp,
                            strStorageReference);
                    Log.d(TAG, "uploaded " + videoClip.toString());
                    String upload = fbVideoDatabaseReference.push().getKey();
                    fbVideoDatabaseReference.child(upload).setValue(videoClip);
                    uploadButton.setEnabled(false);
                }
            })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBarCircle.setVisibility(View.INVISIBLE);
                    }
                });
        } else {
            Toast.makeText(getApplicationContext(), "No file selected", Toast.LENGTH_SHORT).show();
            progressBarCircle.setVisibility(View.INVISIBLE);
        }
    }

    private int getCategory() {
        String author = spinnerFamily.getSelectedItem().toString().trim();
        String strcat = spinnerCategory.getSelectedItem().toString();
        if (strcat.equals("Påminnelse")){ return VideoClip.REMINDER;}
        if (strcat.equals("Tröst")){ return VideoClip.COMFORT;}
        return VideoClip.GREETING;
    }
}