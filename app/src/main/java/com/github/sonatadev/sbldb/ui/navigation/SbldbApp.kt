package com.github.sonatadev.sbldb.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.ui.actions.ActionDetailScreen
import com.github.sonatadev.sbldb.ui.actions.ActionsScreen
import com.github.sonatadev.sbldb.ui.exercises.ExerciseDetailScreen
import com.github.sonatadev.sbldb.ui.log.LogScreen
import android.net.Uri
import com.github.sonatadev.sbldb.ui.glossary.GlossaryScreen
import com.github.sonatadev.sbldb.ui.home.HomeScreen
import com.github.sonatadev.sbldb.ui.muscles.MuscleDetailScreen
import com.github.sonatadev.sbldb.ui.routines.RoutineEditorScreen
import com.github.sonatadev.sbldb.ui.exercises.CustomExerciseScreen
import com.github.sonatadev.sbldb.ui.plan.VolumeTargetsScreen
import com.github.sonatadev.sbldb.ui.body.BodyScreen
import com.github.sonatadev.sbldb.ui.plan.WeeklyPlanScreen
import com.github.sonatadev.sbldb.ui.exercises.ExerciseListScreen
import com.github.sonatadev.sbldb.ui.settings.SettingsScreen
import com.github.sonatadev.sbldb.ui.volume.VolumeScreen
import com.github.sonatadev.sbldb.ui.workout.ActiveWorkoutScreen
import com.github.sonatadev.sbldb.ui.workout.WorkoutDetailScreen

private object Routes {
    const val HOME = "home"
    const val ACTIONS = "actions"
    const val LOG = "log"
    const val SETTINGS = "settings"
    const val ACTIVE_WORKOUT = "active_workout"
    const val WORKOUT_DETAIL = "workout/{workoutId}"
    const val EXERCISES = "exercises"
    const val EXERCISE_DETAIL = "exercise/{exerciseId}"
    const val ACTION_DETAIL = "action/{actionId}"
    const val ROUTINE = "routine/{routineId}"
    const val PICK_EXERCISE = "pick_exercise/{kind}/{targetId}"
    const val VOLUME = "volume"
    const val MUSCLE = "muscle/{group}"
    const val GLOSSARY = "glossary?term={term}"
    const val WEEKLY_PLAN = "weekly_plan"
    const val BODY = "body"
    const val VOLUME_TARGETS = "volume_targets"
    const val CUSTOM_EXERCISE = "custom_exercise?exerciseId={exerciseId}"

    fun muscle(group: String) = "muscle/${Uri.encode(group)}"
    fun glossary(term: String? = null) = "glossary?term=${Uri.encode(term.orEmpty())}"

    fun workoutDetail(id: Long) = "workout/$id"
    fun exerciseDetail(id: Int) = "exercise/$id"
    fun actionDetail(id: Int) = "action/$id"
    fun routine(id: Long) = "routine/$id"
    fun pickExercise(kind: String, targetId: Long) = "pick_exercise/$kind/$targetId"
    fun customExercise(id: Int? = null) = "custom_exercise?exerciseId=${id ?: -1}"
}

private enum class Tab(val route: String, @param:StringRes val label: Int) {
    HOME(Routes.HOME, R.string.tab_home),
    ACTIONS(Routes.ACTIONS, R.string.tab_actions),
    LOG(Routes.LOG, R.string.tab_log),
    SETTINGS(Routes.SETTINGS, R.string.tab_settings)
}

@Composable
fun SbldbApp(openWorkoutRequest: Int = 0, navController: NavHostController = rememberNavController()) {
    // Opened from the workout notification: jump to the workout in progress
    LaunchedEffect(openWorkoutRequest) {
        if (openWorkoutRequest > 0) navController.navigate(Routes.ACTIVE_WORKOUT) { launchSingleTop = true }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = Tab.entries.any { it.route == currentRoute }

    Scaffold(
        containerColor = SbldbTheme.colors.ground,
        // Only the bottom inset here: every screen pads for the status bar itself
        contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
        bottomBar = {
            if (showBottomBar) {
                TextNavBar(
                    tabs = Tab.entries,
                    selected = Tab.entries.firstOrNull { it.route == currentRoute },
                    label = { stringResource(it.label) },
                    onSelect = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            // Tabs switch instantly; detail screens get a short fade instead of the 700 ms default
            enterTransition = { if (isTabSwitch()) EnterTransition.None else fadeIn(tween(150)) },
            exitTransition = { if (isTabSwitch()) ExitTransition.None else fadeOut(tween(150)) },
            popEnterTransition = { if (isTabSwitch()) EnterTransition.None else fadeIn(tween(150)) },
            popExitTransition = { if (isTabSwitch()) ExitTransition.None else fadeOut(tween(150)) }
        ) {
            val openExercise: (Int) -> Unit = { navController.navigate(Routes.exerciseDetail(it)) }
            val back: () -> Unit = { navController.popBackStack() }
            val openMuscle: (String) -> Unit = { navController.navigate(Routes.muscle(it)) }
            val openGlossary: (String?) -> Unit = { navController.navigate(Routes.glossary(it)) }
            val openAction: (Int) -> Unit = { navController.navigate(Routes.actionDetail(it)) }

            composable(Routes.HOME) {
                HomeScreen(
                    onOpenActiveWorkout = { navController.navigate(Routes.ACTIVE_WORKOUT) { launchSingleTop = true } },
                    onEditRoutine = { navController.navigate(Routes.routine(it)) },
                    onOpenVolume = { navController.navigate(Routes.VOLUME) },
                    onOpenPlan = { navController.navigate(Routes.WEEKLY_PLAN) },
                    onOpenWorkout = { navController.navigate(Routes.workoutDetail(it)) }
                )
            }
            composable(Routes.ACTIONS) {
                ActionsScreen(
                    onOpenAction = openAction,
                    onOpenAllExercises = { navController.navigate(Routes.EXERCISES) },
                    onNewCustomExercise = { navController.navigate(Routes.customExercise()) },
                    onOpenGlossary = { openGlossary(null) }
                )
            }
            composable(Routes.LOG) {
                LogScreen(
                    onOpenWorkout = { navController.navigate(Routes.workoutDetail(it)) },
                    onOpenVolume = { navController.navigate(Routes.VOLUME) },
                    onOpenPlan = { navController.navigate(Routes.WEEKLY_PLAN) },
                    onOpenBody = { navController.navigate(Routes.BODY) }
                )
            }
            composable(Routes.BODY) { BodyScreen(onBack = back, onOpenGlossary = openGlossary) }
            composable(Routes.SETTINGS) {
                SettingsScreen(onOpenGlossary = { openGlossary(null) }, onOpenTargets = { navController.navigate(Routes.VOLUME_TARGETS) })
            }
            composable(Routes.WEEKLY_PLAN) {
                WeeklyPlanScreen(
                    onBack = back,
                    onEditRoutine = { navController.navigate(Routes.routine(it)) },
                    onOpenTargets = { navController.navigate(Routes.VOLUME_TARGETS) },
                    onOpenGlossary = openGlossary
                )
            }
            composable(Routes.VOLUME_TARGETS) { VolumeTargetsScreen(onBack = back, onOpenGlossary = openGlossary) }
            composable(Routes.VOLUME) { VolumeScreen(onBack = back, onOpenMuscle = openMuscle, onOpenGlossary = openGlossary) }
            composable(Routes.MUSCLE, arguments = listOf(navArgument("group") { type = NavType.StringType })) {
                MuscleDetailScreen(onBack = back, onOpenAction = openAction, onOpenExercise = openExercise)
            }
            composable(
                Routes.GLOSSARY,
                arguments = listOf(navArgument("term") { type = NavType.StringType; defaultValue = "" })
            ) {
                GlossaryScreen(onBack = back)
            }
            composable(Routes.EXERCISES) { ExerciseListScreen(onOpenExercise = openExercise, onBack = back) }
            composable(Routes.ACTIVE_WORKOUT) {
                ActiveWorkoutScreen(
                    onAddExercise = { navController.navigate(Routes.pickExercise("workout", it)) },
                    onOpenExercise = openExercise,
                    onOpenGlossary = openGlossary,
                    onClose = { navController.popBackStack(Routes.ACTIVE_WORKOUT, inclusive = true) }
                )
            }
            composable(Routes.WORKOUT_DETAIL, arguments = listOf(navArgument("workoutId") { type = NavType.LongType })) {
                WorkoutDetailScreen(
                    onBack = back,
                    onOpenExercise = openExercise,
                    onAddExercise = { navController.navigate(Routes.pickExercise("workout", it)) }
                )
            }
            composable(Routes.EXERCISE_DETAIL, arguments = listOf(navArgument("exerciseId") { type = NavType.IntType })) {
                ExerciseDetailScreen(
                    onBack = back,
                    onOpenAction = openAction,
                    onOpenMuscle = openMuscle,
                    onEdit = { navController.navigate(Routes.customExercise(it)) }
                )
            }
            composable(Routes.ACTION_DETAIL, arguments = listOf(navArgument("actionId") { type = NavType.IntType })) {
                ActionDetailScreen(onBack = back, onOpenExercise = openExercise, onOpenMuscle = openMuscle, onOpenGlossary = openGlossary)
            }
            composable(Routes.ROUTINE, arguments = listOf(navArgument("routineId") { type = NavType.LongType })) {
                RoutineEditorScreen(
                    onBack = back,
                    onAddExercise = { navController.navigate(Routes.pickExercise("routine", it)) },
                    onOpenPlan = { navController.navigate(Routes.WEEKLY_PLAN) }
                )
            }
            composable(
                Routes.PICK_EXERCISE,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType },
                    navArgument("targetId") { type = NavType.LongType }
                )
            ) {
                ExerciseListScreen(
                    onOpenExercise = openExercise,
                    onBack = back,
                    onNewCustomExercise = { navController.navigate(Routes.customExercise()) }
                )
            }
            composable(
                Routes.CUSTOM_EXERCISE,
                arguments = listOf(navArgument("exerciseId") { type = NavType.IntType; defaultValue = -1 })
            ) { entry ->
                val editing = (entry.arguments?.getInt("exerciseId") ?: -1) > 0
                CustomExerciseScreen(
                    onBack = back,
                    onSaved = { id ->
                        if (editing) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Routes.exerciseDetail(id)) {
                                popUpTo(Routes.CUSTOM_EXERCISE) { inclusive = true }
                            }
                        }
                    },
                    onRemoved = {
                        if (!navController.popBackStack(Routes.EXERCISE_DETAIL, inclusive = true)) navController.popBackStack()
                    },
                    onOpenGlossary = openGlossary
                )
            }
        }
    }
}

private val tabRoutes = setOf(Routes.HOME, Routes.ACTIONS, Routes.LOG, Routes.SETTINGS)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch() =
    initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes

/** Bottom navigation as mono labels, with an accent dot above the current tab. */
@Composable
private fun <T> TextNavBar(tabs: List<T>, selected: T?, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    val colors = SbldbTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.ground)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(selected = isSelected, role = Role.Tab) { onSelect(tab) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(Modifier.size(4.dp).background(if (isSelected) colors.accent else Color.Transparent, CircleShape))
                Text(label(tab).uppercase(), style = SbldbType.mono.copy(letterSpacing = 0.12.em), color = if (isSelected) colors.accent else colors.dim)
            }
        }
    }
}
