package com.example.happyplaces.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplaces.R
import com.example.happyplaces.databinding.ActivityMapBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

///// Extend OnMapReadyCallback interface for getMapAsync callBack
class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private var binding: ActivityMapBinding? = null

    ///// Variable to hold the place details that passed throw an intent
    private var mHappyPlaceDetails: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mHappyPlaceDetails = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel?
        }

        if (mHappyPlaceDetails != null) {

            setSupportActionBar(binding?.toolbarMap)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = mHappyPlaceDetails!!.title

            binding?.toolbarMap?.setNavigationOnClickListener {
                onBackPressed()
            }

            ///// SupportMapFragment object
            val supportMapFragment: SupportMapFragment =
                supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            supportMapFragment.getMapAsync(this)
        }

    }

    ///// Define what to happen once the map is ready
    override fun onMapReady(googleMap: GoogleMap) {

        ///// Add a marker on the location using the latitude and longitude and move the camera to it.
        ///// LatLng object (the position)
        val position = LatLng(mHappyPlaceDetails!!.latitude, mHappyPlaceDetails!!.longitude)


        ///// Put the marker
        googleMap.addMarker(MarkerOptions().position(position).title(mHappyPlaceDetails!!.location))

        ///// Zoom the map to the location
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 15f)
        googleMap.animateCamera(newLatLngZoom)
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}