package com.example.happyplaces.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplaces.databinding.ActivityHappyPlaceDetailBinding
import com.example.happyplaces.models.HappyPlaceModel

class HappyPlaceDetailActivity : AppCompatActivity() {
    private var binding: ActivityHappyPlaceDetailBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        ///// Create a HappyPlaceModel object
        var happyPlaceDetailModel: HappyPlaceModel? = null

        ///// If the Intent has shared information
        ///// MainActivity.EXTRA_PLACE_DETAILS is the way to call the companion object (of String type)
        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {

            ///// getSerializableExtra instead of getExtra as the information is a serializable object
            ///// Casting as HappyPlaceModel cause it won't be in the HappyPlaceModel format
                ///// Then we used getParcelableExtra after converting serializable to Parcelable
            happyPlaceDetailModel = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel?
        }

        if (happyPlaceDetailModel != null) {

            //region ActionBar

            ///// Bind with the xml
            setSupportActionBar(binding?.toolbarHappyPlaceDetail)

            ///// Show the backButton
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)

            ///// Set the title of the ActionBar with the selected happyPlaceDetailModel object
            supportActionBar!!.title = happyPlaceDetailModel.title

            ///// Back button listener
            binding?.toolbarHappyPlaceDetail?.setNavigationOnClickListener { onBackPressed() }
            //endregion

            //region Set the Views

            ///// setImageURI as we store the image in the model as a String (link to the database)
            binding?.ivPlaceImage?.setImageURI(Uri.parse(happyPlaceDetailModel.image))
            binding?.tvDescription?.text = happyPlaceDetailModel.description
            binding?.tvLocation?.text = happyPlaceDetailModel.location
            //endregion
        }

        binding?.btnViewOnMap?.setOnClickListener {

            val intent = Intent(this, MapActivity::class.java)

            intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, happyPlaceDetailModel)

            startActivity(intent)
        }

    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}