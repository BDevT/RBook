import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        val sampleDesks = listOf(
            Desk(id = 1, name = "Desk 1", isBooked = false),
            Desk(id = 2, name = "Desk 2", isBooked = false),
            Desk(id = 3, name = "Desk 3", isBooked = true),
            Desk(id = 4, name = "Desk 4", isBooked = false),
            Desk(id = 5, name = "Desk 5", isBooked = true)
        )
        App(initialDesks = sampleDesks)
    }
}