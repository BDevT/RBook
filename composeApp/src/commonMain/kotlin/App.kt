import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.ui.unit.IntSize
import kotlinx.datetime.LocalDate

@Serializable
data class Desk(
    val id: Int,
    val name: String,
    val physical_location: String,
    val virtual_location_x: Float,
    val virtual_location_y: Float,
    val length: Float,
    val width: Float
)

@Serializable
data class Booking(
    val id: Int,
    val desk_id: Int,
    val booked_by: String,
    val booked_date: String
)

@Serializable
data class BookingRequest(val user: String, val booking_date: String) {
    fun toJsonString(): String {
        return """{"user":"$user","booking_date":"$booking_date"}"""
    }
}

@Composable
fun App() {
    var deskList by remember { mutableStateOf(emptyList<Desk>()) }
    var bookings by remember { mutableStateOf(emptyList<Booking>()) }
    val containerSize = IntSize(800, 800) // Fixed size of 800 x 800

    LaunchedEffect(Unit) {
        deskList = fetchDesks()
        bookings = fetchBookings(LocalDate(2024,6,18))
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Desk Booking System") },
                )
            },
            content = { padding ->
                Box(
                    modifier = Modifier
                        .size(800.dp, 800.dp) // Set the size to 800 x 800 dp
                        .padding(0.dp)
                ) {
                    deskList.forEach { desk ->
                        val booking = bookings.find { it.desk_id == desk.id }
                        val isBooked = booking != null
                        val offsetX = (containerSize.width * desk.virtual_location_x) - (desk.width / 2)
                        val offsetY = containerSize.height * (1 - desk.virtual_location_y) - (desk.length / 2)
                        deskItem(
                            desk = desk,
                            isBooked = isBooked,
                            onBook = { userName ->
                                CoroutineScope(Dispatchers.Default).launch {
                                    val selectedDate = LocalDate(2024, 6, 18)
                                    bookDesk(desk.id, selectedDate, userName)
                                    deskList = fetchDesks()
                                    bookings = fetchBookings(selectedDate)
                                }
                            },
                            onCancel = {
                                CoroutineScope(Dispatchers.Default).launch {
                                    val selectedDate = LocalDate(2024, 6, 18)
                                    cancelBooking(desk.id, selectedDate)
                                    deskList = fetchDesks()
                                    bookings = fetchBookings(selectedDate)
                                }
                            },
                            selectedDate = LocalDate(2024, 6, 18),
                            modifier = Modifier
                                .offset(
                                    x = offsetX.dp,
                                    y = offsetY.dp
                                ),
                            bookedBy = booking?.booked_by
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun datePickerToggle(
    selectedDates: List<LocalDate>,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onDateClick,
        modifier = modifier
    ) {
        Text(
            text = selectedDates.firstOrNull()?.let {
                it.toString()
            } ?: "Select Date"
        )
    }
}

@Composable
fun deskItem(desk: Desk, isBooked: Boolean, onBook: suspend (String) -> Unit, onCancel: suspend () -> Unit, selectedDate: LocalDate, modifier: Modifier = Modifier, bookedBy: String? = null) {
    val backgroundColor = if (isBooked) Color(0xFFFFCDD2) else Color(0xFFC8E6C9) // Light red for booked, light green for available
    var showBookingDialog by remember { mutableStateOf(false) }
    var showCancellationDialog by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .size(desk.width.dp, desk.length.dp)
            .padding(0.dp)
            .background(backgroundColor)
            .clickable {
                if (isBooked) {
                    showCancellationDialog = true
                } else {
                    showBookingDialog = true
                }
            },
        elevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Text(
                text = desk.name,
                style = MaterialTheme.typography.h6,
                color = Color.Black
            )
        }
    }

    if (showBookingDialog) {
        AlertDialog(
            onDismissRequest = { showBookingDialog = false },
            title = { Text("Book Desk") },
            text = {
                Column {
                    Text("Desk: ${desk.name}")
                    Text("Date: $selectedDate")
                    TextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Your Name") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showBookingDialog = false
                    CoroutineScope(Dispatchers.Default).launch {
                        onBook(userName)
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = { showBookingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCancellationDialog) {
        AlertDialog(
            onDismissRequest = { showCancellationDialog = false },
            title = { Text("Cancel Booking") },
            text = {
                Column {
                    Text("Desk: ${desk.name}")
                    Text("Date: $selectedDate")
                    Text("Booked by: ${bookedBy ?: "Unknown"}")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showCancellationDialog = false
                    CoroutineScope(Dispatchers.Default).launch {
                        onCancel()
                    }
                }) {
                    Text("Cancel Booking")
                }
            },
            dismissButton = {
                Button(onClick = { showCancellationDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

suspend fun fetchDesks(): List<Desk> {
    return listOf(
        Desk(1, "S1", "Room A", 0.2f, 0.4f, 50.0f, 100.0f),
        Desk(2, "S2", "Room A", 0.2f, 0.5f, 50.0f, 100.0f),
        Desk(3, "S3", "Room B", 0.33f, 0.43f, 100.0f, 50.0f),
        Desk(4, "S4", "Room B", 0.43f, 0.43f, 100.0f, 50.0f),
        Desk(5, "S5", "Room B", 0.36f, 0.55f, 50.0f, 100.0f),
        Desk(6, "S6", "Room C", 0.7f, 0.3f, 50.0f, 100.0f),
        Desk(7, "S7", "Room C", 0.85f, 0.3f, 50.0f, 100.0f),
        Desk(8, "S8", "Room C", 0.7f, 0.38f, 50.0f, 100.0f),
        Desk(9, "S9", "Room C", 0.85f, 0.38f, 50.0f, 100.0f),
        Desk(10, "S10", "Room D", 0.67f, 0.53f, 100.0f, 50.0f),
        Desk(11, "S11", "Room D", 0.77f, 0.53f, 100.0f, 50.0f),
        Desk(12, "S12", "Room D", 0.87f, 0.53f, 100.0f, 50.0f),
        Desk(13, "S13", "Room D", 0.67f, 0.71f, 100.0f, 50.0f),
        Desk(14, "S14", "Room D", 0.77f, 0.71f, 100.0f, 50.0f),
        Desk(15, "S15", "Room D", 0.87f, 0.71f, 100.0f, 50.0f),
        Desk(16, "S16", "Room D", 0.67f, 0.89f, 100.0f, 50.0f),
        Desk(17, "S17", "Room D", 0.77f, 0.89f, 100.0f, 50.0f),
        Desk(18, "S18", "Room D", 0.87f, 0.89f, 100.0f, 50.0f)
    )
}


suspend fun fetchBookings(selectedDate: LocalDate): List<Booking> {
    return emptyList() // No bookings returned
}

suspend fun bookDesk(deskId: Int, selectedDate: LocalDate, userName: String) {

}

suspend fun cancelBooking(deskId: Int, selectedDate: LocalDate) {

}
