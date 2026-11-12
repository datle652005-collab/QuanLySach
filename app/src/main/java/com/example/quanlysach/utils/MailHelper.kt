package com.example.quanlysach.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object MailHelper {

    private const val USER = "quanlysach08@gmail.com"
    private const val PASS = "rkniwxhewtjrdikt"

    private const val ADMIN_EMAIL = "quanlysach08@gmail.com"

    fun sendOTP(toEmail: String, otpCode: String) {
        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.port"] = "587"
        props["mail.smtp.ssl.trust"] = "smtp.gmail.com"

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(USER, PASS)
            }
        })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(USER))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            message.subject = "Mã xác minh OTP"
            message.setText("Mã OTP của bạn là: $otpCode")

            Transport.send(message)

        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    /**
     * Hàm gửi báo cáo
     */
    suspend fun sendReportToAdmin(
        type: String,       // "Báo cáo" / "Chặn" / "Không quan tâm"
        reason: String,
        streamerName: String,
        streamId: String,
        reporterUsername: String
    ) = withContext(Dispatchers.IO) {
        try {
            val props = Properties().apply {
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(USER, PASS)
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(USER))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(ADMIN_EMAIL))
                subject = "[Live App] $type từ người dùng"
                setText("""
                    Loại hành động : $type
                    Lý do          : $reason
                    Streamer       : $streamerName
                    Stream ID      : $streamId
                    Người báo cáo  : $reporterUsername
                    Thời gian      : ${Date()}
                """.trimIndent())
            }

            Transport.send(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}