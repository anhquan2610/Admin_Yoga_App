import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yogaadmin.R
import com.example.yogaadmin.data.YogaClass
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class ClassListAdapter(
    private var classList: List<YogaClass>,
    private val onDeleteClick: (YogaClass) -> Unit,
    private val onUpdateClick: (YogaClass) -> Unit
) : RecyclerView.Adapter<ClassListAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val classNameTextView: TextView = itemView.findViewById(R.id.textViewClassName)
        private val textViewTeacherName: TextView = itemView.findViewById(R.id.textViewTeacherName)
        private val textViewClassDate: TextView = itemView.findViewById(R.id.textViewClassDate)
        private val textViewFee: TextView = itemView.findViewById(R.id.textViewFee)
        private val textViewDays: TextView = itemView.findViewById(R.id.textViewDays)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        private val durationTextView: TextView = itemView.findViewById(R.id.textDuration)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDeleteClass)
        private val buttonUpdate: ImageButton = itemView.findViewById(R.id.buttonUpdateClass)

        fun bind(yogaClass: YogaClass) {
            classNameTextView.text = "Class Name: ${yogaClass.className}"
            textViewTeacherName.text = "Teacher: ${yogaClass.teacherName}"
            textViewClassDate.text = "Start Date: ${formatClassDate(yogaClass.classDate)}"
            durationTextView.text = "Time: ${yogaClass.classDuration} minutes"
            textViewFee.text = "Class Fee: ${formatFee(yogaClass.fee)}$"  // Cập nhật cách hiển thị phí
            textViewDays.text = "Days: ${yogaClass.days}/weekly"
            descriptionTextView.text = "Description: ${yogaClass.description}"

            buttonDelete.setOnClickListener {
                onDeleteClick(yogaClass)
            }

            buttonUpdate.setOnClickListener {
                onUpdateClick(yogaClass)
            }
        }

        private fun formatClassDate(classDate: String): String {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date: Date? = try {
                dateFormat.parse(classDate)
            } catch (e: Exception) {
                null
            }

            return if (date != null) {
                val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                val dayOfWeek = dayOfWeekFormat.format(date)
                "$dayOfWeek, ${dateFormat.format(date)}"
            } else {
                "Ngày không hợp lệ"
            }
        }

        // Hàm định dạng phí
        private fun formatFee(fee: Double): String {
            val decimalFormat = DecimalFormat("#,##0.00")  // Định dạng với hai chữ số thập phân
            return decimalFormat.format(fee)
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(classList[position])
    }

    override fun getItemCount(): Int = classList.size

    fun updateClasses(newClassList: List<YogaClass>) {
        classList = newClassList
        notifyDataSetChanged() // Cập nhật giao diện
    }
}
