package com.example.yogaadmin.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.yogaadmin.R
import com.example.yogaadmin.ui.courselist.CourseListFragment
import com.example.yogaadmin.ui.MainActivity

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)


        val buttonViewCourses: Button = view.findViewById(R.id.buttonViewCourses)
        buttonViewCourses.setOnClickListener {
            (activity as MainActivity).loadFragment(CourseListFragment())
        }

        return view
    }
}
