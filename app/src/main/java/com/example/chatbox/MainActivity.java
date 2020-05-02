package com.example.chatbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ViewPager myViewPager;
    private TabLayout myTabLayout;
    private TabsAccessorAdaptor myTabsAccessorAdaptor;

    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance(); // TO initialize mAuth
        currentUser = mAuth.getCurrentUser(); // To Get Current User
        RootRef = FirebaseDatabase.getInstance().getReference(); // To Initialize Root Reference

        mToolbar = (Toolbar)findViewById(R.id.main_page_toolbar);  //to set the toolbar title
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("ChatBox");

        myViewPager = (ViewPager) findViewById(R.id.main_tabs_pager);
        myTabsAccessorAdaptor = new TabsAccessorAdaptor(getSupportFragmentManager());
        myViewPager.setAdapter(myTabsAccessorAdaptor);   // viewpager assigns the views and titles with the help of tabsaccessoradaptor

        myTabLayout = (TabLayout) findViewById(R.id.main_tabs);
        myTabLayout.setupWithViewPager(myViewPager);             //setting table layout with view pager

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser == null)      // if user is not present then on start screen will move to loginactivity
        {
            SendUserToLoginActivity();
        }
        else
            {
                VerifyUserExistence();
            }
    }

// Function to check the existence of user using uid
    private void VerifyUserExistence()
    {
        String currentUserID = mAuth.getCurrentUser().getUid();
        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if((dataSnapshot.child("name").exists()))   // If name is already present for Current UID
                {
                    Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT).show();
                }
                else                                // Else screen will move to Settings Activity
                {
                    SendUserToSettingsActivity();
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    // The following function creates the options menu on TOP right corner of app shown by 3 dots
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu,menu);
        return true;
    }


    // The following function is used to select a particular item from Options Menu and Perform some action accordingly
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
         super.onOptionsItemSelected(item);

         if(item.getItemId() == R.id.main_logout_option)   // listener for logout option
         {
            mAuth.signOut();
            SendUserToLoginActivity();
         }

        if(item.getItemId() == R.id.main_settings_option)  // listener for settings option
        {
            SendUserToSettingsActivity();
        }

        if(item.getItemId() == R.id.main_create_group_option) // listener for create group option
        {
            RequestNewGroup();
        }

        if(item.getItemId() == R.id.main_find_friends_option)
        {

        }
            return true;
    }

    // Method for New Group Request and Creation
    private void RequestNewGroup()
    {
        // TO CREATE ALERT DIALOGUE MANUALLY
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,R.style.AlertDialog);
        builder.setTitle("Enter Group Name :");

        final EditText groupNameField = new EditText(MainActivity.this);  //field to enter group name
        groupNameField.setHint("e.g School Friends");
        builder.setView(groupNameField);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String groupName = groupNameField.getText().toString();
                
                if(TextUtils.isEmpty(groupName))
                {
                    Toast.makeText(MainActivity.this, "Please write Group Name", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    // Function to create group in database
                    CreateNewGroup(groupName);
                }

            }
        });

        // If the user cancels the dialog
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        builder.show();  //to display dialog on screen
    }

    private void CreateNewGroup(final String groupName)
    {
        RootRef.child("Groups").child(groupName).setValue("")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if(task.isSuccessful())
                {
                    Toast.makeText(MainActivity.this, groupName + "group is Created Successfully", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void SendUserToLoginActivity()
    {
        Intent LoginIntent = new Intent(MainActivity.this , LoginActivity.class);
        LoginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  //prevents user going to previous activity on back press
        startActivity(LoginIntent);
        finish();
    }

    private void SendUserToSettingsActivity()
    {
        Intent settingsIntent = new Intent(MainActivity.this , SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  //prevents user going to previous activity on back press
        startActivity(settingsIntent);
        finish();
    }
}
