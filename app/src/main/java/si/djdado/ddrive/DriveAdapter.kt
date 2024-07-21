package si.djdado.ddrive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DriveAdapter (options: FirestoreRecyclerOptions<Drive>, private val onClickObject: DriveOnClick) : FirestoreRecyclerAdapter<Drive, DriveAdapter.ViewHolder>(options) {
    inner class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val cvLine: LinearLayout = itemView.findViewById(R.id.linearLayoutDrive)
        val textViewDriveName: TextView = itemView.findViewById(R.id.textViewDriveName)
        val textViewDriveDate: TextView = itemView.findViewById(R.id.textViewDriveDate)
    }

    interface DriveOnClick {
        fun onClick(id: String)
        fun onLongClick(id: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_drive, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Drive) {
        //val item = values[position]

        val id = model.id
        val createdAt = model.createdAt.toString()

        holder.textViewDriveName.text = id
        holder.textViewDriveDate.text = createdAt


        holder.cvLine.setOnClickListener {
            onClickObject.onClick(id)
        }

        holder.cvLine.setOnLongClickListener {
            onClickObject.onLongClick(id)
            true
        }
    }
}