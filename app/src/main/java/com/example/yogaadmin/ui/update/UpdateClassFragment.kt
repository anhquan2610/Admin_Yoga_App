package com.example.yogaadmin.ui.update

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.yogaadmin.R
import com.example.yogaadmin.data.Course
import com.example.yogaadmin.data.DatabaseHelper
import com.example.yogaadmin.data.YogaClass
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.*

class UpdateClassFragment : Fragment() {

    private lateinit var editClassName: EditText
    private lateinit var editTeacherName: EditText
    private lateinit var editClassDate: EditText
    private lateinit var editDescription: EditText
    private lateinit var buttonUpdate: Button
    private lateinit var imageViewBackClas: ImageView





    private var yogaClassId: Int = 0 // ID của lớp học cần cập nhật
    private var courseId: Int = 0 // ID của khóa học
    private var dayOfWeek: String? = null
    private val checkBoxList = mutableListOf<CheckBox>()

    private var courseDayOfWeek: String? = null


    companion object {
        fun newInstance(classId: Int, courseId: Int, firestoreClassId: String?, courseFirestoreId: String? ,dayOfWeek: String?): UpdateClassFragment {
            val fragment = UpdateClassFragment() // Tạo một thể hiện mới của UpdateClassFragment

            val args = Bundle().apply {
                putInt("classId", classId)
                putInt("courseId", courseId)
                putString("firestoreClassId", firestoreClassId)
                putString("dayOfWeek", dayOfWeek)
                putString("courseFirestoreId", courseFirestoreId)
            }
            fragment.arguments = args // Gán arguments cho fragment
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseId = it.getInt("courseId")
            dayOfWeek = it.getString("dayOfWeek")

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_update_class, container, false)


        imageViewBackClas = view.findViewById(R.id.imageViewBackClas)
        imageViewBackClas.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Lấy arguments từ Fragment
        arguments?.let {
            yogaClassId = it.getInt("classId", 0)
            courseId = it.getInt("courseId", 0)
            courseDayOfWeek = it.getString("dayOfWeek")

        }


        // Khởi tạo các view
        editClassName = view.findViewById(R.id.editClassName)
        editTeacherName = view.findViewById(R.id.editTeacherName)
        editClassDate = view.findViewById(R.id.editClassDate)
        editDescription = view.findViewById(R.id.editDescription)
        buttonUpdate = view.findViewById(R.id.buttonUpdate)




        // Thêm tất cả các CheckBox vào danh sách để quản lý


        loadClassDetails() // Tải thông tin lớp học

        // Hiển thị DatePickerDialog khi nhấn vào editClassDate
        editClassDate.setOnClickListener {
            showDatePickerDialog()
        }

        buttonUpdate.setOnClickListener {
            updateClassInfo() // Cập nhật thông tin lớp học
        }

        return view
    }


    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)

                // Kiểm tra xem ngày đã chọn có thứ trùng với dayOfWeek không
                if (isDayOfWeekMatch(selectedDate, dayOfWeek)) {
                    editClassDate.setText(selectedDate)
                } else {
                    Toast.makeText(requireContext(), "Ngày đã chọn không hợp lệ. Vui lòng chọn ngày có thứ phù hợp.", Toast.LENGTH_SHORT).show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }



    private fun isDayOfWeekMatch(selectedDate: String, dayOfWeek: String?): Boolean {
        // Chuyển đổi định dạng chuỗi thành ngày để so sánh
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val selected = sdf.parse(selectedDate)

        // Lấy thứ của ngày đã chọn
        val calendar = Calendar.getInstance().apply {
            time = selected
        }
        val selectedDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Chuyển đổi dayOfWeek từ chuỗi sang số thứ tự
        val dayOfWeekMap = mapOf(
            "Monday" to Calendar.MONDAY,
            "Tuesday" to Calendar.TUESDAY,
            "Wednesday" to Calendar.WEDNESDAY,
            "Thursday" to Calendar.THURSDAY,
            "Friday" to Calendar.FRIDAY,
            "Saturday" to Calendar.SATURDAY,
            "Sunday" to Calendar.SUNDAY
        )

        return dayOfWeekMap[dayOfWeek] == selectedDayOfWeek
    }


    private fun loadClassDetails() {
        val dbHelper = DatabaseHelper(requireContext())
        val yogaClass = dbHelper.getClassById(yogaClassId)
        yogaClass?.let {
            editClassName.setText(it.className)
            editTeacherName.setText(it.teacherName)

            // Hiển thị giá trị ngày từ cơ sở dữ liệu
            Log.d("UpdateClassFragment", "Class Date from DB: ${it.classDate}")

            // Đảm bảo rằng định dạng ngày là đúng
            val originalDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            try {
                // Phân tích ngày từ cơ sở dữ liệu
                val parsedDate = originalDateFormat.parse(it.classDate)
                // Nếu phân tích thành công, định dạng lại để hiển thị
                editClassDate.setText(originalDateFormat.format(parsedDate))
            } catch (e: Exception) {
                Log.e("UpdateClassFragment", "Error parsing date: ${e.message}")
                editClassDate.setText("") // Xóa hoặc đặt một giá trị mặc định
            }


            editDescription.setText(it.description)

            // Cập nhật trạng thái của các checkbox



            // Kiểm tra số lượng checkbox đã được chọn
            val checkedCount = checkBoxList.count { it.isChecked }
            if (checkedCount >= 2) {
                checkBoxList.forEach { cb -> if (!cb.isChecked) cb.isEnabled = false }
            }
        }
    }



    private fun updateClassInfo() {
        val className = editClassName.text.toString()
        val teacherName = editTeacherName.text.toString()
        val classDate = editClassDate.text.toString()
        val description = editDescription.text.toString()


        // Kiểm tra dữ liệu hợp lệ trước khi cập nhật
        if (className.isBlank() || teacherName.isBlank() || classDate.isBlank()) {
            Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin bắt buộc!", Toast.LENGTH_SHORT).show()
            return
        }

        // Cập nhật thông tin trong SQLite
        val dbHelper = DatabaseHelper(requireContext())
        courseId = arguments?.getInt("courseId") ?: 0
        yogaClassId = arguments?.getInt("classId")?:0
        val classYoga = dbHelper.getClassById(yogaClassId)



        val updatedClass = YogaClass(
            classId = yogaClassId,
            className = className,
            teacherName = teacherName,
            classDate = classDate,
            description = description,
            courseId = courseId,
            firestoreClassId = arguments?.getString("firestoreClassId"),
            courseFirestoreId = classYoga?.courseFirestoreId
        )

        val success = dbHelper.updateClass(updatedClass)

        if (success) {
            Toast.makeText(requireContext(), "Lớp học đã được cập nhật trong SQLite!", Toast.LENGTH_SHORT).show()

            // Cập nhật dữ liệu trong Firestore nếu firestoreClassId tồn tại
            updatedClass.firestoreClassId?.let { firestoreClassId ->
                updateClassInFirestore(firestoreClassId, updatedClass)
            } ?: run {
                // Thực hiện popBackStack nếu không có Firestore ID
                requireActivity().supportFragmentManager.popBackStack()
            }
        } else {
            Toast.makeText(requireContext(), "Có lỗi xảy ra khi cập nhật trong SQLite.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateClassInFirestore(firestoreClassId: String, yogaClass: YogaClass) {
        val firestore = FirebaseFirestore.getInstance()
        val classData = hashMapOf(
            "className" to yogaClass.className,
            "teacherName" to yogaClass.teacherName,
            "classDate" to yogaClass.classDate,
            "description" to yogaClass.description,
            "courseId" to yogaClass.courseId,
            "firestoreClassId" to yogaClass.firestoreClassId, // Đảm bảo lưu firestoreClassId
            "courseFirestoreId" to yogaClass.courseFirestoreId
        )

        firestore.collection("classes").document(firestoreClassId)
            .set(classData)
            .addOnSuccessListener {
                if (isAdded) {
                    Log.d("FirestoreUpdate", "Lớp học đã được cập nhật trên Firestore.")
                    Toast.makeText(requireContext(), "Lớp học đã được cập nhật trên Firestore!", Toast.LENGTH_SHORT).show()
                    // Pop back stack khi cập nhật thành công lên Firestore
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Log.e("FirestoreUpdate", "Lỗi khi cập nhật lớp học trên Firestore: ${e.message}")
                    Toast.makeText(requireContext(), "Có lỗi khi cập nhật trên Firestore.", Toast.LENGTH_SHORT).show()
                }
            }
    }






}
