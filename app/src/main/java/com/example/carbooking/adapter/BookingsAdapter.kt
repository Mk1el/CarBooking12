package com.example.carbooking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.carbooking.R
import com.example.carbooking.models.Booking
import java.text.NumberFormat
import java.util.Locale

class BookingsAdapter(private var bookings: List<Booking>) :
    RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvModel: TextView = itemView.findViewById(R.id.tvBookingCarModel)
        val tvStatus: TextView = itemView.findViewById(R.id.tvBookingStatus)
        val tvDuration: TextView = itemView.findViewById(R.id.tvBookingDuration)
        val tvPayment: TextView = itemView.findViewById(R.id.tvBookingPaymentMethod)
        val tvPrice: TextView = itemView.findViewById(R.id.tvBookingTotalPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking_card, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val b = bookings[position]
        holder.tvModel.text = b.carModel
        holder.tvStatus.text = b.status
        val unit = if (b.rentalType == "HOURS") "Hours" else "Days"
        holder.tvDuration.text = "Duration: ${b.duration} $unit (${b.rentalType})"
        holder.tvPayment.text = "Paid via: ${b.paymentMethod}"
        val formatted = NumberFormat.getNumberInstance(Locale.US).format(b.totalPrice)
        holder.tvPrice.text = "KES $formatted"
    }

    override fun getItemCount(): Int = bookings.size

    fun updateList(newList: List<Booking>) {
        bookings = newList
        notifyDataSetChanged()
    }
}