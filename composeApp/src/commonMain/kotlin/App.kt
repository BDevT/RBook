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
import io.ktor.http.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import epicarchitect.calendar.compose.datepicker.EpicDatePicker
import epicarchitect.calendar.compose.datepicker.state.EpicDatePickerState
import epicarchitect.calendar.compose.datepicker.state.rememberEpicDatePickerState
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

    var calendarVisible by remember { mutableStateOf(false) }
    val datePickerState = rememberEpicDatePickerState(
        selectionMode = EpicDatePickerState.SelectionMode.Single(),
        selectedDates = listOf(LocalDate(2024, 6, 18))
    )

    LaunchedEffect(datePickerState.selectedDates) {
        deskList = fetchDesks()
        bookings = fetchBookings(datePickerState.selectedDates.firstOrNull() ?: LocalDate(2024, 6, 18))
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Desk Booking System") },
                    actions = {
                        datePickerToggle(
                            selectedDates = datePickerState.selectedDates,
                            onDateClick = { calendarVisible = !calendarVisible }
                        )
                    }
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
                                    val selectedDate = datePickerState.selectedDates.firstOrNull() ?: LocalDate(2024, 6, 18)
                                    bookDesk(desk.id, selectedDate, userName)
                                    deskList = fetchDesks()
                                    bookings = fetchBookings(selectedDate)
                                }
                            },
                            onCancel = {
                                CoroutineScope(Dispatchers.Default).launch {
                                    val selectedDate = datePickerState.selectedDates.firstOrNull() ?: LocalDate(2024, 6, 18)
                                    cancelBooking(desk.id, selectedDate)
                                    deskList = fetchDesks()
                                    bookings = fetchBookings(selectedDate)
                                }
                            },
                            selectedDate = datePickerState.selectedDates.firstOrNull() ?: LocalDate(2024, 6, 18),
                            modifier = Modifier
                                .offset(
                                    x = offsetX.dp,
                                    y = offsetY.dp
                                ),
                            bookedBy = booking?.booked_by
                        )
                    }

                    if (calendarVisible) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Card(
                                modifier = Modifier.size(400.dp),
                                elevation = 8.dp
                            ) {
                                Column {
                                    EpicDatePicker(
                                        state = datePickerState,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                    Button(
                                        onClick = { calendarVisible = false },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text("Confirm")
                                    }
                                }
                            }
                        }
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
    val client = HttpClient()
    return try {
        val response: HttpResponse = client.get("https://rbook-route-scp012-dxm01.apps.ocp.osprey.hartree.stfc.ac.uk/desks/")
        val responseBody: String = response.bodyAsText()
        Json.decodeFromString(responseBody)
    } catch (e: Exception) {
        emptyList()
    } finally {
        client.close()
    }
}

suspend fun fetchBookings(selectedDate: LocalDate): List<Booking> {
    val client = HttpClient()
    return try {
        val formattedDate = selectedDate.toString()
        val response: HttpResponse = client.get("https://rbook-route-scp012-dxm01.apps.ocp.osprey.hartree.stfc.ac.uk/bookings?booking_date=$formattedDate")
        val responseBody: String = response.bodyAsText()
        Json.decodeFromString(responseBody)
    } catch (e: Exception) {
        emptyList()
    } finally {
        client.close()
    }
}

suspend fun bookDesk(deskId: Int, selectedDate: LocalDate, userName: String) {
    val client = HttpClient()
    try {
        val formattedDate = selectedDate.toString()
        val bookingRequest = BookingRequest(userName, formattedDate)
        val bookingStr: String = bookingRequest.toJsonString()
        val response = client.post("https://rbook-route-scp012-dxm01.apps.ocp.osprey.hartree.stfc.ac.uk/desks/$deskId/book") {
            contentType(ContentType.Application.Json)
            setBody(bookingStr)
        }
        println("Booking response: $response")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        client.close()
    }
}

suspend fun cancelBooking(deskId: Int, selectedDate: LocalDate) {
    val client = HttpClient()
    try {
        val formattedDate = selectedDate.toString()
        val response = client.post("https://rbook-route-scp012-dxm01.apps.ocp.osprey.hartree.stfc.ac.uk/desks/$deskId/cancel") {
            contentType(ContentType.Application.Json)
            parameter("booking_date", formattedDate)
        }
        println("Cancellation response: $response")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        client.close()
    }
}
