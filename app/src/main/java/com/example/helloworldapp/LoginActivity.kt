package com.example.helloworldapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.content.Intent

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.et_username)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnDebugEnter = findViewById<Button>(R.id.btn_debug_enter)
        
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()
            val dbHelper = MyDatabaseHelper(this)

            val isSuccess = dbHelper.checkLogin(username, password)

            if (isSuccess) {
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show()
            }
        }

        btnDebugEnter.setOnClickListener {
            Toast.makeText(this, "调试模式：直接进入", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}