package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType

@Composable
fun HomeScreen(vm: GameViewModel, navController: NavController) {
    val highscore by vm.highscore.collectAsState()
    val nBack by vm.nBack.collectAsState()
    val eventInterval by vm.eventInterval.collectAsState()
    val correctNumbers by vm.correctNumbers.collectAsState()
    val gameState by vm.gameState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(20.dp),
                text = "High-Score: \n$highscore",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.padding(20.dp),
                text = "Value of N: \n$nBack",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.padding(20.dp),
                text = "Interval: \n$eventInterval",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.padding(20.dp),
                text = "Correct numbers: \n$correctNumbers",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Text(
                modifier = Modifier.padding(16.dp),
                text = "Start Game".uppercase(),
                style = MaterialTheme.typography.displaySmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button for Audio Game
                Button(onClick = {
                    vm.setGameType(GameType.Audio)
                    vm.startGame()
                    navController.navigate("game")
                    }) {
                    Icon(
                        painter = painterResource(id = R.drawable.sound_on),
                        contentDescription = "Sound",
                        modifier = Modifier
                            .height(48.dp)
                            .aspectRatio(3f / 2f)
                    )
                }

                // Button for Visual Game
                Button(onClick = {
                    vm.setGameType(GameType.Visual)
                    vm.startGame()
                    navController.navigate("game")  // Navigate to the GameScreen
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.visual),
                        contentDescription = "Visual",
                        modifier = Modifier
                            .height(48.dp)
                            .aspectRatio(3f / 2f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    // Using a fake ViewModel for preview purposes
    HomeScreen(vm = FakeVM(), navController = rememberNavController())
}
