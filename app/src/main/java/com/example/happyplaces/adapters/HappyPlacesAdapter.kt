package com.example.happyplaces.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.R
import com.example.happyplaces.activities.AddHappyPlaceActivity
import com.example.happyplaces.activities.MainActivity
import com.example.happyplaces.models.HappyPlaceModel
import kotlinx.android.synthetic.main.item_happy_place.view.*

class HappyPlacesAdapter(
    private val context: Context,
    private var list: ArrayList<HappyPlaceModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_happy_place, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {
            holder.itemView.iv_place_image.setImageURI(Uri.parse(model.image))
            holder.itemView.tvTitle.text = model.title
            holder.itemView.tvDescription.text = model.description

            ///// Assigning onClick method to every item in the RV
            holder.itemView.setOnClickListener {
                if (onClickListener != null){
                    onClickListener?.onClick(position, model)
                }
            }

        }
    }

    fun notifyEditItem(activity: Activity, position: Int, requestCode: Int) {

        ///// Intent to go to AddHappyPlaceActivity for editing
        val intent = Intent(context, AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, list[position])
        activity.startActivityForResult(intent, requestCode)

        ///// Notify the adapter that the item at position has changed
        notifyItemChanged(position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    ///// A function to bind the onclickListener

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }
    interface OnClickListener {
        fun onClick(position: Int, model: HappyPlaceModel)
    }
    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}