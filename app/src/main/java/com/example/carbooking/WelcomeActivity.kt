package com.example.carbooking

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton

class WelcomeActivity: AppCompatActivity() {
    private val mercedesImageUrl = "https://images.unsplash.com/photo-1617814076367-b759c7d7e738?q=80&w=1200&auto=format&fit=crop"
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val ivCarBackground = findViewById<ImageView>(R.id.ivCarBackground)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        Glide.with(this)
            .load(mercedesImageUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .into(ivCarBackground)

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}