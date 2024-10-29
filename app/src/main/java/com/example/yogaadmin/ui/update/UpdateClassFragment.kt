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
import com.example.yogaadmin.data.DatabaseHelper
import com.example.yogaadmin.data.YogaClass
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class UpdateClassFragment : Fragment() {

    private lateinit var editClassName: EditText
    private lateinit var editTeacherName: EditText
    private lateinit var editClassDate: EditText
    private lateinit var editFee: EditText
    private lateinit var editDuration: EditText
    private lateinit var editDescription: EditText
    private lateinit var buttonUpdate: Button
    private lateinit var imageViewBackClas: ImageView

    // Các CheckBox để chọn ngày học trong tuần
    private lateinit var checkBoxMonday: CheckBox
    private lateinit var checkBoxTuesday: CheckBox
    private lateinit var checkBoxWednesday: CheckBox
    private lateinit var checkBoxThursday: CheckBox
    private lateinit var checkBoxFriday: CheckBox
    private lateinit var checkBoxSaturday: CheckBox
    private lateinit var checkBoxSunday: CheckBox

    private var yogaClassId: Int = 0 // ID của lớp học cần cập nhật
    private var courseId: Int = 0 // ID của khóa học
    private val checkBoxList = mutableListOf<CheckBox>()

    private var courseStartDate: String? = null
    private var courseEndDate: String? = null

    companion object {
        fun newInstance(classId: Int, courseId: Int, firestoreClassId: String? ,startDate: String?, endDate: String?): UpdateClassFragment {
            val fragment = UpdateClassFragment() // Tạo một thể hiện mới của UpdateClassFragment

            val args = Bundle().apply {
                putInt("classId", classId)
                putInt("courseId", courseId)
                putString("firestoreClassId", firestoreClassId)
                putString("startDate", startDate)
                putString("endDate", endDate)
            }
            fragment.arguments = args // Gán arguments cho fragment
            Log.d("UpdateClassFragment", "Received Start Date: $startDate, End Date: $endDate")
            return fragment
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
            courseStartDate = it.getString("startDate")
            courseEndDate = it.getString("endDate")
        }

        // Khởi tạo các view
        editClassName = view.findViewById(R.id.editClassName)
        editTeacherName = view.findViewById(R.id.editTeacherName)
        editClassDate = view.findViewById(R.id.editClassDate)
        editFee = view.findViewById(R.id.editFee)
        editDuration = view.findViewById(R.id.editDuration)
        editDescription = view.findViewById(R.id.editDescription)
        buttonUpdate = view.findViewById(R.id.buttonUpdate)

        // Thêm InputFilter cho editFee và editDuration
        editFee.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                if (!Character.isDigit(source[i]) && source[i] != '.') {
                    return@InputFilter "" // Không cho phép nhập ký tự không phải số
                }
            }
            null // Chấp nhận giá trị nhập vào
        })

        editDuration.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                if (!Character.isDigit(source[i])) {
                    return@InputFilter "" // Không cho phép nhập ký tự không phải số
                }
            }
            null // Chấp nhận giá trị nhập vào
        })

        // Khởi tạo các CheckBox
        checkBoxMonday = view.findViewById(R.id.checkBoxMonday)
        checkBoxTuesday = view.findViewById(R.id.checkBoxTuesday)
        checkBoxWednesday = view.findViewById(R.id.checkBoxWednesday)
        checkBoxThursday = view.findViewById(R.id.checkBoxThursday)
        checkBoxFriday = view.findViewById(R.id.checkBoxFriday)
        checkBoxSaturday = view.findViewById(R.id.checkBoxSaturday)
        checkBoxSunday = view.findViewById(R.id.checkBoxSunday)

        // Thêm tất cả các CheckBox vào danh sách để quản lý
        checkBoxList.addAll(listOf(checkBoxMonday, checkBoxTuesday, checkBoxWednesday,
            checkBoxThursday, checkBoxFriday, checkBoxSaturday, checkBoxSunday))

        loadClassDetails() // Tải thông tin lớp học
        setCheckboxLimit() // Gọi hàm để giới hạn checkbox

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
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Thiết lập ngày bắt đầu và kết thúc của khóa học
        val minDate = courseStartDate?.let { dateFormat.parse(it)?.time }
        val maxDate = courseEndDate?.let { dateFormat.parse(it)?.time }

        // Hiển thị DatePickerDialog với giới hạn ngày
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                editClassDate.setText(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Thiết lập giới hạn cho DatePickerDialog
        minDate?.let { datePickerDialog.datePicker.minDate = it }
        maxDate?.let { datePickerDialog.datePicker.maxDate = it }

        Log.d("UpdateClassFragment", "Course Start Date: $courseStartDate, Course End Date: $courseEndDate")

        datePickerDialog.show()
    }

    private fun setCheckboxLimit() {
        // Giới hạn số lượng checkbox được chọn
        checkBoxList.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ ->
                val checkedCount = checkBoxList.count { it.isChecked }
                checkBoxList.forEach { cb ->
                    // Bỏ qua checkbox đã được chọn
                    cb.isEnabled = checkedCount < 2 || cb.isChecked
                }
            }
        }
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

            editFee.setText(it.fee.toString())
            editDuration.setText(it.classDuration)
            editDescription.setText(it.description)

            // Cập nhật trạng thái của các checkbox
            val days = it.days.split(",").map { day -> day.trim() }
            checkBoxMonday.isChecked = days.contains("Monday")
            checkBoxTuesday.isChecked = days.contains("Tuesday")
            checkBoxWednesday.isChecked = days.contains("Wednesday")
            checkBoxThursday.isChecked = days.contains("Thursday")
            checkBoxFriday.isChecked = days.contains("Friday")
            checkBoxSaturday.isChecked = days.contains("Saturday")
            checkBoxSunday.isChecked = days.contains("Sunday")

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
        val feeValue = editFee.text.toString().toDoubleOrNull() ?: 0.0
        val duration = editDuration.text.toString()
        val description = editDescription.text.toString()
        val selectedDays = checkBoxList.filter { it.isChecked }.map { it.text.toString() }

        // Kiểm tra dữ liệu hợp lệ trước khi cập nhật
        if (className.isBlank() || teacherName.isBlank() || classDate.isBlank()) {
            Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin bắt buộc!", Toast.LENGTH_SHORT).show()
            return
        }

        // Cập nhật thông tin trong SQLite
        val dbHelper = DatabaseHelper(requireContext())
        val updatedClass = YogaClass(
            classId = yogaClassId,
            className = className,
            teacherName = teacherName,
            classDate = classDate,
            fee = feeValue,
            classDuration = duration,
            description = description,
            courseId = courseId,
            days = selectedDays.joinToString(", "),
            firestoreClassId = arguments?.getString("firestoreClassId")
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
            "fee" to yogaClass.fee,
            "duration" to yogaClass.classDuration,
            "description" to yogaClass.description,
            "courseId" to yogaClass.courseId
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
