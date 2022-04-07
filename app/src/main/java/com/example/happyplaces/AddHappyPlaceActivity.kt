package com.example.happyplaces

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

///// Extend View.OnClickListener to implement it's member onClick functionality
class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private var binding: ActivityAddHappyPlaceBinding? = null

    //region Variables

    ///// Calendar instance
    private var cal = Calendar.getInstance()

    ///// On selecting date listener
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        //region ActionBar

        setSupportActionBar(binding?.toolbarAddPlace)

        ///// Display back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }
        //endregion

        //region DatePickerDialog

        ///// Listener to pick the user's choice
        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->

            ///// Set the Calender year to the user's choice
            cal.set(Calendar.YEAR, year)

            ///// Set the Calender month to the user's choice
            cal.set(Calendar.MONTH, month)

            ///// Set the Calender day to the user's choice
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            ///// Update EditText with selected date function
            updateDateInView()
        }
        //endregion

        ///// this point to AddHappyPlaceActivity as it implement OnClickListener functionality
        binding?.etDate?.setOnClickListener(this)

        ///// this point to AddHappyPlaceActivity as it implement OnClickListener functionality
        binding?.tvAddImage?.setOnClickListener(this)
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    override fun onClick(v: View) {

        when (v.id) {

            R.id.et_date -> {

                ///// Show DatePickerDialog
                DatePickerDialog(
                    this@AddHappyPlaceActivity, dateSetListener,
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            R.id.tv_add_image -> {

                //region Dialog preparation

                ///// Dialog for multiple options
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")

                ///// Dialog options as an array of Strings
                val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")

                pictureDialog.setItems(pictureDialogItems) { _, which ->

                    when (which) {

                        ///// Option #1
                        0 -> choosePhotoFromGallery()

                        ///// Option #2
                        1 -> takePhotoFromCamera()

                    }
                }
                //endregion

                ///// Show Dialog onClick
                pictureDialog.show()
            }

        }
    }

    ///// To put the selected date in the EditText function
    private fun updateDateInView() {

        ///// Format the date
        val myFormat = "dd.mm.yyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())

        ///// Set the selected date in the EditText
        binding?.etDate?.setText(sdf.format(cal.time).toString())
    }

    ///// onResult for Intent opening ( the gallery or the camera )
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                if (data != null) {

                    ///// Date we got
                    val contentURI = data.data

                    try {
                        ///// Date we got ( incase from the gallery ) from the MediaStore based on the URI
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)

                        ///// Call saving image to the storage fun & passing the image of type Bitmap
                        val saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("saved image: ", "path :: $saveImageToInternalStorage")

                        /////// Set the selected image from GALLERY to the imageView
                        binding?.ivPlaceImage!!.setImageBitmap(selectedImageBitmap)

                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this@AddHappyPlaceActivity, "Failed!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }else if (requestCode == CAMERA) {

                ///// Date we got ( incase from the camera ) and cast or convert it to Bitmap as it of type Intent
                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap

                ///// Call saving image to the storage fun & passing the image of type Bitmap
                val saveImageToInternalStorage = saveImageToInternalStorage(thumbnail)
                Log.e("saved image: ", "path :: $saveImageToInternalStorage")

                /////// Set the selected image from CAMERA to the imageView
                binding?.ivPlaceImage!!.setImageBitmap(thumbnail)
            }
        }
    }

    private fun takePhotoFromCamera() {
        ///// Ask for the permissions
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        )
            .withListener(object : MultiplePermissionsListener {

                ///// On permissions granted
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {

                    ///// When permission is granted execute opening the gallery code
                    if (report.areAllPermissionsGranted()) {

                        ///// Intent for opening the camera
                        val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        ///// Start the intent
                        startActivityForResult(galleryIntent, CAMERA)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>, token: PermissionToken
                ) {

                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
    }

    ///// Ask for permissions for choosing photos form gallery
    private fun choosePhotoFromGallery() {

        ///// Ask for the permissions
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
            .withListener(object : MultiplePermissionsListener {

                ///// On permissions granted
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {

                ///// When permission is granted execute opening the gallery code
                if (report.areAllPermissionsGranted()) {
                    /*
                    Toast.makeText(
                        this@AddHappyPlaceActivity,
                        "Storage Read/Write permissions are granted. Now you can select image form the gallery",
                        Toast.LENGTH_SHORT
                    ).show()
                    */
                    ///// Intent for opening the gallery
                    val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    ///// Start the intent
                    startActivityForResult(galleryIntent, GALLERY)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>, token: PermissionToken
            ) {

                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    ///// Show dialog when permissions are not granted function
    private fun showRationalDialogForPermissions() {

        ///// Dialog
        AlertDialog.Builder(this).setMessage("You turned off permission for this feature")

                ///// Option #1
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {

                    //region Go to the settings Intent

                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)

                    intent.data = uri
                    startActivity(intent)
                    //endregion

                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }

            ///// Option #2
            .setNegativeButton("Cancel") { dialog, _ ->

                ///// Dismiss the dialog when cancel is clicked
                dialog.dismiss()
            }

            ///// Show the Dialog fun
            .show()
    }

    ///// It returns a Uri which is a location of the image we are storing
    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {

        ///// Get the context wrapper instance
        val wrapper = ContextWrapper(applicationContext)                       ///// Context

        ///// Initializing a new file directory in internal storage
        ///// MODE_PRIVATE means the file is accessible only by the app
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)      ///// Directory

        // Create a file to save the image
        file = File(file, "${UUID.randomUUID()}.jpg")                    ///// File name

        try {
            // Get the file output stream
            val stream: OutputStream = FileOutputStream(file)

            // Compress bitmap
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

            // Flush the stream
            stream.flush()

            // Close stream
            stream.close()
        }
        catch (e: IOException) {
            e.printStackTrace()
        }

        ///// Return the saved image in the Uri format
        return Uri.parse(file.absolutePath)
    }

    ///// For constants as static for all classes
    companion object{
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
    }
}