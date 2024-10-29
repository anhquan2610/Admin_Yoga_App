package com.example.yogaadmin.ui.updatecourse

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText

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
            startDateEditText = view.findViewById(R.id.editTextStartDate)
            endDateEditText = view.findViewById(R.id.editTextEndDate)
            val capacityEditText: EditText = view.findViewById(R.id.editTextCapacity)
            val descriptionEditText: EditText = view.findViewById(R.id.editTextDescription)
            val typeEditText: EditText = view.findViewById(R.id.editTextCourseType)

            nameEditText.setText(course.name)
            startDateEditText.setText(course.startDate)
            endDateEditText.setText(course.endDate)
            capacityEditText.setText(course.capacity.toString())
            descriptionEditText.setText(course.description)
            typeEditText.setText(course.courseType)

            startDateEditText.setOnClickListener { showDatePickerDialog(startDateEditText) }
            endDateEditText.setOnClickListener { showDatePickerDialog(endDateEditText) }

            val updateButton: Button = view.findViewById(R.id.buttonUpdate)
            updateButton.setOnClickListener {
                val updatedCourseName = nameEditText.text.toString()
                val updatedStartDate = startDateEditText.text.toString()
                val updatedEndDate = endDateEditText.text.toString()
                val updatedCapacity = capacityEditText.text.toString().toIntOrNull() ?: 0
                val updatedDescription = descriptionEditText.text.toString()
                val updatedCourseType = typeEditText.text.toString()

                if (updatedCourseName.isNotEmpty() && updatedStartDate.isNotEmpty() &&
                    updatedEndDate.isNotEmpty() && updatedCapacity > 0 && updatedCourseType.isNotEmpty()) {

                    val startDateParts = updatedStartDate.split("/").map { it.toInt() }
                    val endDateParts = updatedEndDate.split("/").map { it.toInt() }

                    if (endDateParts[2] < startDateParts[2] ||
                        (endDateParts[2] == startDateParts[2] && endDateParts[1] < startDateParts[1]) ||
                        (endDateParts[2] == startDateParts[2] && endDateParts[1] == startDateParts[1] && endDateParts[0] <= startDateParts[0])) {
                        Toast.makeText(requireContext(), "Ngày kết thúc phải sau ngày bắt đầu", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val updatedCourse = Course(
                        id = courseId,
                        name = updatedCourseName,
                        startDate = updatedStartDate,
                        endDate = updatedEndDate,
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

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            editText.setText(formattedDate)
        }, year, month, day)

        datePickerDialog.show()
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
