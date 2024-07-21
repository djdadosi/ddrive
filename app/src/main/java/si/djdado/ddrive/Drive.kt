package si.djdado.ddrive

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Drive(
    @DocumentId
    val id: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
)
