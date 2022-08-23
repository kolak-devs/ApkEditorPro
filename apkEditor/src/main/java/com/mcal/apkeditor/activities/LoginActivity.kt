package com.mcal.apkeditor.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.mcal.apkeditor.R
import com.mcal.common.activities.CustomizedLangActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : CustomizedLangActivity(), View.OnClickListener {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var login: Button
    private lateinit var signUp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        login = findViewById(R.id.login)
        login.setOnClickListener(this)
        signUp = findViewById(R.id.sign_up)
        signUp.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val email = email.text.toString()
        val password = password.text.toString()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show()
            return
        }

        when (v.id) {
            R.id.login -> {
                CoroutineScope(Dispatchers.Default).launch {
                    login(email, password)
                }
            }
            R.id.sign_up -> {
                CoroutineScope(Dispatchers.Default).launch {
                    login(email, password, true)
                }
            }
        }
    }

    private suspend fun login(email: String, pass: String, signUp: Boolean = false) =
        withContext(Dispatchers.IO) {
            val url =
                URL("https://timscriptov.ru/apkeditor/" + if (signUp) "signup.php" else "login.php")
            val con = url.openConnection() as HttpURLConnection
            con.doInput = true
            con.doOutput = true

            val writer = con.outputStream.bufferedWriter()
            writer.write("email=$email&password=$pass")
            writer.close()

            if (con.responseCode == 200) {
                val token = con.inputStream.bufferedReader().readText()
                withContext(Dispatchers.Main) {
                    PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
                        .edit { putString("token", token) }
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            } else {
                val msg = con.errorStream.bufferedReader().readText()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
}