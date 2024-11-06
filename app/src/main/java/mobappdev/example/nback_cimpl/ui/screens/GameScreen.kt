package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.MatchStatus

@Composable
fun GameScreen(vm: GameViewModel) {
    val gameState by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()

    // Reset color flash after a delay
    if (gameState.matchStatus != MatchStatus.NONE) {
        LaunchedEffect(gameState.matchStatus) {
            kotlinx.coroutines.delay(500) // Flash duration of 500ms
            vm.resetMatchStatus() // New function to reset the status
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Score: $score",
            style = MaterialTheme.typography.headlineLarge
        )

        for (row in 0 until 3) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col + 1
                    val circleColor = when {
                        index == gameState.eventValue -> when (gameState.matchStatus) {
                            MatchStatus.CORRECT -> Color.Green
                            MatchStatus.INCORRECT -> Color.Red
                            else -> Color.Yellow
                        }
                        else -> Color.Gray
                    }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .padding(4.dp)
                            .background(circleColor, shape = CircleShape)
                    )
                }
            }
        }

        Button(
            onClick = { vm.startGame() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Start Game")
        }

        Button(
            onClick = { vm.checkMatch() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Check match")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    GameScreen(FakeVM()) // Preview with a fake VM to avoid needing real data
}
