package com.example.yogaadmin.ui.updatecourse

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.yogaadmin.R
import com.example.yogaadmin.data.Course
import com.example.yogaadmin.data.DatabaseHelper
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class UpdateCourseFragment : Fragment() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var firestore: FirebaseFirestore
    private var courseId: Int = 0
    private lateinit var spinnerDayOfWeek: Spinner
    private lateinit var editTextTime: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_update_course, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = DatabaseHelper(requireContext())
        firestore = FirebaseFirestore.getInstance()
        courseId = arguments?.getInt("courseId") ?: 0

        val course = databaseHelper.getCourseById(courseId)

        if (course != null && course.id != 0) {
            val nameEditText: EditText = view.findViewById(R.id.editTextCourseName)
            val capacityEditText: EditText = view.findViewById(R.id.editTextCapacity)
            val descriptionEditText: EditText = view.findViewById(R.id.editTextDescription)
            val typeEditText: EditText = view.findViewById(R.id.editTextCourseType)
            spinnerDayOfWeek = view.findViewById(R.id.spinnerDayOfWeekUD)
            editTextTime = view.findViewById(R.id.editTextTimeUD)
            val durationEditText: EditText = view.findViewById(R.id.editTextDurationUD)
            val priceEditText: EditText = view.findViewById(R.id.editTextPriceUD)

            nameEditText.setText(course.name)
            capacityEditText.setText(course.capacity.toString())
            descriptionEditText.setText(course.description)
            typeEditText.setText(course.courseType)
            spinnerDayOfWeek.setSelection(getDayOfWeekPosition(course.dayOfWeek)) // Giả định có hàm getDayOfWeekPosition
            editTextTime.setText(course.time)
            durationEditText.setText(course.duration.toString())
            priceEditText.setText(course.price.toString())


            editTextTime.setOnClickListener { showTimePickerDialog(editTextTime) }

            val updateButton: Button = view.findViewById(R.id.buttonUpdate)
            updateButton.setOnClickListener {
                val updatedCourseName = nameEditText.text.toString()
                val updatedDayOfWeek = spinnerDayOfWeek.selectedItem.toString()
                val updatedTime = editTextTime.text.toString()
                val updatedDuration: Int = durationEditText.text.toString().toIntOrNull() ?: 0 // Chuyển đổi sang Int
                val updatedPrice: Double = priceEditText.text.toString().toDoubleOrNull() ?: 0.0 // Chuyển đổi sang Double
                val updatedCapacity = capacityEditText.text.toString().toIntOrNull() ?: 0
                val updatedDescription = descriptionEditText.text.toString()
                val updatedCourseType = typeEditText.text.toString()

                if (updatedCourseName.isNotEmpty() &&
                    updatedDayOfWeek.isNotEmpty() &&
                    updatedCapacity > 0 &&
                    updatedCourseType.isNotEmpty()
                )
                {

                val updatedCourse = Course(
                        id = courseId,
                        name = updatedCourseName,
                        dayOfWeek = updatedDayOfWeek,
                        time = updatedTime,
                        duration = updatedDuration,
                        price = updatedPrice,
                        capacity = updatedCapacity,
                        description = updatedDescription,
                        courseType = updatedCourseType,
                        firestoreId = course.firestoreId // Giữ lại firestoreId để cập nhật vào Firestore
                    )

                    // Cập nhật khóa học trong SQLite
                    databaseHelper.updateCourse(updatedCourse)

                    // Cập nhật khóa học trong Firestore
                    updateCourseInFirestore(updatedCourse)

                } else {
                    Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Khóa học không tồn tại", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }
        val backButton: ImageView = view.findViewById(R.id.imageViewBack)
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
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

    private fun getDayOfWeekPosition(dayOfWeek: String): Int {
        val daysOfWeek = resources.getStringArray(R.array.days_of_week)
        return daysOfWeek.indexOf(dayOfWeek)
    }



    private fun updateCourseInFirestore(course: Course) {
        course.firestoreId?.let { firestoreId ->
            firestore.collection("courses").document(firestoreId)
                .set(course)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Khóa học đã được cập nhật trên Firestore.", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Có lỗi xảy ra khi cập nhật khóa học trên Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
