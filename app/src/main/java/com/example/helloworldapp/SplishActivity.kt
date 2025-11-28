package com.example.helloworldapp

import android.widget.TextView
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.animation.Animator
import android.animation.AnimatorListenerAdapter

class SplishActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splish)

        val welcomeText = findViewById<TextView>(R.id.welcomeText)

        val fadeIn = ObjectAnimator.ofFloat(welcomeText, View.ALPHA, 0f, 1f).apply {
            duration = 1000
        }

        fadeIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {

                welcomeText.postDelayed({
                    startActivity(Intent(this@SplishActivity, LoginActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                }, 1500)
            }
        })


        fadeIn.start()
    }
}