package com.example.chatbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button UpdateAccountSettings;
    private EditText userName, userStatus;
    private CircleImageView userProfileImage;

    private FirebaseAuth mAuth;
    private String currentUserID;
    private DatabaseReference RootRef;

    private static final int GalleryPick = 1;
    private StorageReference UserProfileImageRef;  //For image ref
    private ProgressDialog loadingBar;
    private Uri ImageUri;
    public Uri resultUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");  //Initialization

        InitializeFields();

        UpdateAccountSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                UpdateSettings();
            }
        });

        RetrieveUserInfo();  // Function to retrieve user info from database

        // Setting up listener for userProfileImage to open gallery
        userProfileImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GalleryPick);

            }
        });
    }

    private void InitializeFields()
    {
        UpdateAccountSettings = (Button) findViewById(R.id.update_settings_button);
        userName = (EditText) findViewById(R.id.set_user_name);
        userStatus = (EditText) findViewById(R.id.set_profile_status);
        userProfileImage = (CircleImageView) findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);
    }

    // Function to  select and store Profile image in storage
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==GalleryPick && resultCode==RESULT_OK && data!=null && data.getData()!=null)
        {
             ImageUri = data.getData();

            CropImage.activity().
                    setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1).start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK)
            {
                loadingBar.setTitle("Setting Profile Image");
                loadingBar.setMessage("Please Wait, your profile image is updating...");
                loadingBar.setCanceledOnTouchOutside(true);
                loadingBar.show();

                resultUri = result.getUri();
          //      Picasso.get().load(resultUri).into(userProfileImage);

                // Storing the resulted cropped image in the firebase storage
                final StorageReference filePath = UserProfileImageRef.child(currentUserID + ".jpg");
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task)
                    {
                        if (task.isSuccessful())
                        {
                           Toast.makeText(SettingsActivity.this, "Image Uploaded to Firebase Storage Successfully..", Toast.LENGTH_SHORT).show();

                // Retrieving the stored image From Firebase Storage and displaying on circular image view
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {

                                    String downloadedUrl =  String.valueOf(uri);
                                    Picasso.get().load(uri).into(userProfileImage);
                                    Toast.makeText(SettingsActivity.this, "Profile pic updated successfully", Toast.LENGTH_SHORT).show();
                                    loadingBar.dismiss();

                                    // To store the url of image in realtime database from firebase storage
                                    RootRef.child("Users").child(currentUserID).child("image")
                                            .setValue(downloadedUrl).
                                            addOnCompleteListener(new OnCompleteListener<Void>()
                                            {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task)
                                                {
                                                    if (task.isSuccessful())
                                                    {
                                                        Toast.makeText(SettingsActivity.this, "Image saved in firebase  database", Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    }

                                                    else
                                                    {
                                                        String message = task.getException().toString();
                                                        Toast.makeText(SettingsActivity.this, "Error :" + message, Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    }

                                                }
                                            });


                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                }
                            });

                        }

                        // else of storage image upload failure
                        else
                        {
                            String message = task.getException().toString();
                            Toast.makeText(SettingsActivity.this, "Error: " +message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }

                    }
                });
            }

        }
    }


    // Function to update user info on settings activity
    private void UpdateSettings()
    {
        String setUserName = userName.getText().toString();
        String setStatus = userStatus.getText().toString();

        if(TextUtils.isEmpty(setUserName))
        {
            Toast.makeText(this, "Please write your user name first ...", Toast.LENGTH_SHORT).show();
        }

        if(TextUtils.isEmpty(setUserName))
        {
            Toast.makeText(this, "Please write your status ...", Toast.LENGTH_SHORT).show();
        }

        else
        {
            HashMap<String,String> profileMap = new HashMap<>();  // HashMap type object creation to put name,status into database
                profileMap.put("uid",currentUserID);
                profileMap.put("name",setUserName);
                profileMap.put("status",setStatus);

            RootRef.child("Users").child(currentUserID).setValue(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if(task.isSuccessful())
                            {
                                Toast.makeText(SettingsActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                                SendUserToMainActivity();
                            }

                            else
                            {
                                String message = task.getException().toString();
                                Toast.makeText(SettingsActivity.this, "Error :" + message, Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
        }
    }

    // To retrieve Username and Status from database and displaying these on settings activity
    private void RetrieveUserInfo()
    {
        RootRef.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name")) && dataSnapshot.hasChild("image"))
                {
                    String retrieveUserName = dataSnapshot.child("name").getValue().toString();
                    String retrieveStatus = dataSnapshot.child("status").getValue().toString();
                    String retrieveProfileImage = dataSnapshot.child("image").getValue().toString();
                    Log.i("INFO", retrieveProfileImage);
                    userName.setText(retrieveUserName);
                    userStatus.setText(retrieveStatus);
                    Picasso.get().load(retrieveProfileImage).into(userProfileImage);
                }

                else if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name")))
                {
                    String retrieveUserName = dataSnapshot.child("name").getValue().toString();
                    String retrieveStatus = dataSnapshot.child("status").getValue().toString();

                    userName.setText(retrieveUserName);
                    userStatus.setText(retrieveStatus);
                }

                else
                {
                    Toast.makeText(SettingsActivity.this, "Please Update Your Profile Information", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendUserToMainActivity()
    {
        Intent mainIntent = new Intent(SettingsActivity.this , MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  //prevents user going to previous activity on back press
        startActivity(mainIntent);
        finish();
    }

}
