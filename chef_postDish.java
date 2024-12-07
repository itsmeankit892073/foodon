package com.ankit.foodon;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class chef_postDish extends AppCompatActivity {

    private ImageButton imageButton;
    private Button postDishButton;
    private Spinner dishSpinner;
    private TextView descriptionText, quantityText, priceText;
    private Uri imageUri;

    private FirebaseStorage storage;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    private String chefId, randomUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chef_post_dish);

        // Firebase initialization
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("FoodDetails");

        // UI components
        dishSpinner = findViewById(R.id.dishes);
        descriptionText = findViewById(R.id.description);
//        quantityText = findViewById(R.id.quantity);
        priceText = findViewById(R.id.price);
        postDishButton = findViewById(R.id.post);
        imageButton = findViewById(R.id.image_upload);

        // Image upload button click listener
        imageButton.setOnClickListener(v -> selectImage());

        // Post dish button click listener
        postDishButton.setOnClickListener(v -> postDish());
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
    }

    private void postDish() {
        String dish = dishSpinner.getSelectedItem().toString();
        String description = descriptionText.getText().toString().trim();
        String quantity = quantityText.getText().toString().trim();
        String price = priceText.getText().toString().trim();

        if (isValidInput(description, quantity, price)) {
            uploadImage(dish, description, quantity, price);
        }
    }

    private boolean isValidInput(String description, String quantity, String price) {
        if (TextUtils.isEmpty(description) || TextUtils.isEmpty(quantity) || TextUtils.isEmpty(price)) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void uploadImage(String dish, String description, String quantity, String price) {
        if (imageUri != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            randomUID = UUID.randomUUID().toString();
            StorageReference ref = storageReference.child("dishes").child(randomUID);
            chefId = auth.getCurrentUser().getUid();

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                FoodDetails foodDetails = new FoodDetails(dish, quantity, price, description, uri.toString(), randomUID, chefId);
                                databaseReference.child(chefId).child(randomUID).setValue(foodDetails)
                                        .addOnCompleteListener(task -> {
                                            progressDialog.dismiss();
                                            Toast.makeText(this, "Dish posted successfully", Toast.LENGTH_SHORT).show();
                                        });
                            }))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                imageUri = data.getData();
                imageButton.setImageURI(imageUri);  // Set the selected image directly without cropping
            }
        }
    }
}
