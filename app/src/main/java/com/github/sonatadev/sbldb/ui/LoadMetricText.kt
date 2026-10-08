package com.github.sonatadev.sbldb.ui

import androidx.annotation.StringRes
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.domain.LoadMetric

/** Name on the selector: "e1RM", "Max", "Volume". */
@get:StringRes
val LoadMetric.shortName: Int
    get() = when (this) {
        LoadMetric.E1RM -> R.string.metric_e1rm
        LoadMetric.HEAVIEST -> R.string.metric_heaviest
        LoadMetric.VOLUME -> R.string.metric_volume
        LoadMetric.DURATION -> R.string.metric_duration
    }

/** Module label above the best value. */
@get:StringRes
val LoadMetric.bestLabel: Int
    get() = when (this) {
        LoadMetric.E1RM -> R.string.module_best_e1rm
        LoadMetric.HEAVIEST -> R.string.module_best_heaviest
        LoadMetric.VOLUME -> R.string.module_best_volume
        LoadMetric.DURATION -> R.string.module_best_duration
    }

/** One line on how the value is worked out. */
@get:StringRes
val LoadMetric.hint: Int
    get() = when (this) {
        LoadMetric.E1RM -> R.string.e1rm_hint
        LoadMetric.HEAVIEST -> R.string.heaviest_hint
        LoadMetric.VOLUME -> R.string.load_volume_hint
        LoadMetric.DURATION -> R.string.duration_hint
    }
