package com.example.carbooking

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    // SENDER CREDENTIALS:
    // Use your Gmail address and a 16-character Google "App Password"
    private const val SENDER_EMAIL = "moseskilonzo2001@gmail.com"
    private const val SENDER_PASSWORD = "usku sfkd eyhq araf" // 16-character App Password

    fun sendOtpEmail(toEmail: String, otp: String, onResult: (Boolean, String?) -> Unit) {
        val mainHandler = Handler(Looper.getMainLooper())

        Thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.socketFactory.port", "465")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.port", "465")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                val mimeMessage = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, "Car Booking Luxury Fleet"))
                    addRecipient(Message.RecipientType.TO, InternetAddress(toEmail))
                    subject = "Your Verification Code: $otp"
                    setText(
                        """
                        Hello,

                        Your 6-digit verification code for Car Booking is:

                        $otp

                        This code is required to access your account. Please do not share it with anyone.

                        Best regards,
                        The Car Booking Team
                        """.trimIndent()
                    )
                }

                Transport.send(mimeMessage)
                mainHandler.post { onResult(true, null) }

            } catch (e: Exception) {
                Log.e("EmailSender", "Failed to send email", e)
                mainHandler.post { onResult(false, e.message) }
            }
        }.start()
    }
}