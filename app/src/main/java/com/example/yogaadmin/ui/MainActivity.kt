package com.example.yogaadmin.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.yogaadmin.R

import com.example.yogaadmin.ui.home.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Khởi tạo BottomNavigationView
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Thiết lập Fragment mặc định
        loadFragment(HomeFragment())

        // Thiết lập listener cho BottomNavigationView
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())
                    true
                }

                // Thêm các mục navigation khác nếu cần
                else -> false
            }
        }

    }

    // Phương thức loadFragment để thay thế Fragment
    fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
