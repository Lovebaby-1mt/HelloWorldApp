package com.example.helloworldapp

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 个人中心 Activity
 * 功能：
 * 1. 展示用户头像、昵称、签名等信息
 * 2. 提供修改个人信息的功能 (保存至 SharedPreferences)
 * 3. 提供静态菜单入口（如我的收藏、浏览历史等）
 * 4. 底部导航栏跳转
 */
class UserInfoActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        setupUserInfoLogic()
        setupBottomNavigation()
    }

    /**
     * 初始化用户信息及交互逻辑
     * 读取本地存储 (SharedPreferences) 中的数据并显示
     */
    private fun setupUserInfoLogic() {
        val tvName = findViewById<TextView>(R.id.tv_name)
        val tvSignature = findViewById<TextView>(R.id.tv_signature)
        val itemPersonalInfo = findViewById<View>(R.id.item_personal_info)
        val itemFavorites = findViewById<View>(R.id.item_favorites)

        // 使用 SharedPreferences 存储用户配置
        val sp = getSharedPreferences("user_info", Context.MODE_PRIVATE)
        val savedName = sp.getString("name", "用户名")
        val savedSignature = sp.getString("signature", "欢迎来到信息App")

        tvName.text = savedName
        tvSignature.text = savedSignature

        // 点击“个人信息”项弹出修改对话框
        itemPersonalInfo.setOnClickListener {
            showEditDialog(sp, tvName, tvSignature)
        }

        // 点击“我的收藏”
        itemFavorites.setOnClickListener {
            Toast.makeText(this, "点击了我的收藏", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 显示修改信息的弹窗
     * @param sp SharedPreferences 实例，用于保存修改
     * @param tvName 昵称 TextView，用于实时更新显示
     * @param tvSignature 签名 TextView，用于实时更新显示
     */
    private fun showEditDialog(sp: android.content.SharedPreferences, tvName: TextView, tvSignature: TextView) {
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.setPadding(60, 40, 60, 40)

        val etNewName = EditText(this)
        etNewName.hint = "请输入新昵称（留空则不改）"
        val etNewSignature = EditText(this)
        etNewSignature.hint = "请输入新签名（留空则不改）"

        container.addView(etNewName)
        container.addView(etNewSignature)

        AlertDialog.Builder(this)
            .setTitle("修改个人信息")
            .setView(container)
            .setPositiveButton("确定") { _, _ ->
                val inputName = etNewName.text.toString().trim()
                val inputSig = etNewSignature.text.toString().trim()
                var toastMsg = ""

                // 保存昵称
                if (inputName.isNotEmpty()) {
                    sp.edit().putString("name", inputName).apply()
                    tvName.text = inputName
                    toastMsg += "昵称 "
                }

                // 保存签名
                if (inputSig.isNotEmpty()) {
                    sp.edit().putString("signature", inputSig).apply()
                    tvSignature.text = inputSig
                    toastMsg += "签名 "
                }

                if (toastMsg.isNotEmpty()) {
                    Toast.makeText(this, "已更新：$toastMsg", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "未做任何修改", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 配置底部导航栏点击事件
     */
    private fun setupBottomNavigation() {
        val tabHome = findViewById<View>(R.id.tab_home)
        val tabFeed = findViewById<View>(R.id.tab_feed)
        val tabMine = findViewById<View>(R.id.tab_mine)

        // 点击首页：结束当前页面，返回上一级（通常是 HomeActivity）
        tabHome.setOnClickListener {
            finish()
            overridePendingTransition(0, 0) // 取消转场动画
        }

        tabFeed.setOnClickListener {
            Toast.makeText(this, "发现功能开发中...", Toast.LENGTH_SHORT).show()
        }

        tabMine.setOnClickListener {
            Toast.makeText(this, "已经在个人中心", Toast.LENGTH_SHORT).show()
        }
    }
}