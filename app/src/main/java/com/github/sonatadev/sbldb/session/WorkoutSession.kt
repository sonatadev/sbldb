package com.github.sonatadev.sbldb.session

import com.github.sonatadev.sbldb.domain.RestTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory state of the workout in progress that the database does not hold: the rest timer. */
class WorkoutSession {
    private val _rest = MutableStateFlow(RestTimer.Idle)
    val rest: StateFlow<RestTimer> = _rest.asStateFlow()

    /** What to do after the rest, for the notification ("Next: Bench press, set 3"). */
    val nextUp = MutableStateFlow<String?>(null)

    fun startRest(seconds: Int, next: String?) {
        _rest.value = RestTimer.start(seconds, System.currentTimeMillis())
        nextUp.value = next
    }

    fun adjustRest(deltaSeconds: Int) {
        _rest.value = _rest.value.adjust(deltaSeconds, System.currentTimeMillis())
    }

    fun skipRest() {
        _rest.value = RestTimer.Idle
    }
}
