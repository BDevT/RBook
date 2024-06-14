import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

data class Desk(val id: Int, val name: String, var isBooked: Boolean)

@Composable
fun DeskBookingApp(initialDesks: List<Desk>) {
    var deskList by remember { mutableStateOf(initialDesks) }
    var showDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Desk Booking System") },
                    actions = {
                        Button(onClick = { showDialog = true }) {
                            Text("Sign In")
                        }
                    }
                )
            },
            content = { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                if (showDialog) {
                    SignInDialog(onDismiss = { showDialog = false })
                }
            }
        )
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

@Composable
fun SignInDialog(onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign In") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { /* Handle login button click */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Log in")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
