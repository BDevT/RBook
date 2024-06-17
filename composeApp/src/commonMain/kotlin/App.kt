import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.unit.IntSize

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
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        deskList = fetchDesks()
        bookings = fetchBookings()
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
                        .fillMaxSize()
                        .padding(padding)
                        .onGloballyPositioned { coordinates ->
                            containerSize = coordinates.size
                        }
                ) {
                    deskList.forEach { desk ->
                        val isBooked = bookings.any { it.desk_id == desk.id }
                        val offsetX = containerSize.width * desk.virtual_location_x
                        val offsetY = containerSize.height * (1 - desk.virtual_location_y)
                        deskItem(
                            desk = desk,
                            isBooked = isBooked,
                            onBook = {
                                CoroutineScope(Dispatchers.Default).launch {
                                    bookDesk(desk.id)
                                    deskList = fetchDesks()
                                    bookings = fetchBookings()
                                }
                            },
                            modifier = Modifier
                                .offset(
                                    x = offsetX.dp,
                                    y = offsetY.dp
                                )
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun deskItem(desk: Desk, isBooked: Boolean, onBook: suspend () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .size(desk.length.dp * 100, desk.width.dp * 100)
            .padding(0.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = desk.name, style = MaterialTheme.typography.h6)
            Text(
                text = if (isBooked) "Booked" else "Available",
                style = MaterialTheme.typography.body2,
                color = if (isBooked) MaterialTheme.colors.error else MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { CoroutineScope(Dispatchers.Default).launch { onBook() } }, enabled = !isBooked) {
                Text(text = if (isBooked) "Booked" else "Book")
            }
        }
    }
}

suspend fun fetchDesks(): List<Desk> {
    val client = HttpClient()

    return try {
        val response: HttpResponse = client.get("http://localhost:8000/desks")
        val responseBody: String = response.bodyAsText()
        Json.decodeFromString(responseBody)
    } catch (e: Exception) {
        emptyList()
    } finally {
        client.close()
    }
}

suspend fun fetchBookings(): List<Booking> {
    val client = HttpClient()

    return try {
        val response: HttpResponse = client.get("http://localhost:8000/bookings?booking_date=${getCurrentDate()}")
        val responseBody: String = response.bodyAsText()
        Json.decodeFromString(responseBody)
    } catch (e: Exception) {
        emptyList()
    } finally {
        client.close()
    }
}

fun getCurrentDate(): String {
    return LocalDate(2024, 6, 16).toString()
}

suspend fun bookDesk(deskId: Int) {
    val client = HttpClient() {
        defaultRequest {
            url("http://localhost:8000")
        }
        install(ContentNegotiation) {
            Json { ignoreUnknownKeys = true }
        }
    }

    try {
        val bookingRequest = BookingRequest("default_user", getCurrentDate())
        val bookingStr: String = bookingRequest.toJsonString()
        println(bookingRequest.booking_date)
        val response = client.post("/desks/$deskId/book") {
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