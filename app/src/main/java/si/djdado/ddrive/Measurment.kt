package si.djdado.ddrive

data class Measurment(
    val id: String,

    val startLat: Double,
    val endLat: Double,

    val startLng: Double,
    val endLng: Double,

    val rating: Double = 10.0
)
