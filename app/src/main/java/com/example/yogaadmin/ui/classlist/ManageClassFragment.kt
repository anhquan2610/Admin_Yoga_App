package com.example.yogaadmin.ui.classlist

import ClassListAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yogaadmin.R
import com.example.yogaadmin.data.DatabaseHelper
import com.example.yogaadmin.data.YogaClass
import com.example.yogaadmin.ui.classform.ClassFormActivity
import com.example.yogaadmin.ui.update.UpdateClassFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.log

class ManageClassFragment : Fragment() {

    private lateinit var classRecyclerView: RecyclerView
    private lateinit var addButton: FloatingActionButton
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var classAdapter: ClassListAdapter
    private lateinit var editTextSearch: EditText
    private lateinit var backButton: ImageView

    private var courseId: Int = 0
    private var dayOfWeek: String? = null



    // Khai báo ActivityResultLauncher
    private lateinit var addClassLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val REQUEST_CODE = 1001

        fun newInstance(courseId: Int, startDate: String, endDate: String,dayOfWeek: String): ManageClassFragment {
            val fragment = ManageClassFragment()
            val args = Bundle()
            args.putInt("courseId", courseId)
            args.putString("startDate", startDate)
            args.putString("endDate", endDate)
            args.putString("dayOfWeek", dayOfWeek)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_class, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        dbHelper.logAllClassIds()

        // Nhận courseId, startDate và endDate từ arguments
        courseId = arguments?.getInt("courseId") ?: 0
        dayOfWeek = arguments?.getString("dayOfWeek")
        Log.d("ClassFormActivity", "Received dayOfWeek: $dayOfWeek")



        // Ánh xạ RecyclerView, FloatingActionButton và EditText tìm kiếm
        classRecyclerView = view.findViewById(R.id.classRecyclerView)
        addButton = view.findViewById(R.id.fabAddClass)
        editTextSearch = view.findViewById(R.id.editTextSearch)
        backButton = view.findViewById(R.id.buttonBack)

        dbHelper = DatabaseHelper(requireContext())

        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Khởi tạo ActivityResultLauncher
        addClassLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                refreshClassList()
            }
        }

        // Lấy danh sách lớp học từ DatabaseHelper
        val classList = dbHelper.getClassesByCourse(courseId)
        classAdapter = ClassListAdapter(
            classList = classList,
            onDeleteClick = { yogaClass -> confirmDeleteClass(yogaClass)
            },
            onUpdateClick = { yogaClass ->
                val firestoreClassId = yogaClass.firestoreClassId
                val courseFirestoreId = yogaClass.courseFirestoreId

                if (firestoreClassId != null) {
                    val updateClassFragment = UpdateClassFragment.newInstance(
                        classId = yogaClass.classId,
                        courseId = courseId,
                        dayOfWeek = dayOfWeek,
                        firestoreClassId = firestoreClassId,
                        courseFirestoreId = courseFirestoreId
                        // Chỉ gọi nếu không null
                    )
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, updateClassFragment)
                        .addToBackStack(null)
                        .commit()
                } else {
                    Toast.makeText(requireContext(), "ID Firestore không hợp lệ.", Toast.LENGTH_SHORT).show()
                }
            }

        )


        // Thiết lập RecyclerView
        classRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        classRecyclerView.adapter = classAdapter

        // Xử lý sự kiện thêm lớp học
        addButton.setOnClickListener {
            Log.d("ManageClassFragment", "courseId: $courseId")
            Log.d("ManageClassFragment", "dayOfWeek: $dayOfWeek")
            val intent = Intent(requireContext(), ClassFormActivity::class.java).apply {
                putExtra("courseId", courseId)
                putExtra("dayOfWeek", dayOfWeek)
            }
            addClassLauncher.launch(intent)
        }



        // Tạo một đối tượng TextWatcher
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Không làm gì
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Gọi phương thức lọc khi người dùng nhập
                filterClasses(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // Không làm gì
            }
        })
    }

    private fun confirmDeleteClass(yogaClass: YogaClass) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Xác nhận xóa")
            setMessage("Bạn có chắc chắn muốn xóa lớp học: ${yogaClass.className}?")
            setPositiveButton("Có") { _, _ ->
                deleteClass(yogaClass.classId, yogaClass.firestoreClassId) // Thêm tham số firestoreId
            }
            setNegativeButton("Không", null)
            show()
        }
    }

    private fun deleteClass(classId: Int, firestoreClassId: String?) {
        val db = dbHelper.writableDatabase
        if (!db.isOpen) {
            Toast.makeText(requireContext(), "Cơ sở dữ liệu không mở.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Nếu firestoreId không null, xóa lớp học từ Firestore trước
            if (firestoreClassId != null) {
                deleteClassFromFirestore(firestoreClassId) { success ->
                    if (success) {
                        // Nếu xóa thành công từ Firestore, xóa lớp học trong SQLite
                        if (dbHelper.deleteClass(classId)) {
                            Toast.makeText(requireContext(), "Lớp học đã được xóa!", Toast.LENGTH_SHORT).show()
                            refreshClassList() // Cập nhật danh sách lớp học
                        } else {
                            Toast.makeText(requireContext(), "Có lỗi xảy ra khi xóa lớp học từ SQLite.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Có lỗi xảy ra khi xóa lớp học từ Firestore.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "ID Firestore không hợp lệ.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Có lỗi xảy ra: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteClassFromFirestore(firestoreClassId: String, callback: (Boolean) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("classes").document(firestoreClassId)
            .delete()
            .addOnSuccessListener {
                Log.d("FirestoreDelete", "Lớp học đã được xóa khỏi Firestore.")
                callback(true) // Trả về true nếu xóa thành công
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDelete", "Lỗi khi xóa lớp học từ Firestore: ${e.message}")
                callback(false) // Trả về false nếu có lỗi xảy ra
            }
    }

    private fun filterClasses(query: String) {
        val filteredList = dbHelper.getClassesByCourse(courseId).filter { yogaClass ->
            yogaClass.className.contains(query, ignoreCase = true) ||
                    yogaClass.teacherName.contains(query, ignoreCase = true)
        }
        classAdapter.updateClasses(filteredList)
    }


    private fun refreshClassList() {
        val updatedClassList = dbHelper.getClassesByCourse(courseId)
        classAdapter.updateClasses(updatedClassList)
    }
}
