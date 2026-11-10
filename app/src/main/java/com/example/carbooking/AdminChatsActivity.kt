package com.example.carbooking

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminChatsActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance("default") }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var rvChatThreads: RecyclerView
    private lateinit var tvNoChats: TextView
    private lateinit var bottomNav: BottomNavigationView
    private val conversationList = mutableListOf<DocumentSnapshot>()
    private lateinit var adapter: ThreadAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_chats)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAdminChats)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvChatThreads = findViewById(R.id.rvAdminChatThreads)
        tvNoChats = findViewById(R.id.tvNoActiveChats)
        bottomNav = findViewById(R.id.bottomNavigationAdminChats)

        bottomNav.selectedItemId = R.id.menu_chat

        rvChatThreads.layoutManager = LinearLayoutManager(this)
        adapter = ThreadAdapter(conversationList) { doc ->
            val customerId = doc.getString("customerId") ?: doc.id
            val customerName = doc.getString("customerName") ?: "Customer"

            // Open private chat directly with this customer
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("CHAT_ROOM_ID", customerId)
                putExtra("SENDER_ROLE", "admin")
                putExtra("SENDER_NAME", "Fleet Admin")
                putExtra("CUSTOMER_NAME", customerName)
            }
            startActivity(intent)
        }
        rvChatThreads.adapter = adapter

        listenToConversations()

        // Admin Bottom Navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home, R.id.menu_bookings, R.id.menu_profile -> {
                    val intent = Intent(this, AdminDashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_chat -> true
                R.id.menu_logout -> {
                    auth.signOut()
                    val intent = Intent(this, WelcomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finishAffinity()
                    true
                }
                else -> false
            }
        }
    }

    private fun listenToConversations() {
        db.collection("chats")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    conversationList.clear()
                    conversationList.addAll(snapshot.documents)
                    adapter.notifyDataSetChanged()

                    if (conversationList.isEmpty()) {
                        tvNoChats.visibility = View.VISIBLE
                        rvChatThreads.visibility = View.GONE
                    } else {
                        tvNoChats.visibility = View.GONE
                        rvChatThreads.visibility = View.VISIBLE
                    }
                }
            }
    }

    class ThreadAdapter(
        private val list: List<DocumentSnapshot>,
        private val onClick: (DocumentSnapshot) -> Unit
    ) : RecyclerView.Adapter<ThreadAdapter.Holder>() {

        class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(android.R.id.text1)
            val tvMsg: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val doc = list[position]
            val name = doc.getString("customerName") ?: "Customer"
            val lastMsg = doc.getString("lastMessage") ?: "No messages"
            val unread = doc.getBoolean("unreadByAdmin") ?: false

            holder.tvName.text = if (unread) "🔴 $name (New Message)" else "👤 $name"
            holder.tvName.setTextColor(Color.WHITE)
            holder.tvName.textSize = 16f

            holder.tvMsg.text = lastMsg
            holder.tvMsg.setTextColor(if (unread) Color.parseColor("#D4AF37") else Color.parseColor("#8E95AA"))

            holder.itemView.setOnClickListener { onClick(doc) }
        }

        override fun getItemCount(): Int = list.size
    }
}