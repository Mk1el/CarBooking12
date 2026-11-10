package com.example.carbooking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.carbooking.R
import com.example.carbooking.models.Car
import com.google.android.material.button.MaterialButton

class CarAdapter(
    private var cars: List<Car>,
    private val onBookClick: (Car) -> Unit,
    private val onRateClick: (Car) -> Unit
) : RecyclerView.Adapter<CarAdapter.CarHolder>() {

    class CarHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivThumb: ImageView = v.findViewById(R.id.ivCarThumbnail)
        val tvModel: TextView = v.findViewById(R.id.tvCarModelTitle)
        val tvSpecs: TextView = v.findViewById(R.id.tvCarSpecs)
        val tvDaily: TextView = v.findViewById(R.id.tvPriceDaily)
        val tvHourly: TextView = v.findViewById(R.id.tvPriceHourly)
        val tvRating: TextView = v.findViewById(R.id.tvCarRatingBadge)
        val btnBook: MaterialButton = v.findViewById(R.id.btnBookCar)
        val btnRate: MaterialButton = v.findViewById(R.id.btnRateCar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_car_card, parent, false)
        return CarHolder(view)
    }

    override fun onBindViewHolder(holder: CarHolder, position: Int) {
        val car = cars[position]

        holder.tvModel.text = car.model
        holder.tvSpecs.text = "Category: ${car.category} • 📍 ${car.location}"
        holder.tvDaily.text = "KES ${car.pricePerDay.toInt()} / day"
        holder.tvHourly.text = "KES ${car.pricePerHour.toInt()} / hr"
        holder.tvRating.text = "★ ${car.rating}"

        if (car.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(car.imageUrl).centerCrop().into(holder.ivThumb)
        }

        // Tapping the card opens the booking page
        holder.itemView.setOnClickListener { onBookClick(car) }

        // Tapping "Reserve Now" opens the booking page
        holder.btnBook.setOnClickListener { onBookClick(car) }

        // Tapping "Rate" opens the rating dialog
        holder.btnRate.setOnClickListener { onRateClick(car) }
    }

    override fun getItemCount(): Int = cars.size

    fun updateList(newList: List<Car>) {
        cars = newList
        notifyDataSetChanged()
    }
}