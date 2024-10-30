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
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView
    private lateinit var courseList: List<Course>




    private var dayOfWeek: String? = null
    private var isDataEntered = false // Cờ theo dõi dữ liệu nhập vào
    private var selectedDaysCount = 0 // Biến đếm số checkbox đã chọn



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_form)

        dbHelper = DatabaseHelper(this)
        firestore = FirebaseFirestore.getInstance() // Khởi tạo Firestore

        // Khai báo các EditText và Button
        classNameEditText = findViewById(R.id.editTextClassName)
        teacherNameEditText = findViewById(R.id.editTextTeacherName)
        classDateEditText = findViewById(R.id.editTextClassDate)
        descriptionEditText = findViewById(R.id.editTextDescription)
        saveButton = findViewById(R.id.buttonSave)
        backButton = findViewById(R.id.buttonBack)




        // Lấy courseId từ Intent
        val courseId = intent.getIntExtra("courseId", 0)
        courseList = dbHelper.getAllCourses() // Lấy danh sách khóa học từ DatabaseHelper
        dayOfWeek = intent.getStringExtra("dayOfWeek")




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

    }




    private fun handleSaveClass(courseId: Int) {
        val className = classNameEditText.text.toString().trim()
        val teacherName = teacherNameEditText.text.toString().trim()
        val classDate = classDateEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()

        // Kiểm tra các trường thông tin bắt buộc
        if (className.isEmpty() || teacherName.isEmpty() || classDate.isEmpty()  ) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }


        val classId = UUID.randomUUID().toString()

        val courseFirestoreId = courseList.find { it.id == courseId }?.firestoreId

        val newClass = YogaClass(
            courseId = courseId,
            teacherName = teacherName,
            classDate = classDate,
            className = className,
            description = description,
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

                // Kiểm tra xem ngày đã chọn có thứ trùng với dayOfWeek không
                if (isDayOfWeekMatch(selectedDate, dayOfWeek)) {
                    classDateEditText.setText(selectedDate)
                } else {
                    Toast.makeText(this, "Ngày đã chọn không hợp lệ. Vui lòng chọn ngày có thứ phù hợp.", Toast.LENGTH_SHORT).show()
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

