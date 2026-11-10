package com.example.carbooking

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carbooking.models.ChatMessage
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class ChatActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance("default") }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var toolbar: Toolbar
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: MaterialButton
    private lateinit var bottomNav: BottomNavigationView

    private var chatRoomId: String = ""
    private var actualRole: String = "customer"
    private var senderName: String = ""
    private var customerDisplayName: String = "Customer"

    private val messagesList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private var messagesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        toolbar = findViewById(R.id.toolbarChat)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvMessages = findViewById(R.id.rvChatMessages)
        etMessage = findViewById(R.id.etChatMessage)
        btnSend = findViewById(R.id.btnSendMessage)
        bottomNav = findViewById(R.id.bottomNavigationChat)

        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.layoutManager = layoutManager
        chatAdapter = ChatAdapter(messagesList, currentUid)
        rvMessages.adapter = chatAdapter

        // Verify actual user role from Firestore to enforce Customer -> Admin isolation
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { userDoc ->
                val dbRole = userDoc.getString("role")?.lowercase()?.trim() ?: "customer"
                val dbName = userDoc.getString("name")?.takeIf { it.isNotBlank() }
                    ?: auth.currentUser?.email?.substringBefore("@")
                    ?: "Customer"

                if (dbRole == "admin") {
                    actualRole = "admin"
                    senderName = "Fleet Admin"
                    // Admin opens the specific customer's chat thread
                    chatRoomId = intent.getStringExtra("CHAT_ROOM_ID") ?: ""
                    customerDisplayName = intent.getStringExtra("CUSTOMER_NAME") ?: "Customer"

                    if (chatRoomId.isEmpty()) {
                        Toast.makeText(this, "Select a customer conversation first.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, AdminChatsActivity::class.java))
                        finish()
                        return@addOnSuccessListener
                    }

                    supportActionBar?.title = "Chat: $customerDisplayName"
                    supportActionBar?.subtitle = "Replying as Fleet Admin"

                    // Mark thread as read by Admin
                    db.collection("chats").document(chatRoomId)
                        .set(mapOf("unreadByAdmin" to false), SetOptions.merge())

                    setupBottomNavigation(isAdmin = true)
                } else {
                    // STRICT RULE: Customer is ALWAYS locked to their own UID room (can ONLY chat with Admin)
                    actualRole = "customer"
                    chatRoomId = currentUid
                    senderName = dbName
                    customerDisplayName = dbName

                    supportActionBar?.title = "Mercedes-Benz Admin Support"
                    supportActionBar?.subtitle = "Private chat with Fleet Manager"

                    setupBottomNavigation(isAdmin = false)
                }

                listenToMessages()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Unable to verify session.", Toast.LENGTH_SHORT).show()
                finish()
            }

        btnSend.setOnClickListener {
            sendMessage(currentUid)
        }
    }

    private fun sendMessage(currentUid: String) {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty() || chatRoomId.isEmpty()) return

        val now = System.currentTimeMillis()
        val msgRef = db.collection("chats").document(chatRoomId).collection("messages").document()

        val msg = ChatMessage(
            id = msgRef.id,
            senderId = currentUid,
            senderName = senderName,
            senderRole = actualRole,
            message = text,
            timestamp = now
        )

        etMessage.setText("")

        // 1. Save message in the conversation subcollection
        msgRef.set(msg).addOnSuccessListener {
            // 2. Update parent thread metadata so it appears immediately on Admin's Inbox
            val threadSummary = mutableMapOf<String, Any>(
                "customerId" to chatRoomId,
                "lastMessage" to text,
                "lastSenderRole" to actualRole,
                "updatedAt" to now,
                "unreadByAdmin" to (actualRole == "customer")
            )
            if (actualRole == "customer") {
                threadSummary["customerName"] = customerDisplayName
                threadSummary["customerEmail"] = auth.currentUser?.email ?: ""
            } else if (customerDisplayName.isNotBlank() && customerDisplayName != "Customer") {
                threadSummary["customerName"] = customerDisplayName
            }

            db.collection("chats").document(chatRoomId).set(threadSummary, SetOptions.merge())
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenToMessages() {
        messagesListener?.remove()
        messagesListener = db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messagesList.clear()
                    for (doc in snapshot.documents) {
                        val msg = doc.toObject(ChatMessage::class.java)
                        if (msg != null) messagesList.add(msg)
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (messagesList.isNotEmpty()) {
                        rvMessages.scrollToPosition(messagesList.size - 1)
                    }

                    // Clear unread flag while Admin has the chat open
                    if (actualRole == "admin" && snapshot.documents.isNotEmpty()) {
                        db.collection("chats").document(chatRoomId)
                            .set(mapOf("unreadByAdmin" to false), SetOptions.merge())
                    }
                }
            }
    }

    private fun setupBottomNavigation(isAdmin: Boolean) {
        bottomNav.menu.clear()
        bottomNav.inflateMenu(if (isAdmin) R.menu.menu_admin_bottom else R.menu.menu_customer_bottom)
        bottomNav.selectedItemId = R.id.menu_chat

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_chat -> {
                    if (isAdmin) {
                        // Return to Admin's full list of customer chats
                        startActivity(Intent(this, AdminChatsActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.menu_home, R.id.menu_bookings, R.id.menu_profile -> {
                    val target = if (isAdmin) AdminDashboardActivity::class.java else CustomerHomeActivity::class.java
                    val intent = Intent(this, target).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                    true
                }
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

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }

    class ChatAdapter(
        private val list: List<ChatMessage>,
        private val currentUid: String
    ) : RecyclerView.Adapter<ChatAdapter.Holder>() {

        class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val root: LinearLayout = v.findViewById(R.id.llMessageRoot)
            val tvSender: TextView = v.findViewById(R.id.tvMessageSender)
            val cardBubble: MaterialCardView = v.findViewById(R.id.cardMessageBubble)
            val tvBody: TextView = v.findViewById(R.id.tvMessageBody)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = list[position]
            val isMe = item.senderId == currentUid

            holder.tvSender.text = if (isMe) {
                "You"
            } else if (item.senderRole == "admin") {
                "Fleet Admin ⚡"
            } else {
                item.senderName.ifBlank { "Customer" }
            }

            holder.tvBody.text = item.message
            holder.root.gravity = if (isMe) Gravity.END else Gravity.START

            if (isMe) {
                holder.cardBubble.setCardBackgroundColor(Color.parseColor("#D4AF37"))
                holder.tvBody.setTextColor(Color.parseColor("#0C0D14"))
            } else {
                holder.cardBubble.setCardBackgroundColor(Color.parseColor("#1E2235"))
                holder.tvBody.setTextColor(Color.WHITE)
            }
        }

        override fun getItemCount(): Int = list.size
    }
}