package com.example.yogaadmin.data
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

data class Course(
    val id: Int = 0,
    val name: String,
    val startDate: String,
    val endDate: String,
    val capacity: Int,
    val description: String?,
    val courseType: String,
    var isSynced: Boolean = false,
    var firestoreId: String? = null

)

object CourseRepository {
    private val courses = mutableListOf<Course>()
    private var nextId = 1
    private val firestore = FirebaseFirestore.getInstance()

    fun addCourse(course: Course) {
        val newCourse = course.copy(id = nextId) // Gán ID
        courses.add(newCourse)
        nextId++
        Log.d("CourseRepository", "Khoá học đã được thêm: $newCourse")
    }


    fun getCourses(): List<Course> {
        Log.d("CourseRepository", "Danh sách khoá học: $courses")
        return courses
    }

    fun updateCourseSyncStatus(courseId: Int, isSynced: Boolean) {
        courses.find { it.id == courseId }?.let {
            it.isSynced = isSynced // Đây là kiểu boolean
            Log.d("CourseRepository", "Cập nhật trạng thái đồng bộ cho khóa học: ${it.name}, isSynced: $isSynced")
        } ?: Log.d("CourseRepository", "Khóa học với ID $courseId không tìm thấy.")
    }





}
