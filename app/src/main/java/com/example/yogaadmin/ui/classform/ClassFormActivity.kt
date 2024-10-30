package com.example.yogaadmin.ui.classform

import android.app.Activity
import android.app.DatePickerDialog
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yogaadmin.R
import com.example.yogaadmin.data.Course
import com.example.yogaadmin.data.DatabaseHelper
import com.example.yogaadmin.data.YogaClass
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ClassFormActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var firestore: FirebaseFirestore
    private lateinit var classNameEditText: EditText
    private lateinit var teacherNameEditText: EditText
    private lateinit var classDateEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var feeEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView
    private lateinit var courseList: List<Course>
    private lateinit var durationEditText: EditText

    private var startDate: String? = null // Ngày bắt đầu khóa học
    private var endDate: String? = null // Ngày kết thúc khóa học
    private var isDataEntered = false // Cờ theo dõi dữ liệu nhập vào
    private var selectedDaysCount = 0 // Biến đếm số checkbox đã chọn

    private lateinit var checkBoxMonday: CheckBox
    private lateinit var checkBoxTuesday: CheckBox
    private lateinit var checkBoxWednesday: CheckBox
    private lateinit var checkBoxThursday: CheckBox
    private lateinit var checkBoxFriday: CheckBox
    private lateinit var checkBoxSaturday: CheckBox
    private lateinit var checkBoxSunday: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_form)

        dbHelper = DatabaseHelper(this)
        firestore = FirebaseFirestore.getInstance() // Khởi tạo Firestore

        // Khai báo các EditText và Button
        classNameEditText = findViewById(R.id.editTextClassName)
        teacherNameEditText = findViewById(R.id.editTextTeacherName)
        classDateEditText = findViewById(R.id.editTextClassDate)
        feeEditText = findViewById(R.id.editTextFee)
        descriptionEditText = findViewById(R.id.editTextDescription)
        saveButton = findViewById(R.id.buttonSave)
        backButton = findViewById(R.id.buttonBack)
        durationEditText = findViewById(R.id.editTextClassDuration)

        checkBoxMonday = findViewById(R.id.checkBoxMonday)
        checkBoxTuesday = findViewById(R.id.checkBoxTuesday)
        checkBoxWednesday = findViewById(R.id.checkBoxWednesday)
        checkBoxThursday = findViewById(R.id.checkBoxThursday)
        checkBoxFriday = findViewById(R.id.checkBoxFriday)
        checkBoxSaturday = findViewById(R.id.checkBoxSaturday)
        checkBoxSunday = findViewById(R.id.checkBoxSunday)

        // Lấy courseId từ Intent
        val courseId = intent.getIntExtra("courseId", 0)
        courseList = dbHelper.getAllCourses() // Lấy danh sách khóa học từ DatabaseHelper


        // Xử lý sự kiện khi nhấn vào EditText để chọn ngày
        classDateEditText.setOnClickListener {
            showDatePickerDialog()
        }

        // Cập nhật cờ khi người dùng nhập dữ liệu
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isDataEntered = true
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        classNameEditText.addTextChangedListener(textWatcher)
        teacherNameEditText.addTextChangedListener(textWatcher)
        classDateEditText.addTextChangedListener(textWatcher)
        feeEditText.addTextChangedListener(textWatcher)
        descriptionEditText.addTextChangedListener(textWatcher)

        // Xử lý sự kiện khi nhấn nút Lưu
        saveButton.setOnClickListener {
            handleSaveClass(courseId)
        }

        // Xử lý sự kiện khi nhấn nút Quay lại
        backButton.setOnClickListener {
            if (isDataEntered) {
                showConfirmationDialog()
            } else {
                finish() // Nếu không có dữ liệu đã nhập, đóng Activity
            }
        }

        // Đăng ký sự kiện cho các checkbox
        setCheckBoxListener(checkBoxMonday)
        setCheckBoxListener(checkBoxTuesday)
        setCheckBoxListener(checkBoxWednesday)
        setCheckBoxListener(checkBoxThursday)
        setCheckBoxListener(checkBoxFriday)
        setCheckBoxListener(checkBoxSaturday)
        setCheckBoxListener(checkBoxSunday)
    }

    private fun setCheckBoxListener(checkBox: CheckBox) {
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (selectedDaysCount >= 2) {
                    // Nếu đã chọn 2 checkbox, không cho phép chọn thêm
                    checkBox.isChecked = false
                    Toast.makeText(this, "Bạn chỉ có thể chọn tối đa 2 ngày", Toast.LENGTH_SHORT).show()
                } else {
                    // Tăng biến đếm
                    selectedDaysCount++
                    disableOtherCheckBoxes(checkBox, selectedDaysCount == 2) // Disable các checkbox khác nếu đã chọn 2
                }
            } else {
                // Giảm biến đếm khi bỏ chọn
                selectedDaysCount--
                if (selectedDaysCount < 2) {
                    disableOtherCheckBoxes(checkBox, false) // Bỏ disable các checkbox khác nếu đã bỏ chọn
                }
            }
        }
    }

    private fun disableOtherCheckBoxes(selectedCheckBox: CheckBox, disable: Boolean) {
        // Disable hoặc kích hoạt các checkbox khác
        val checkBoxes = listOf(checkBoxMonday, checkBoxTuesday, checkBoxWednesday, checkBoxThursday,
            checkBoxFriday, checkBoxSaturday, checkBoxSunday)

        for (checkBox in checkBoxes) {
            if (checkBox != selectedCheckBox) {
                checkBox.isEnabled = !disable // Kích hoạt nếu không disable
            }
        }
    }

    private fun handleSaveClass(courseId: Int) {
        val className = classNameEditText.text.toString().trim()
        val teacherName = teacherNameEditText.text.toString().trim()
        val classDate = classDateEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val feeString = feeEditText.text.toString().trim()
        val duration = durationEditText.text.toString().trim()
        val fee = feeString.toDoubleOrNull()

        // Kiểm tra các trường thông tin bắt buộc
        if (className.isEmpty() || teacherName.isEmpty() || classDate.isEmpty() || feeString.isEmpty() || duration.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra xem giá có hợp lệ không
        if (fee == null || fee <= 0) {
            Toast.makeText(this, "Giá không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra số lượng ngày đã chọn
        if (selectedDaysCount == 0) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ngày", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDays = mutableListOf<String>()
        if (checkBoxMonday.isChecked) selectedDays.add("Monday")
        if (checkBoxTuesday.isChecked) selectedDays.add("Tuesday")
        if (checkBoxWednesday.isChecked) selectedDays.add("Wednesday")
        if (checkBoxThursday.isChecked) selectedDays.add("Thursday")
        if (checkBoxFriday.isChecked) selectedDays.add("Friday")
        if (checkBoxSaturday.isChecked) selectedDays.add("Saturday")
        if (checkBoxSunday.isChecked) selectedDays.add("Sunday")

        val daysString = selectedDays.joinToString(",")

        val classId = UUID.randomUUID().toString()

        val courseFirestoreId = courseList.find { it.id == courseId }?.firestoreId

        val newClass = YogaClass(
            courseId = courseId,
            teacherName = teacherName,
            classDate = classDate,
            className = className,
            description = description,
            fee = fee,
            classDuration = duration,
            days = daysString,
            firestoreClassId = classId,
            courseFirestoreId = courseFirestoreId
        )

        val isInserted = dbHelper.addClass(newClass)

        if (isInserted) {
            if (isInternetAvailable(this)) {
                // Nếu có kết nối internet, tải dữ liệu lên Firestore
                firestore.collection("classes").document(classId).set(newClass)
                    .addOnSuccessListener {
                        dbHelper.updateClassFirestoreId(newClass.className, classId)
                        Toast.makeText(this, "Lớp học đã được thêm thành công", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi khi lưu vào Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Nếu không có internet, hiển thị thông báo và chỉ lưu vào SQLite
                Toast.makeText(this, "Không có kết nối internet. Lớp học đã được lưu cục bộ và sẽ tự động đẩy lên khi có mạng.", Toast.LENGTH_LONG).show()
            }

            val intent = Intent()
            intent.putExtra("classAdded", true)
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else {
            Toast.makeText(this, "Lỗi khi lưu vào cơ sở dữ liệu", Toast.LENGTH_SHORT).show()
        }
    }



    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)

                // Kiểm tra xem ngày đã chọn có nằm trong khoảng thời gian của khóa học không
                if (isDateInRange(selectedDate, startDate ?: "", endDate ?: "")) {
                    classDateEditText.setText(selectedDate)
                } else {
                    Toast.makeText(this, "Ngày đã chọn không nằm trong khoảng thời gian của khóa học", Toast.LENGTH_SHORT).show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Cấu hình giới hạn ngày hợp lệ
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val start = sdf.parse(startDate)
        val end = sdf.parse(endDate)

        // Thiết lập ngày tối thiểu và ngày tối đa cho DatePicker
        datePickerDialog.datePicker.minDate = start.time
        datePickerDialog.datePicker.maxDate = end.time

        datePickerDialog.show()
    }

    private fun isDateInRange(selectedDate: String, startDate: String, endDate: String): Boolean {
        // Chuyển đổi định dạng chuỗi thành ngày để so sánh
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val start = sdf.parse(startDate)
        val end = sdf.parse(endDate)
        val selected = sdf.parse(selectedDate)

        // Kiểm tra nếu ngày đã chọn nằm trong khoảng thời gian (bao gồm cả ngày bắt đầu và ngày kết thúc)
        return (selected.equals(start) || selected.equals(end) || (selected.after(start) && selected.before(end)))
    }



    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Bạn có muốn thoát mà không lưu thay đổi không?")
            .setPositiveButton("Có") { _, _ -> finish() }
            .setNegativeButton("Không", null)
        builder.create().show()
    }


    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

