import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Desk(val id: Int, val name: String, var isBooked: Boolean)

@Composable
fun DeskBookingApp(initialDesks: List<Desk>) {
    var deskList by remember { mutableStateOf(initialDesks) }
    MaterialTheme {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Desk Booking System", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(deskList) { desk ->
                    DeskItem(desk = desk, onBook = {
                        bookDesk(deskList, desk.id) { updatedDesks ->
                            deskList = updatedDesks
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun DeskItem(desk: Desk, onBook: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = desk.name, style = MaterialTheme.typography.h6)
            Text(
                text = if (desk.isBooked) "Booked" else "Available",
                style = MaterialTheme.typography.body2,
                color = if (desk.isBooked) MaterialTheme.colors.error else MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onBook, enabled = !desk.isBooked) {
                Text(text = if (desk.isBooked) "Booked" else "Book")
            }
        }
    }
}

fun bookDesk(desks: List<Desk>, deskId: Int, updateDesks: (List<Desk>) -> Unit) {
    val updatedDesks = desks.map { desk ->
        if (desk.id == deskId) {
            desk.copy(isBooked = true)
        } else {
            desk
        }
    }
    updateDesks(updatedDesks)
}
