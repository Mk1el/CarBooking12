package com.example.carbooking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.carbooking.R
import com.example.carbooking.models.Booking
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

class AdminBookingsAdapter(
    private var bookings: List<Booking>,
    private val onApproveBooking: (Booking) -> Unit,
    private val onApproveReturn: (Booking) -> Unit,
    private val onDecline: (Booking) -> Unit
) : RecyclerView.Adapter<AdminBookingsAdapter.AdminBookingViewHolder>() {

    class AdminBookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCar: TextView = itemView.findViewById(R.id.tvAdminBookingCar)
        val tvStatus: TextView = itemView.findViewById(R.id.tvAdminBookingStatusBadge)
        val tvName: TextView = itemView.findViewById(R.id.tvAdminCustomerName)
        val tvEmail: TextView = itemView.findViewById(R.id.tvAdminCustomerEmail)
        val tvPhone: TextView = itemView.findViewById(R.id.tvAdminCustomerPhone)
        val tvPrice: TextView = itemView.findViewById(R.id.tvAdminBookingPrice)
        val llActions: LinearLayout = itemView.findViewById(R.id.llAdminApprovalActions)
        val btnApprove: MaterialButton = itemView.findViewById(R.id.btnAdminApproveBooking)
        val btnDecline: MaterialButton = itemView.findViewById(R.id.btnAdminDeclineBooking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminBookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking_review, parent, false)
        return AdminBookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminBookingViewHolder, position: Int) {
        val b = bookings[position]
        holder.tvCar.text = b.carModel
        holder.tvStatus.text = b.status
        holder.tvName.text = "Customer: ${b.customerName}"
        holder.tvEmail.text = "Email: ${b.customerEmail}"
        holder.tvPhone.text = "Phone: ${b.customerPhone}"

        val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(b.totalPrice)
        if (b.overdueFee > 0) {
            val overdueFormatted = NumberFormat.getNumberInstance(Locale.US).format(b.overdueFee)
            holder.tvPrice.text = "Total: KES $formattedPrice (Base + KES $overdueFormatted late fee for ${b.overdueMinutes} mins)"
        } else {
            holder.tvPrice.text = "Total: KES $formattedPrice (${b.duration} ${b.rentalType} • ${b.paymentMethod})"
        }

        when (b.status) {
            "PENDING_APPROVAL" -> {
                holder.llActions.visibility = View.VISIBLE
                holder.btnApprove.text = "Approve Booking"
                holder.btnApprove.setOnClickListener { onApproveBooking(b) }
                holder.btnDecline.visibility = View.VISIBLE
                holder.btnDecline.setOnClickListener { onDecline(b) }
            }
            "RETURN_PENDING" -> {
                holder.llActions.visibility = View.VISIBLE
                holder.btnApprove.text = "Approve Return & Release Car"
                holder.btnApprove.setOnClickListener { onApproveReturn(b) }
                holder.btnDecline.visibility = View.GONE
            }
            else -> {
                holder.llActions.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateList(newList: List<Booking>) {
        bookings = newList
        notifyDataSetChanged()
    }
}