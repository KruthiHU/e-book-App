package com.example.ecloudapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.ecloudapp.databinding.ActivityPdfAddBinding;
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

import java.util.ArrayList;
import java.util.HashMap;

public class PdfAddActivity extends AppCompatActivity {

    //view binding
    private ActivityPdfAddBinding binding;

    //firebase auth
    private FirebaseAuth firebaseAuth;

    // progress dialog
    private ProgressDialog progressDialog;

    //arraylist to hold pdf categories
    private ArrayList<String> categoryTitleArrayList, categoryIdArrayList;

    //uri of picked pdf
    private Uri pdfUri=null;

    private static final int PDF_PICK_CODE = 1000;

    //Tag for debugging
    private static final String TAG = "ADD_PDF_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        loadPdfCategories();

        // setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle click, go to previous activity
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //handle click, attach pdf
        binding.attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               pdfPickIntent();
            }
        });

        //handle click, PICK category
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categoryPickDialog();
            }
        });

        //handle click, upload pdf
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //validate data
                validateData();
            }
        });
    }

    private String title = "", description = "";

    private void validateData() {
        //S1:validate data
        Log.d(TAG, "validateData: validating data...");
        //get data
        title = binding.titleEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();
       // category = binding.categoryTv.getText().toString().trim();

        //validate data
        if (TextUtils.isEmpty(title)){
            //name edit text is empty
            Toast.makeText(this, "Enter Title ...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(description)){
            //email is either not entered or invalid
            Toast.makeText( this,  "Enter Description...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(selectedCategoryTitle)){
            //password edit text is empty
            Toast.makeText(this, "Pick Category ...", Toast.LENGTH_SHORT) .show();
        }
        else if (pdfUri==null){
            //confirm password edit text is empty
            Toast.makeText( this,"Pick Pdf", Toast.LENGTH_SHORT).show();
        }
        else{
            //all data is validated
           uploadPdfToStorage();
        }
    }

    private void uploadPdfToStorage() {
        //S2: Upload Pdf to firebase storage
        Log.d(TAG, "uploadPdfToStorage: Uploading to Storage...");

        //show progress
        progressDialog.setMessage("Uploading Pdf...");
        progressDialog.show();

        // timestamp
        long timestamp = System.currentTimeMillis();

        //path of pdf in firebase storage
        String filePathAndName = "Books/" + timestamp;

        //storage
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(pdfUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "OnSuccess: PDF uploaded to storage");
                        Log.d(TAG, "OnSuccess: getting Pdf url");

                        //get pdf url
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String uploadedPdfUrl = ""+uriTask.getResult();

                        //upload to firebase db
                        uploadPdfInfoToDb(uploadedPdfUrl, timestamp);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure:Pdf upload failed due to "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Pdf upload failed due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadPdfInfoToDb(String uploadedPdfUrl, long timestamp) {
        //S3: Upload Pdf info to firebase db
        Log.d(TAG, "uploadPdfInfoToDb: Uploading Pdf info to firebase db...");

        //show progress
        progressDialog.setMessage("Uploading Pdf info...");
        progressDialog.show();

        String uid = firebaseAuth.getUid();

        //setup data to upload
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("uid", ""+uid);
        hashMap.put("id", ""+timestamp);
        hashMap.put("title", ""+title);
        hashMap.put("description", ""+description);
        hashMap.put("categoryId", ""+selectedCategoryId);
        hashMap.put("url", ""+uploadedPdfUrl);
        hashMap.put("timestamp", timestamp);
        hashMap.put("viewsCount", 0);
        hashMap.put("downloadsCount", 0);

        //db reference: DB > Books
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(""+timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // data added to db
                        progressDialog.dismiss();
                        Log.d(TAG, "OnSuccess: Successfully uploaded ");
                        Toast.makeText( PdfAddActivity.this,  "Successfully uploaded ", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure:Pdf upload to db failed due to "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Pdf upload to db failed due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void loadPdfCategories() {
        Log.d(TAG, "loadPdfCategories: Loading pdf categories....");
        categoryTitleArrayList = new ArrayList<>();
        categoryIdArrayList = new ArrayList<>();

        //db reference to load categories db > Categories
       DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "Categories");
       ref.addListenerForSingleValueEvent(new ValueEventListener() {
           @Override
           public void onDataChange(@NonNull DataSnapshot snapshot) {
               categoryTitleArrayList.clear(); // clear before adding data
               categoryIdArrayList.clear();
               for (DataSnapshot ds: snapshot.getChildren()) {
                   // get id and title of category
                   String categoryId = ""+ds.child("id").getValue();
                   String categoryTitle = ""+ds.child("category").getValue();

                   //ModelCategory model = ds.getValue(ModelCategory.class);

                   //add to  respective arraylist
                   categoryTitleArrayList.add(categoryTitle);
                   categoryIdArrayList.add(categoryId);
                  // Log.d(TAG, "onDataChange: "+model.getCategory());
                 }
               }

           @Override
           public void onCancelled(@NonNull DatabaseError error){

           }
       });
    }

    // selected category id and category title
    private String selectedCategoryId, selectedCategoryTitle;

    private void categoryPickDialog() {
        //first we need to get categories from firebase
        Log.d(TAG, "categoryPickDialog: showing category pick dialog");

        // get string array of categories from arraylist
        String[] categoriesArray = new String[categoryTitleArrayList.size()];
        for (int i=0;i<categoryTitleArrayList.size();i++) {
            categoriesArray[i] = categoryTitleArrayList.get(i);
        }

        // alert dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Pick Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // handle item click
                        // get clicked item from list
                        selectedCategoryTitle = categoryTitleArrayList.get(i);
                        selectedCategoryId = categoryIdArrayList.get(i);

                        //set to category textview
                        binding.categoryTv.setText(selectedCategoryTitle);

                        Log.d(TAG, "onClick: Selecting Category: "+selectedCategoryId+" "+selectedCategoryTitle);
                    }
                })
                .show();

    }

    private void pdfPickIntent() {
        Log.d(TAG, "pdfPickIntent: starting pdf pick intent");

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Pdf"),PDF_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            Log.d(TAG, "onActivityResult: PDF Picked");

            pdfUri = data.getData();

            Log.d(TAG, "onActivityResult: URI: "+pdfUri);

        }else{
            Log.d(TAG, "onActivityResult: Cancelled Picking Pdf");
            Toast.makeText(this, "Cancelled Picking Pdf", Toast.LENGTH_SHORT).show();
        }
    }
}