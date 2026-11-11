package com.example.carbooking

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carbooking.adapter.AdminBookingsAdapter
import com.example.carbooking.adapter.UserAdapter
import com.example.carbooking.models.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminDashboardActivity : BaseActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance("default") }

    private lateinit var userAdapter: UserAdapter
    private lateinit var bookingReviewsAdapter: AdminBookingsAdapter
    private lateinit var carReviewsAdapter: AdminCarReviewsAdapter

    private var allBookingsList = listOf<Booking>()
    private var allCarReviewsList = listOf<CarReview>()
    private var isViewingCarReviews = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val layoutDashboard = findViewById<View>(R.id.layoutAdminHomeView)
        val layoutBookingReviews = findViewById<View>(R.id.layoutAdminBookingsReviewView)
        val layoutProfile = findViewById<View>(R.id.layoutAdminProfileView)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationAdmin)

        val tvTotalRevenue = findViewById<TextView>(R.id.tvAdminTotalRevenue)
        val tvTotalUsers = findViewById<TextView>(R.id.tvAdminTotalUsers)
        val tvActiveBookings = findViewById<TextView>(R.id.tvAdminActiveBookings)
        val tvPeakDaySummary = findViewById<TextView>(R.id.tvPeakDaySummary)

        val rvUsers = findViewById<RecyclerView>(R.id.rvAdminUsersList)
        val rvBookingReviews = findViewById<RecyclerView>(R.id.rvAdminBookingReviews)

        val btnAddCar = findViewById<MaterialButton>(R.id.btnAdminAddNewCar)
        val btnAddNewUser = findViewById<MaterialButton>(R.id.btnAdminAddNewUser)
        val btnReleaseFleet = findViewById<MaterialButton>(R.id.btnAdminReleaseFleet)

        val etAdminName = findViewById<EditText>(R.id.etAdminName)
        val etAdminEmail = findViewById<EditText>(R.id.etAdminEmail)
        val btnSaveAdmin = findViewById<MaterialButton>(R.id.btnUpdateAdminProfile)

        bottomNav.bringToFront()
        bottomNav.elevation = 16f

        bottomNav.post {
            val navHeight = bottomNav.height
            val extraPadding = (16 * resources.displayMetrics.density).toInt()
            val totalBottomPadding = navHeight + extraPadding

            listOf(rvUsers, rvBookingReviews, layoutDashboard, layoutBookingReviews, layoutProfile).forEach { v ->
                v?.let {
                    it.setPadding(it.paddingLeft, it.paddingTop, it.paddingRight, totalBottomPadding)
                    if (it is ViewGroup) {
                        it.clipToPadding = false
                    }
                }
            }
        }

        // 1. User Directory Setup
        userAdapter = UserAdapter(emptyList<UserProfile>()) { selectedUser ->
            showUserActionDialog(selectedUser)
        }
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = userAdapter

        // 2. Booking Approvals Adapter Setup
        bookingReviewsAdapter = AdminBookingsAdapter(
            bookings = emptyList(),
            onApproveBooking = { booking ->
                db.collection("bookings").document(booking.id).update("status", "APPROVED")
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Booking approved for ${booking.customerName}!", Toast.LENGTH_SHORT).show()
                    }
            },
            onApproveReturn = { booking ->
                db.collection("bookings").document(booking.id).update("status", "COMPLETED")
                    .addOnSuccessListener {
                        if (booking.carId.isNotEmpty()) {
                            db.collection("cars").document(booking.carId).update("available", true)
                        }
                        Toast.makeText(this, "✅ Return approved. ${booking.carModel} is back in fleet!", Toast.LENGTH_LONG).show()
                    }
            },
            onDecline = { booking ->
                db.collection("bookings").document(booking.id).update("status", "REJECTED")
                    .addOnSuccessListener {
                        if (booking.carId.isNotEmpty()) {
                            db.collection("cars").document(booking.carId).update("available", true)
                        }
                        Toast.makeText(this, "Booking declined. Vehicle restored to fleet.", Toast.LENGTH_SHORT).show()
                    }
            }
        )

        // 3. Customer Car Reviews Adapter Setup
        carReviewsAdapter = AdminCarReviewsAdapter(
            reviews = emptyList(),
            onReplyClick = { review ->
                showReplyToReviewDialog(review)
            },
            onChatClick = { review ->
                // Open live chat with the reviewer
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("CHAT_ROOM_ID", review.userId)
                    putExtra("SENDER_ROLE", "admin")
                    putExtra("SENDER_NAME", "Fleet Manager")
                }
                startActivity(intent)
            }
        )

        rvBookingReviews.layoutManager = LinearLayoutManager(this)
        rvBookingReviews.adapter = bookingReviewsAdapter

        // 4. Live Users Fetching
        db.collection("users").addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e("AdminDashboard", "Error fetching users", error)
                return@addSnapshotListener
            }
            if (snapshots != null) {
                tvTotalUsers.text = snapshots.size().toString()
                val users = snapshots.documents.map { doc ->
                    UserProfile(
                        uid = doc.id,
                        name = doc.getString("name") ?: doc.getString("email")?.substringBefore("@") ?: "Customer",
                        email = doc.getString("email") ?: "No email provided",
                        phone = doc.getString("phone") ?: "No phone",
                        location = doc.getString("location") ?: "Nairobi",
                        role = doc.getString("role") ?: "customer"
                    )
                }
                userAdapter.updateUsers(users)
            }
        }

        // 5. Live Bookings Listener
        db.collection("bookings")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    var totalKes = 0.0
                    var activeCount = 0
                    val allBookings = mutableListOf<Booking>()
                    val confirmedList = mutableListOf<Booking>()

                    for (doc in snapshots.documents) {
                        val booking = doc.toObject(Booking::class.java) ?: continue
                        allBookings.add(booking)
                        if (booking.status == "APPROVED" || booking.status == "CONFIRMED" || booking.status == "COMPLETED") {
                            totalKes += booking.totalPrice
                            if (booking.status != "COMPLETED") {
                                activeCount++
                            }
                            confirmedList.add(booking)
                        }
                    }

                    allBookingsList = allBookings
                    val formatted = NumberFormat.getNumberInstance(Locale.US).format(totalKes)
                    tvTotalRevenue.text = "KES $formatted"
                    tvActiveBookings.text = activeCount.toString()

                    if (!isViewingCarReviews) {
                        bookingReviewsAdapter.updateList(allBookings)
                    }
                    updateDailyAnalyticsGraph(confirmedList, tvPeakDaySummary)
                }
            }

        db.collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("AdminDashboard", "Error fetching reviews", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    allCarReviewsList = snapshots.documents.mapNotNull { doc ->
                        doc.toObject(CarReview::class.java)?.copy(id = doc.id)
                    }

                    if (isViewingCarReviews) {
                        carReviewsAdapter.updateList(allCarReviewsList)
                    }
                }
            }

        // 7. Button Actions
        btnAddCar.setOnClickListener {
            startActivity(Intent(this, AddCarActivity::class.java))
        }

        btnAddNewUser.setOnClickListener {
            startActivity(Intent(this, AddUserActivity::class.java))
        }

        btnReleaseFleet.setOnClickListener {
            db.collection("cars").whereEqualTo("available", false).get().addOnSuccessListener { query ->
                for (doc in query.documents) {
                    db.collection("cars").document(doc.id).update("available", true)
                }
                Toast.makeText(this, "Success: All vehicles restored to Available!", Toast.LENGTH_LONG).show()
            }
        }

        // Profile Update
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                etAdminName.setText(snapshot.getString("name") ?: "Administrator")
                etAdminEmail.setText(snapshot.getString("email") ?: "")
            }
        }

        btnSaveAdmin.setOnClickListener {
            val newName = etAdminName.text.toString().trim()
            if (newName.isNotEmpty()) {
                db.collection("users").document(uid).update("name", newName)
                    .addOnSuccessListener { Toast.makeText(this, "Success: Admin profile updated!", Toast.LENGTH_SHORT).show() }
            }
        }

        // 8. Navigation Switching
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    layoutDashboard.visibility = View.VISIBLE
                    layoutBookingReviews.visibility = View.GONE
                    layoutProfile.visibility = View.GONE
                    true
                }
                R.id.menu_bookings -> {
                    layoutDashboard.visibility = View.GONE
                    layoutBookingReviews.visibility = View.VISIBLE
                    layoutProfile.visibility = View.GONE
                    promptSwitchBookingsOrReviews(rvBookingReviews)
                    true
                }
                R.id.menu_profile -> {
                    layoutDashboard.visibility = View.GONE
                    layoutBookingReviews.visibility = View.GONE
                    layoutProfile.visibility = View.VISIBLE
                    true
                }
                R.id.menu_chat -> {
                    startActivity(Intent(this, AdminChatsActivity::class.java))
                    false
                }
                R.id.menu_logout -> {
                    auth.signOut()
                    val intent = Intent(this, WelcomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finishAffinity()
                    true
                }
                else -> false
            }
        }
    }

    private fun promptSwitchBookingsOrReviews(recyclerView: RecyclerView) {
        val options = arrayOf(
            "📋 Vehicle Bookings (${allBookingsList.size})",
            "⭐ Customer Car Reviews (${allCarReviewsList.size})"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Section to View")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        isViewingCarReviews = false
                        recyclerView.adapter = bookingReviewsAdapter
                        bookingReviewsAdapter.updateList(allBookingsList)
                    }
                    1 -> {
                        isViewingCarReviews = true
                        recyclerView.adapter = carReviewsAdapter
                        carReviewsAdapter.updateList(allCarReviewsList)
                    }
                }
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showReplyToReviewDialog(review: CarReview) {
        val input = EditText(this).apply {
            hint = "Type response to customer..."
            setText(review.adminReply)
        }

        AlertDialog.Builder(this)
            .setTitle("Reply to ${review.userName}")
            .setMessage("Vehicle: ${review.carModel}\nRating: ★ ${review.rating}\nFeedback: \"${review.comment}\"")
            .setView(input)
            .setPositiveButton("Post Response") { _, _ ->
                val replyText = input.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    db.collection("reviews").document(review.id)
                        .update("adminReply", replyText)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Response posted! Customer can now see it.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUserActionDialog(user: UserProfile) {
        val options = arrayOf("💬 Open Live Chat with Customer", "🗑️ Remove Scam Account")

        MaterialAlertDialogBuilder(this)
            .setTitle(user.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, ChatActivity::class.java).apply {
                            putExtra("CHAT_ROOM_ID", user.uid)
                            putExtra("SENDER_ROLE", "admin")
                            putExtra("SENDER_NAME", "Fleet Manager")
                        }
                        startActivity(intent)
                    }
                    1 -> {
                        confirmDeleteScamUser(user)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteScamUser(user: UserProfile) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Suspected Scam Account")
            .setMessage("Are you sure you want to permanently remove ${user.name} (${user.email}) from the database?")
            .setPositiveButton("Remove Permanently") { _, _ ->
                db.collection("users").document(user.uid).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Success: '${user.name}' has been deleted.", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDailyAnalyticsGraph(bookings: List<Booking>, tvSummary: TextView) {
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val dayRevenueMap = mutableMapOf(
            "Mon" to 0.0, "Tue" to 0.0, "Wed" to 0.0, "Thu" to 0.0,
            "Fri" to 0.0, "Sat" to 0.0, "Sun" to 0.0
        )

        for (b in bookings) {
            // FIX: Using timestampMillis which safely handles Date, Timestamp, or Long
            val day = dayFormat.format(Date(b.timestampMillis))
            if (dayRevenueMap.containsKey(day)) {
                dayRevenueMap[day] = dayRevenueMap[day]!! + b.totalPrice
            }
        }

        val maxRevenue = dayRevenueMap.values.maxOrNull() ?: 1.0
        val peakDay = dayRevenueMap.maxByOrNull { it.value }

        if (peakDay != null && peakDay.value > 0) {
            val peakFormatted = NumberFormat.getNumberInstance(Locale.US).format(peakDay.value)
            tvSummary.text = "Peak booking day: ${peakDay.key} (KES $peakFormatted)"
        } else {
            tvSummary.text = "Awaiting weekly booking data."
        }

        fun scaleHeight(revenue: Double): Int {
            val density = resources.displayMetrics.density
            val minHeightPx = (15 * density).toInt()
            val maxHeightPx = (100 * density).toInt()
            if (maxRevenue <= 0.0) return minHeightPx
            return (minHeightPx + ((revenue / maxRevenue) * (maxHeightPx - minHeightPx))).toInt()
        }

        setBarHeight(R.id.barMon, scaleHeight(dayRevenueMap["Mon"] ?: 0.0))
        setBarHeight(R.id.barTue, scaleHeight(dayRevenueMap["Tue"] ?: 0.0))
        setBarHeight(R.id.barWed, scaleHeight(dayRevenueMap["Wed"] ?: 0.0))
        setBarHeight(R.id.barThu, scaleHeight(dayRevenueMap["Thu"] ?: 0.0))
        setBarHeight(R.id.barFri, scaleHeight(dayRevenueMap["Fri"] ?: 0.0))
        setBarHeight(R.id.barSat, scaleHeight(dayRevenueMap["Sat"] ?: 0.0))
        setBarHeight(R.id.barSun, scaleHeight(dayRevenueMap["Sun"] ?: 0.0))
    }

    private fun setBarHeight(barId: Int, heightPx: Int) {
        val bar = findViewById<View>(barId) ?: return
        val params = bar.layoutParams
        params.height = heightPx
        bar.layoutParams = params
    }

    class AdminCarReviewsAdapter(
        private var reviews: List<CarReview>,
        private val onReplyClick: (CarReview) -> Unit,
        private val onChatClick: (CarReview) -> Unit
    ) : RecyclerView.Adapter<AdminCarReviewsAdapter.Holder>() {

        class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val tvCarModel: TextView = v.findViewById(R.id.tvAdminReviewCarModel)
            val tvRating: TextView = v.findViewById(R.id.tvAdminReviewRating)
            val tvAuthor: TextView = v.findViewById(R.id.tvAdminReviewAuthor)
            val tvComment: TextView = v.findViewById(R.id.tvAdminReviewComment)
            val llReply: LinearLayout = v.findViewById(R.id.llAdminReplyContainer)
            val tvReplyContent: TextView = v.findViewById(R.id.tvAdminReplyContent)
            val btnReply: MaterialButton = v.findViewById(R.id.btnReplyToReview)
            val btnChat: MaterialButton = v.findViewById(R.id.btnChatWithReviewer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_car_review, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = reviews[position]

            holder.tvCarModel.text = item.carModel
            holder.tvRating.text = "★ ${item.rating}"
            holder.tvAuthor.text = "By: ${item.userName} (${item.userEmail})"
            holder.tvComment.text = item.comment

            if (item.adminReply.isNotEmpty()) {
                holder.llReply.visibility = View.VISIBLE
                holder.tvReplyContent.text = item.adminReply
            } else {
                holder.llReply.visibility = View.GONE
            }

            holder.btnReply.setOnClickListener { onReplyClick(item) }
            holder.btnChat.setOnClickListener { onChatClick(item) }
        }

        override fun getItemCount(): Int = reviews.size

        fun updateList(newList: List<CarReview>) {
            reviews = newList
            notifyDataSetChanged()
        }
    }
}