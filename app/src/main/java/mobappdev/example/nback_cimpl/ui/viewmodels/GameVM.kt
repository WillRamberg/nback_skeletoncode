package mobappdev.example.nback_cimpl.ui.viewmodels

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import java.util.Locale

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */


interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: StateFlow<Int>
    val eventInterval: StateFlow<Long>
    val correctNumbers: StateFlow<Int>
    var currentIndex: StateFlow<Int>

    fun setGameType(gameType: GameType)
    fun startGame()

    fun checkMatch()
    fun resetMatchStatus()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context,
): GameViewModel, ViewModel(), TextToSpeech.OnInitListener{
    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState>
        get() = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int>
        get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore

    // nBack is currently hardcoded
    private val _nBack = MutableStateFlow(2)
    override val nBack: StateFlow<Int>
        get() = _nBack

    private var _currentIndex = MutableStateFlow(-1)  // Correct numbers counter
    override var currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var matchChecked = false

    private var job: Job? = null  // coroutine job for the game event

    private val _correctNumbers = MutableStateFlow(0)  // Correct numbers counter
    override val correctNumbers: StateFlow<Int> = _correctNumbers.asStateFlow()

    private val _eventInterval = MutableStateFlow(2000L)  // Event interval (time between events in milliseconds)
    override val eventInterval: StateFlow<Long> = _eventInterval


    private val nBackHelper = NBackHelper()  // Helper that generate the event array
    private var events = emptyArray<Int>()  // Array with all events

    private var textToSpeech: TextToSpeech? = null


    override fun setGameType(gameType: GameType) {
        // update the gametype in the gamestate
        _gameState.value = _gameState.value.copy(gameType = gameType)
        Log.d("GameVM", "Setting game type to: $gameType")
    }

    override fun startGame() {
        resetGame()  // Reset game before starting a new game

        // Get the events from our C-model (returns IntArray, so we need to convert to Array<Int>)
        events = nBackHelper.generateNBackString(10, 9, 30, nBack.value).toList().toTypedArray()  // Todo Higher Grade: currently the size etc. are hardcoded, make these based on user input
        Log.d("GameVM", "The following sequence was generated: ${events.contentToString()}")

        job = viewModelScope.launch {
            when (gameState.value.gameType) {
                GameType.Audio -> runAudioGame(events)
                GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame(events)
            }
            if(_highscore.value < _score.value){
                userPreferencesRepository.saveHighScore(_score.value)
            }
        }
    }

    override fun checkMatch() {
        if (currentIndex.value >= nBack.value) {
            val currentEvent = _gameState.value.eventValue
            val nBackEvent = events[currentIndex.value - nBack.value]

            if (currentEvent == nBackEvent && !matchChecked) {
                _score.value += 1
                _correctNumbers.value += 1
                matchChecked = true
                _gameState.value = _gameState.value.copy(matchStatus = MatchStatus.CORRECT)
            } else if (!matchChecked) {
                matchChecked = true
                if(_score.value > 0)
                    _score.value -= 1
                _gameState.value = _gameState.value.copy(matchStatus = MatchStatus.INCORRECT)
            }
        } else {
            _gameState.value = _gameState.value.copy(matchStatus = MatchStatus.NONE)
        }
    }

    override fun resetMatchStatus() {
        _gameState.value = _gameState.value.copy(matchStatus = MatchStatus.NONE)
    }

    private fun resetGame() {
        job?.cancel() // Cancel any ongoing game loop

        // Reset all necessary properties
        _score.value = 0  // Reset score to 0
        _currentIndex.value = -1  // Reset index to its initial state
        matchChecked = false // Reset match check status
        events = emptyArray() // Clear any generated events
        _correctNumbers.value = 0

        Log.d("GameVM", "Game has been reset.")
    }

    private suspend fun runAudioGame(events: Array<Int>) {
        for (value in events) {
            Log.d("GameVM", "Now playing audio game")
            _gameState.value = _gameState.value.copy(eventValue = value) // Update eventValue
            _currentIndex.value += 1
            playAudioForEvent(value)  // Now this uses TTS to speak the event letter
            delay(eventInterval.value) // Delay to wait for the next event
            matchChecked = false
        }
    }

    private fun mapEventValueToLetter(eventValue: Int): String {
        return if (eventValue in 1..9) {
            ('A' + eventValue - 1).toString()  // Maps 1 -> 'A', 2 -> 'B', ...
        } else {
            "Invalid" // Return invalid if the value is outside the range
        }
    }

    private fun playAudioForEvent(eventValue: Int) {
        val eventLetter = mapEventValueToLetter(eventValue)
        Log.d("GameVM", "Playing TTS for event: $eventLetter")

        // Use TTS to read out the event letter
        speak(eventLetter)
    }

    private suspend fun runVisualGame(events: Array<Int>) {
        for (value in events) {
            Log.d("GameVM", "Now playing visual game")
            _gameState.value = _gameState.value.copy(eventValue = value) // Update eventValue
            _currentIndex.value += 1
            delay(eventInterval.value) // Delay to see the color change
            matchChecked = false
        }
    }

    private fun runAudioVisualGame(){
        // Todo: Make work for Higher grade
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository, application.applicationContext)
            }
        }
    }


    init {
        // Code that runs during creation of the vm
        textToSpeech = TextToSpeech(context, this)

        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // The TTS engine was initialized successfully
            val langResult = textToSpeech?.setLanguage(Locale.ENGLISH)

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("GameVM", "Language is not supported or missing data")
            } else {
                Log.d("GameVM", "Text-to-Speech Initialized successfully")
            }
        } else {
            Log.e("GameVM", "Text-to-Speech initialization failed")
        }
    }

    private fun speak(text: String) {
        textToSpeech?.let {
            if (it.isSpeaking) {
                it.stop() // Stop speaking if something is already being spoken
            }
            it.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) // Speak the event
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.apply {
            stop()  // Stop any ongoing speech
            shutdown()  // Properly release TTS resources
        }
    }
}

// Class with the different game types
enum class GameType{
    Audio,
    Visual,
    AudioVisual
}

enum class MatchStatus {
    CORRECT, INCORRECT, NONE
}

data class GameState(
    val gameType: GameType = GameType.Visual,
    val eventValue: Int = -1,
    val matchStatus: MatchStatus = MatchStatus.NONE // Track match status
)

class FakeVM: GameViewModel{
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()
    override val nBack: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val eventInterval: StateFlow<Long>
        get() = MutableStateFlow(2000L).asStateFlow()
    override val correctNumbers: StateFlow<Int>
        get() = MutableStateFlow(3).asStateFlow()
    override var currentIndex: StateFlow<Int> = MutableStateFlow(-1)
        get() = MutableStateFlow(-1).asStateFlow()

    override fun setGameType(gameType: GameType) {
    }

    override fun startGame() {
    }

    override fun checkMatch() {
    }

    override fun resetMatchStatus() {
    }
}