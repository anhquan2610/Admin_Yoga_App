import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.yogaadmin.R
import com.example.yogaadmin.data.Course

class CourseListAdapter(
    private var courseList: List<Course>,
    private val onDeleteClick: (Course) -> Unit,
    private val onUpdateClick: (Course) -> Unit,
    private val onViewDetailsClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseListAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewCourseName: TextView = itemView.findViewById(R.id.textViewCourseName)
        private val textViewCourseType: TextView = itemView.findViewById(R.id.textViewCourseType)
        private val textViewCapacity: TextView = itemView.findViewById(R.id.textViewCapacity)
        private val textViewDay: TextView = itemView.findViewById(R.id.textViewDay)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewTime) // Hiển thị thời gian
        private val textViewPrice: TextView = itemView.findViewById(R.id.textViewPrice) // Hiển thị giá
        private val textViewDuration: TextView = itemView.findViewById(R.id.textViewDuration)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
        private val buttonUpdate: ImageButton = itemView.findViewById(R.id.buttonUpdate)
        private val buttonViewDetails: ImageButton = itemView.findViewById(R.id.buttonViewDetails)

        fun bind(course: Course) {
            textViewCourseName.text = course.name
            textViewCourseType.text = "Loại khoá học: ${course.courseType}"
            textViewCapacity.text = "Số lượng: ${course.capacity}"
            textViewDay.text = "Ngày: ${course.dayOfWeek}"
            textViewTime.text = "Thời gian: ${course.time}" // Hiển thị thời gian
            textViewPrice.text = "Giá: ${course.price}" // Hiển thị giá
            textViewDuration.text = "Thời lượng: ${course.duration}" // Hiển thị thời lượng

            // Xử lý sự kiện xóa với thông báo xác nhận
            buttonDelete.setOnClickListener {
                onDeleteClick(course) // Gọi hàm onDeleteClick trực tiếp
            }

            // Xử lý sự kiện cập nhật
            buttonUpdate.setOnClickListener {
                onUpdateClick(course)
            }

            //Xử lý sự kiện xem chi tiết
            buttonViewDetails.setOnClickListener {
                onViewDetailsClick(course)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courseList[position]
        holder.bind(course)
    }

    override fun getItemCount(): Int = courseList.size

    fun updateCourses(newCourses: List<Course>) {
        courseList = newCourses
        notifyDataSetChanged()
    }

}
