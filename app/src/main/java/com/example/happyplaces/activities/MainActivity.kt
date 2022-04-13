package com.example.happyplaces.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.adapters.HappyPlacesAdapter
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ActivityMainBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.happyplaces.utils.SwipeToDeleteCallback
import pl.kitek.rvswipetodelete.SwipeToEditCallback

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.fabAddHappyPlace?.setOnClickListener {
            val intent = Intent(this, AddHappyPlaceActivity::class.java)

            ///// StartActivityForResult in order to make the RV responsible to the database new entries
            startActivityForResult(intent, ADD_PLACE_ACTIVITY_REQUEST_CODE)
        }

        getHappyPlacesListFromLocalDB()
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun setupHappyPlacesRecyclerView(happyPlacesList: ArrayList<HappyPlaceModel>) {

        binding?.rvHappyPlacesList?.layoutManager = LinearLayoutManager(this)
        binding?.rvHappyPlacesList?.setHasFixedSize(true)

        val placesAdapter = HappyPlacesAdapter(this, happyPlacesList)
        binding?.rvHappyPlacesList?.adapter = placesAdapter

        ///// Call setOnClickListener fun from the adapter
        ///// Pass OnClickListener interface to it as an object because it need a parameter of type OnClickListener
        placesAdapter.setOnClickListener(object : HappyPlacesAdapter.OnClickListener {

            ///// In the interface OnClickListener we can override the onClick fun
            override fun onClick(position: Int, model: HappyPlaceModel) {

                ///// Code to execute on click event
                ///// Go to HappyPlaceDetailActivity
                val intent = Intent(this@MainActivity, HappyPlaceDetailActivity::class.java)

                ///// Pass information the model info to HappyPlaceDetailActivity to populate the views for editing
                intent.putExtra(EXTRA_PLACE_DETAILS, model)
                startActivity(intent)
            }
        })

        //region Swipe To Edit

        ///// Object of SwipeToEditCallback
        val editSwipeHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvHappyPlacesList?.adapter as HappyPlacesAdapter

                ///// Notify the adapter which calls an Intent
                adapter.notifyEditItem(this@MainActivity, viewHolder.adapterPosition, ADD_PLACE_ACTIVITY_REQUEST_CODE)
            }
        }

        ///// Assign editSwipeHandler via ItemTouchHelper object
        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)

        ///// Attach ItemTouchHelper to the RV
        editItemTouchHelper.attachToRecyclerView(binding?.rvHappyPlacesList)
        //endregion
        //region Swipe To Delete

        ///// Object of SwipeToEditCallback
        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvHappyPlacesList?.adapter as HappyPlacesAdapter

                ///// Notify the adapter which calls an Intent
                adapter.removeAt(viewHolder.adapterPosition)

                ///// Reload the entries from the database again after deleting
                getHappyPlacesListFromLocalDB()
            }
        }

        ///// Assign editSwipeHandler via ItemTouchHelper object
        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)

        ///// Attach ItemTouchHelper to the RV
        deleteItemTouchHelper.attachToRecyclerView(binding?.rvHappyPlacesList)
        //endregion

    }

    ///// Load the entries from the database & prepare the Views
    private fun getHappyPlacesListFromLocalDB() {

        val dbHandler = DatabaseHandler(this)

        val getHappyPlacesList: ArrayList<HappyPlaceModel> = dbHandler.getHappyPlacesList()

        if (getHappyPlacesList.size > 0) {

            ///// Prepare the Views
            binding?.rvHappyPlacesList?.visibility = View.VISIBLE
            binding?.tvNoRecordsAvailable?.visibility = View.GONE

            ///// Call the recyclerView fun
            setupHappyPlacesRecyclerView(getHappyPlacesList)


        } else {
            ///// Hide the Views
            binding?.rvHappyPlacesList?.visibility = View.GONE
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE

        }
    }

    // Call Back method  to get the Message form other Activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // check if the request code is same as what is passed  here it is 'ADD_PLACE_ACTIVITY_REQUEST_CODE'
        if (requestCode == ADD_PLACE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {

                ///// Refresh the RV here
                getHappyPlacesListFromLocalDB()
            } else {
                Log.e("Activity", "Cancelled or Back Pressed")
            }
        }
    }

    companion object {
        private const val ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        internal const val EXTRA_PLACE_DETAILS = "extra_place_details"
    }
}