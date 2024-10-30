package com.example.yogaadmin.ui.courselist

import CourseListAdapter
import android.app.AlertDialog
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yogaadmin.R
import com.example.yogaadmin.data.Course
import com.example.yogaadmin.data.DatabaseHelper
import com.example.yogaadmin.ui.addcourse.AddCourseFragment
import com.example.yogaadmin.ui.classlist.ManageClassFragment
import com.example.yogaadmin.ui.updatecourse.UpdateCourseFragment
import com.google.firebase.firestore.FirebaseFirestore

class CourseListFragment : Fragment() {

    private lateinit var adapter: CourseListAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var editTextSearch: EditText
    private lateinit var spinnerFilter: Spinner
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        dbHelper.logAllCourseIds()
        firestore = FirebaseFirestore.getInstance()

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewCourses)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        editTextSearch = view.findViewById(R.id.editTextSearch)
        spinnerFilter = view.findViewById(R.id.spinnerFilter)

        adapter = CourseListAdapter(
            dbHelper.getAllCourses(),
            onDeleteClick = { course -> confirmDeleteCourse(course) },
            onUpdateClick = { course ->
                val updateCourseFragment = UpdateCourseFragment()
                val args = Bundle().apply { putInt("courseId", course.id) }
                updateCourseFragment.arguments = args
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, updateCourseFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onViewDetailsClick = { course ->
                val manageClassFragment = ManageClassFragment()
                val args = Bundle().apply { putInt("courseId", course.id) }
                manageClassFragment.arguments = args
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, manageClassFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )

        recyclerView.adapter = adapter
        setupSpinner()

        // Listener cho tìm kiếm
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCourses(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Listener cho lọc loại khóa học
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterCourses(editTextSearch.text.toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Listener cho nút "Thêm"
        val buttonAddCourse: View = view.findViewById(R.id.buttonAddCourse)
        buttonAddCourse.setOnClickListener {
            val addCourseFragment = AddCourseFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, addCourseFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun confirmDeleteCourse(course: Course) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Xác nhận xóa")
            setMessage("Bạn có chắc chắn muốn xóa khóa học: ${course.name}?")
            setPositiveButton("Có") { _, _ ->
                deleteCourseWithClasses(course.id) // Gọi phương thức xóa khóa học cùng các lớp liên quan
            }
            setNegativeButton("Không", null)
            show()
        }
    }

    private fun deleteCourseWithClasses(courseId: Int) {
        val db = dbHelper.writableDatabase
        if (!db.isOpen) {
            Toast.makeText(requireContext(), "Cơ sở dữ liệu không mở.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Lấy course và firestoreId từ SQLite
            val course = dbHelper.getCourseById(courseId)
            val firestoreId = course?.firestoreId

            if (firestoreId != null) {
                // Bắt đầu giao dịch để xóa khóa học và lớp học liên quan
                val courseClasses = dbHelper.getClassesByCourse(courseId)

                // Xóa tất cả các lớp học liên quan từ SQLite và Firestore
                for (classItem in courseClasses) {
                    dbHelper.deleteClass(classItem.classId) // Xóa lớp học trong SQLite
                    classItem.firestoreClassId?.let { // Kiểm tra null
                        deleteClassFromFirestore(it) // Gọi hàm nếu không null
                    }
                }

                // Xóa khóa học từ Firestore trước kia
                deleteCourseFromFirestore(firestoreId) { success ->
                    if (success) {
                        // Nếu xóa thành công từ Firestore, xóa khóa học trong SQLite
                        if (dbHelper.deleteCourse(courseId)) {
                            Toast.makeText(requireContext(), "Khóa học và các lớp liên quan đã được xóa!", Toast.LENGTH_SHORT).show()
                            updateCourseList() // Cập nhật danh sách khóa học
                        } else {
                            Toast.makeText(requireContext(), "Có lỗi xảy ra khi xóa khóa học từ SQLite.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Có lỗi xảy ra khi xóa khóa học từ Firestore.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "ID Firestore không hợp lệ.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SQLiteDatabaseLockedException) {
            Toast.makeText(requireContext(), "Cơ sở dữ liệu đang bị khóa. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Có lỗi xảy ra: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun deleteCourseFromFirestore(firestoreId: String, callback: (Boolean) -> Unit) {
        firestore.collection("courses").document(firestoreId)
            .delete()
            .addOnSuccessListener {
                Log.d("FirestoreDelete", "Khóa học đã được xóa khỏi Firestore.")
                callback(true) // Trả về true nếu xóa thành công
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDelete", "Lỗi khi xóa khóa học từ Firestore: ${e.message}")
                callback(false) // Trả về false nếu có lỗi xảy ra
            }
    }


    private fun deleteClassFromFirestore(firestoreClassId: String) {
        firestore.collection("classes").document(firestoreClassId)
            .delete()
            .addOnSuccessListener {
                Log.d("FirestoreDelete", "Lớp học đã được xóa khỏi Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDelete", "Lỗi khi xóa lớp học từ Firestore: ${e.message}")
            }
    }






    private fun updateCourseList() {
        val updatedCourses = dbHelper.getAllCourses()
        adapter.updateCourses(updatedCourses)
    }

    private fun setupSpinner() {
        val courseTypes = arrayOf("Tất cả", "Flow Yoga", "Aerial Yoga", "Family Yoga") // Thay đổi theo loại khóa học của bạn
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courseTypes)
        spinnerFilter.adapter = adapter
    }

    private fun filterCourses(query: String) {
        val filteredCourses = dbHelper.getAllCourses().filter { course ->
            course.name.contains(query, ignoreCase = true) &&
                    (spinnerFilter.selectedItem.toString() == "Tất cả" || course.courseType == spinnerFilter.selectedItem.toString())
        }
        adapter.updateCourses(filteredCourses)
    }
}

