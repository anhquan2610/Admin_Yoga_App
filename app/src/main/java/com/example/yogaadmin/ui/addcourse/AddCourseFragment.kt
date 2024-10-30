package com.example.yogaadmin.ui.addcourse

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.yogaadmin.R
import com.example.yogaadmin.data.Course
import com.example.yogaadmin.data.DatabaseHelper
import com.example.yogaadmin.ui.courselist.CourseListFragment
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import androidx.appcompat.app.AlertDialog
import com.example.yogaadmin.data.CourseRepository

class AddCourseFragment : Fragment() {

    private lateinit var spinnerDayOfWeek: Spinner
    private lateinit var spinnerCourseType: Spinner
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo Firestore ở đây để chắc chắn rằng biến firestore đã được khởi tạo
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_course, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editTextCourseName: EditText = view.findViewById(R.id.editTextCourseName)
        spinnerDayOfWeek = view.findViewById(R.id.spinnerDayOfWeek)
        val editTextCapacity: EditText = view.findViewById(R.id.editTextCapacity)
        val editTextDescription: EditText = view.findViewById(R.id.editTextDescription)
        spinnerCourseType = view.findViewById(R.id.spinnerCourseType)
        val buttonAddCourse: Button = view.findViewById(R.id.buttonAddCourse)

        // Trường mới: Thời gian, Giá mỗi lớp, Thời lượng
        val editTextTime: EditText = view.findViewById(R.id.editTextTime)
        val editTextPrice: EditText = view.findViewById(R.id.editTextPrice)
        val editTextDuration: EditText = view.findViewById(R.id.editTextDuration)

        editTextTime.setOnClickListener { showTimePickerDialog(editTextTime) }

        // Cấu hình dữ liệu cho spinnerDayOfWeek
        val daysOfWeek = resources.getStringArray(R.array.days_of_week) // Đảm bảo bạn đã định nghĩa mảng này trong strings.xml
        val dayOfWeekAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, daysOfWeek)
        dayOfWeekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDayOfWeek.adapter = dayOfWeekAdapter

        val buttonBack: ImageView = view.findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            // Kiểm tra nếu có bất kỳ thông tin nào đã được nhập
            if (editTextCourseName.text.isNotEmpty() ||
                editTextCapacity.text.isNotEmpty() ||
                editTextDescription.text.isNotEmpty() ||
                spinnerCourseType.selectedItemPosition != 0
            ) {
                // Hiển thị hộp thoại xác nhận
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("Xác nhận")
                    setMessage("Bạn có chắc chắn muốn quay lại mà không lưu thay đổi?")
                    setPositiveButton("Có") { _, _ ->
                        // Quay lại trang danh sách khóa học
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, CourseListFragment())
                            .commit()
                    }
                    setNegativeButton("Không", null)
                    show()
                }
            } else {
                // Nếu không có thông tin nào đã nhập, trực tiếp quay lại trang danh sách
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, CourseListFragment())
                    .commit()
            }
        }

        buttonAddCourse.setOnClickListener {
            val courseName = editTextCourseName.text.toString()
            val selectedDay = spinnerDayOfWeek.selectedItem.toString()
            val capacity = editTextCapacity.text.toString().toIntOrNull() ?: 0
            val description = editTextDescription.text.toString()
            val courseType = spinnerCourseType.selectedItem.toString()
            val time = editTextTime.text.toString()
            val price = editTextPrice.text.toString().toDoubleOrNull()
            val duration = editTextDuration.text.toString().toIntOrNull()

            if (courseName.isNotEmpty() && selectedDay.isNotEmpty() && capacity > 0 && courseType.isNotEmpty() &&
                time.isNotEmpty() && price != null && duration != null) {
                // Tạo đối tượng khóa học mới mà không gán ID ngay
                val course = Course(
                    name = courseName, dayOfWeek = selectedDay, capacity = capacity,
                    description = description, courseType = courseType, time = time,
                    price = price, duration = duration
                )
                uploadData(course)
            } else {
                Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun uploadData(course: Course) {
        // Kiểm tra xem có kết nối Internet không
        if (!isInternetAvailable(requireContext())) {
            Log.d("InternetCheck", "Không có kết nối Internet.")
            Toast.makeText(requireContext(), "Không có kết nối Internet", Toast.LENGTH_SHORT).show()
            return // Ngưng thực hiện nếu không có Internet
        }

        // Tạo ID duy nhất cho khóa học
        val courseId = firestore.collection("courses").document().id

        // Tạo đối tượng dữ liệu để lưu vào Firestore
        val courseData = hashMapOf(
            "id" to courseId,
            "name" to course.name,
            "dayOfWeek" to course.dayOfWeek,
            "capacity" to course.capacity,
            "description" to course.description,
            "courseType" to course.courseType,
            "time" to course.time,
            "price" to course.price,
            "duration" to course.duration
        )

        firestore.collection("courses").document(courseId)
            .set(courseData)
            .addOnSuccessListener {
                // Cập nhật khóa học trong SQLite với ID Firestore
                val dbHelper = DatabaseHelper(requireContext())
                val updatedCourse = course.copy(firestoreId = courseId) // Cập nhật ID Firestore vào đối tượng course
                dbHelper.addCourse(updatedCourse) // Thêm khóa học với ID mới vào SQLite

                // Cập nhật trạng thái đồng bộ hóa
                CourseRepository.updateCourseSyncStatus(updatedCourse.id, true)

                Log.d("CourseUpdate", "Cập nhật Firestore ID cho khóa học với ID: ${updatedCourse.id}")
                Toast.makeText(requireContext(), "Dữ liệu đã được đẩy lên Firestore", Toast.LENGTH_SHORT).show()

                // Quay lại trang danh sách khóa học
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, CourseListFragment())
                    .commit()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreUpload", "Lỗi khi đẩy dữ liệu lên Firestore: ${e.message}")
                Toast.makeText(requireContext(), "Lỗi khi đẩy dữ liệu lên Firestore", Toast.LENGTH_SHORT).show()
            }
    }



    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork?.let {
            connectivityManager.getNetworkCapabilities(it)
        }

        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }


    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            editText.setText(formattedTime)
        }, hour, minute, true).show()
    }
}
