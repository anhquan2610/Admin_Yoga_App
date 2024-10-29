package com.example.yogaadmin.ui.addcourse

import androidx.lifecycle.ViewModel
import com.example.yogaadmin.data.Course
import com.example.yogaadmin.data.CourseRepository

class AddCourseViewModel : ViewModel() {

    fun addCourse(course: Course) {
        CourseRepository.addCourse(course)
    }
}