package com.example.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.happyplaces.R
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
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

    ///// Image path for easy reuse
    private var saveImageToInternalStorage: Uri? = null

    ///// A variable which will hold the latitude value
    private var mLatitude: Double = 0.0

    ///// A variable which will hold the longitude value
    private var mLongitude: Double = 0.0

    ///// For storing new entries changing due to RV swipe to edit feature
    private var mHappyPlaceDetails: HappyPlaceModel? = null

    ///// A fused location client variable which is further user to get the user's current location
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        //region ActionBar

        setSupportActionBar(binding?.toolbarAddPlace)

        ///// Display back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.setTitle(R.string.add_happy_place)

        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }
        //endregion

        //region Swipe to edit Intent (Receiving the info)

        ///// When model info passed throw intent store them in mHappyPlaceDetails
        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mHappyPlaceDetails = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS)
        }

        ///// When model info passed throw intent populate the views automatically
        if (mHappyPlaceDetails != null) {

            supportActionBar?.setTitle(R.string.edit_happy_place)

            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)

            binding?.ivPlaceImage?.setImageURI(saveImageToInternalStorage)

            binding?.btnSave?.setText(R.string.update)
        }
        //endregion

        //region DatePickerDialog

        ///// Listener to pick the user's choice
        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->

            ///// Set the Calender year to the user's choice
            cal.set(Calendar.YEAR, year)

            ///// Set the Calender month to the user's choice
            cal.set(Calendar.MONTH, monthOfYear)

            ///// Set the Calender day to the user's choice
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            ///// Update EditText with selected date function
            updateDateInView()
        }
        //endregion

        ///// Automatic set current date onCreate
        updateDateInView()

        ///// When model info passed throw intent to populate the views automatically
        if (mHappyPlaceDetails != null) {

            supportActionBar?.setTitle(R.string.edit_happy_place)

            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)

            binding?.ivPlaceImage?.setImageURI(saveImageToInternalStorage)

            binding?.btnSave?.setText(R.string.update)
        }

        //region setOnClickListener

        ///// this point to AddHappyPlaceActivity as it implement OnClickListener functionality
        binding?.etDate?.setOnClickListener(this)

        binding?.tvAddImage?.setOnClickListener(this)

        binding?.btnSave?.setOnClickListener(this)

        binding?.etLocation?.setOnClickListener(this)

        binding?.tvSelectCurrentLocation?.setOnClickListener(this)

        binding?.ivPlaceImage?.setOnClickListener(this)
        //endregion

        ///// Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ///// Initialize the places sdk if it is not initialized earlier using the api key.
        if (!Places.isInitialized()) {
            Places.initialize(
                this@AddHappyPlaceActivity, resources.getString(R.string.google_maps_api_key)
            )
        }

    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///// For constants as static for all classes
    companion object {
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"

        ///// A constant variable for place picker
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }

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

            R.id.iv_place_image -> {

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

            R.id.btn_save -> {

                ///// Check first if the fields is not empty
                when {

                    binding?.etTitle?.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }

                    binding?.etDescription?.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
                    }

                    binding?.etLocation?.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT).show()
                    }

                    saveImageToInternalStorage == null -> {
                        Toast.makeText(this, "Please add image", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        ///// Execute save to storage code
                        ///// HappyPlaceModel object with it's properties
                        //region Set the mLatitude & mLatitude to the manual values
                        if (binding?.etLatitude?.text?.isNotEmpty() == true){
                            if (mLatitude == 0.0 && mLongitude == 0.0){
                                mLatitude = binding?.etLatitude?.text.toString().toDouble()
                                mLongitude = binding?.etLongitude?.text.toString().toDouble()
                            }
                        }
                        //endregion
                        val happyPlaceModel = HappyPlaceModel(
                            if (mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                            binding?.etTitle?.text.toString(),
                            saveImageToInternalStorage.toString(),
                            binding?.etDescription?.text.toString(),
                            binding?.etDate?.text.toString(),
                            binding?.etLocation?.text.toString(),
                            mLatitude,
                            mLongitude
                        )
                        ///// DatabaseHandler object for storing data
                        val dbHandler = DatabaseHandler(this)

                        ///// Check to add or update the entry
                        if (mHappyPlaceDetails == null) {

                            //Add
                            ///// Result
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)

                            ///// If there's no errors (as addHappyPlace fun returns Long value)
                            if (addHappyPlace > 0) {

                                setResult(Activity.RESULT_OK)
                                /*
                                Toast.makeText(
                                    this,
                                    "The happy place details are inserted successfully", Toast.LENGTH_SHORT
                                ).show()
                                */
                                ///// Finish the addHappyPlaceActivity and return to the main activity
                                finish()
                            }

                        } else {

                            //Update
                            ///// Result
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)

                            ///// If there's no errors (as updateHappyPlace fun returns Int value)
                            if (updateHappyPlace > 0) {

                                setResult(Activity.RESULT_OK)
                                /*
                                Toast.makeText(
                                    this,
                                    "The happy place details are inserted successfully", Toast.LENGTH_SHORT
                                ).show()
                                */
                                ///// Finish the addHappyPlaceActivity and return to the main activity
                                finish()
                            }
                        }

                    }
                }
            }

            R.id.et_location -> {
                try {
                    ///// These are the list of fields which we required is passed
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    ///// Start the autocomplete intent with a unique request code.
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                        .build(this@AddHappyPlaceActivity)

                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            R.id.tv_select_current_location -> {
                if (!isLocationEnabled()) {
                    Toast.makeText(
                        this, "Your location provider is turned off. Please turn it on.",
                        Toast.LENGTH_SHORT
                    ).show()

                    ///// This will redirect you to settings of location provider
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else {

                    ///// Start Dexter for getting permissions
                    Dexter.withActivity(this).withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                        ///// Listener object for permitting & denial
                        .withListener(object : MultiplePermissionsListener {

                            ///// On permission granted
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                if (report!!.areAllPermissionsGranted()) {

                                    ///// Call request location fun after permission granted
                                    requestNewLocationData()
                                    /*
                                    Toast.makeText(
                                        this@AddHappyPlaceActivity,
                                        "Location permission granted",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                     */
                                }
                            }

                            ///// On permission denial
                            override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                            ) {
                                ///// Show the dialog created before
                                showRationalDialogForPermissions()
                            }
                        }).onSameThread()
                        .check()
                }
            }

        }
    }

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
                        saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("saved image: ", "path :: $saveImageToInternalStorage")

                        /////// Set the selected image from GALLERY to the imageView
                        binding?.ivPlaceImage!!.setImageBitmap(selectedImageBitmap)

                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this@AddHappyPlaceActivity, "Failed!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else if (requestCode == CAMERA) {

                ///// Date we got ( incase from the camera ) and cast or convert it to Bitmap as it of type Intent
                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap

                ///// Call saving image to the storage fun & passing the image of type Bitmap
                saveImageToInternalStorage = saveImageToInternalStorage(thumbnail)
                Log.e("saved image: ", "path :: $saveImageToInternalStorage")

                /////// Set the selected image from CAMERA to the imageView
                binding?.ivPlaceImage!!.setImageBitmap(thumbnail)
            } else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {

                ///// Passing the data we get with the intent
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)

                ///// Display in the  location editText the current address
                binding?.etLocation?.setText(place.address)

                ///// Set mLatitude, mLongitude variables to the current
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }
        }
    }

    ///// Request the location
    @SuppressLint("MissingPermission") // We asked for permission manually before calling the function
    private fun requestNewLocationData() {

        ///// LocationRequest object
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        ///// Properties
        // How many every ms I want to run this
        mLocationRequest.interval = 0
        // update once
        mLocationRequest.numUpdates = 1

        ///// Assign settings to the Fused Location Client
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    ///// CallBack variable for requestLocationUpdates
    private val mLocationCallback = object : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {

            val mLastLocation: Location = locationResult.lastLocation

            ///// Assigning the  Latitude & Longitude
            mLatitude = mLastLocation.latitude
            Log.e("Current Latitude", "$mLatitude")


            mLongitude = mLastLocation.longitude
            Log.e("Current Longitude", "$mLongitude")

            binding?.etLongitude?.setText("$mLongitude")
            binding?.etLatitude?.setText("$mLatitude")

            val latitudeAndLongitudeForEtLocation = "$mLatitude, $mLongitude"
            binding?.etLocation?.setText(latitudeAndLongitudeForEtLocation)

        }
    }

    ///// Check if we have the permissions to get the location
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    ///// To put the selected date in the EditText function
    private fun updateDateInView() {

        ///// Format the date
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())

        ///// Set the selected date in the EditText
        binding?.etDate?.setText(sdf.format(cal.time).toString())
    }

    ///// onResult for Intent opening depending on the code
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
        } catch (e: IOException) {
            e.printStackTrace()
        }

        ///// Return the saved image in the Uri format
        return Uri.parse(file.absolutePath)
    }

}