package com.example.carbooking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.carbooking.R
import com.example.carbooking.models.UserProfile
import com.google.android.material.button.MaterialButton

class UserAdapter(
    private var users: List<UserProfile>,
    private val onDeleteScamUser: (UserProfile) -> Unit = {}
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvItemUserName)
        val tvEmail: TextView = itemView.findViewById(R.id.tvItemUserEmail)
        val tvPhone: TextView = itemView.findViewById(R.id.tvItemUserPhone)
        val tvRole: TextView = itemView.findViewById(R.id.tvItemUserRole)
        val tvId: TextView? = itemView.findViewById(R.id.tvItemUserId)
        val btnDelete: MaterialButton? = itemView.findViewById(R.id.btnDeleteScamUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_directory, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val u = users[position]
        holder.tvName.text = u.name
        holder.tvEmail.text = u.email
        holder.tvPhone.text = "${u.phone} • ${u.location}"
        holder.tvRole.text = u.role.uppercase()
        holder.tvId?.text = "UID: ${u.uid.take(12)}..."

        // Click on "Remove User" button
        holder.btnDelete?.setOnClickListener {
            onDeleteScamUser(u)
        }

        // Long press anywhere on the card as a fallback
        holder.itemView.setOnLongClickListener {
            onDeleteScamUser(u)
            true
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<UserProfile>) {
        users = newUsers
        notifyDataSetChanged()
    }
}