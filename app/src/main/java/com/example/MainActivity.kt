package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.AppLanguage
import com.example.core.AppRoute
import com.example.core.AppThemeMode
import com.example.core.LocalAppLanguage
import com.example.core.ProvideAppLanguage
import com.example.core.RedirectReason
import com.example.core.RoleGuardState
import com.example.core.RoleNavigationGuard
import com.example.core.configureAppCheck
import com.example.core.isDarkThemeActive
import com.example.core.rememberAppLanguageState
import com.example.core.rememberAppThemeModeState
import com.example.core.stringsFor
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.LanguageSwitcherBar
import com.example.data.repository.AiTutorRepository
import com.example.data.repository.CommunicationAndRecordsRepository
import com.example.data.repository.SchoolRepository
import com.example.data.repository.attemptAutoSignIn
import com.example.data.repository.authStateFlow
import com.example.data.repository.signOut
import com.example.ui.screens.AcademicsScreen
import com.example.ui.screens.AdminManagementScreen
import com.example.ui.screens.AiTutorScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunicationScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeveloperScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.theme.KoofkeetSchoolTheme
import com.example.ui.viewmodel.SchoolViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val (themeMode, setThemeMode) = rememberAppThemeModeState()
            val darkTheme = isDarkThemeActive(themeMode)
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                }
            }
            KoofkeetSchoolTheme(darkTheme = darkTheme) {
                AppContent(
                    themeMode = themeMode,
                    onThemeModeSelected = setThemeMode
                )
            }
        }
    }
}

private data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppContent(
    auth: FirebaseAuth = Firebase.auth,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeSelected: (AppThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val (currentLanguage, setAppLanguage) = rememberAppLanguageState()

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    ProvideAppLanguage(language = currentLanguage) {
        val user = currentUser
        if (user == null) {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                AuthScreen(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = setAppLanguage,
                    onAuthSuccess = {},
                    modifier = Modifier.padding(padding)
                )
            }
        } else {
            key(user.uid) {
                AuthenticatedSchoolRoot(
                    currentUserId = user.uid,
                    userEmail = user.email.orEmpty(),
                    userDisplayName = user.displayName.orEmpty(),
                    themeMode = themeMode,
                    onThemeModeSelected = onThemeModeSelected,
                    onLanguageSelected = setAppLanguage,
                    onSignOut = {
                        signOut(credentialManager, scope)
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedSchoolRoot(
    currentUserId: String,
    userEmail: String,
    userDisplayName: String,
    themeMode: AppThemeMode,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: SchoolViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SchoolViewModel(
                    schoolRepo = SchoolRepository(db),
                    commsRepo = CommunicationAndRecordsRepository(db),
                    aiRepo = AiTutorRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val roleGuardState by viewModel.roleGuardState.collectAsStateWithLifecycle()
    val schoolMeta by viewModel.schoolMeta.collectAsStateWithLifecycle()
    val pendingGrant by viewModel.pendingGrant.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val localizedStrings = stringsFor(LocalAppLanguage.current)

    when (val state = roleGuardState) {
        is RoleGuardState.CheckingFirestoreRole -> {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                        .testTag("role_guard_loading"),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = localizedStrings.verifyingRoleTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = localizedStrings.verifyingRoleSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        is RoleGuardState.Error -> {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                        .testTag("role_guard_error"),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        androidx.compose.material3.Button(
                            onClick = { viewModel.refreshOnboardingContext() },
                            modifier = Modifier.testTag("role_guard_retry_button")
                        ) {
                            Text(localizedStrings.retryLabel)
                        }
                        androidx.compose.material3.OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("role_guard_sign_out_button")
                        ) {
                            Text(localizedStrings.signOutLabel)
                        }
                    }
                }
            }
        }
        is RoleGuardState.NeedsOnboarding -> {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                ProfileSetupScreen(
                    email = userEmail,
                    defaultName = userDisplayName,
                    schoolMeta = schoolMeta,
                    pendingGrant = pendingGrant,
                    isBusy = isBusy,
                    onComplete = { name, grade, lang, claimAdmin ->
                        onLanguageSelected(lang)
                        viewModel.completeOnboarding(name, grade, lang, claimAdmin)
                    },
                    onSignOut = onSignOut,
                    onLanguageSelected = onLanguageSelected,
                    modifier = Modifier.padding(padding)
                )
            }
        }
        is RoleGuardState.Authorized -> {
            LaunchedEffect(state.profile.userId, state.profile.preferredLanguage) {
                if (state.profile.preferredLanguage.isNotBlank()) {
                    onLanguageSelected(AppLanguage.fromCode(state.profile.preferredLanguage))
                }
            }
            KoofkeetMainShell(
                profile = state.profile,
                targetDashboard = state.targetDashboard,
                viewModel = viewModel,
                themeMode = themeMode,
                onThemeModeSelected = onThemeModeSelected,
                onLanguageSelected = { lang ->
                    onLanguageSelected(lang)
                    viewModel.updatePreferredLanguage(lang)
                },
                onSignOut = onSignOut
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KoofkeetMainShell(
    profile: UserProfile,
    targetDashboard: AppRoute,
    viewModel: SchoolViewModel,
    themeMode: AppThemeMode,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentLanguage = LocalAppLanguage.current
    val localizedStrings = stringsFor(currentLanguage)
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val darkActive = isDarkThemeActive(themeMode)

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val assignmentsState by viewModel.assignmentsState.collectAsStateWithLifecycle()
    val submissionsState by viewModel.submissionsState.collectAsStateWithLifecycle()
    val quizzesState by viewModel.quizzesState.collectAsStateWithLifecycle()
    val quizAttemptsState by viewModel.quizAttemptsState.collectAsStateWithLifecycle()
    val examResultsState by viewModel.examResultsState.collectAsStateWithLifecycle()
    val attendanceState by viewModel.attendanceState.collectAsStateWithLifecycle()
    val materialsState by viewModel.materialsState.collectAsStateWithLifecycle()
    val earlyWarningsState by viewModel.earlyWarningsState.collectAsStateWithLifecycle()
    val parentMessagesState by viewModel.parentMessagesState.collectAsStateWithLifecycle()
    val announcementsState by viewModel.announcementsState.collectAsStateWithLifecycle()
    val teachersState by viewModel.teacherAssignmentsState.collectAsStateWithLifecycle()
    val studentsState by viewModel.studentRegistrationsState.collectAsStateWithLifecycle()
    val studentRoster by viewModel.studentRosterState.collectAsStateWithLifecycle()
    val usersState by viewModel.usersState.collectAsStateWithLifecycle()
    val roleGrantsState by viewModel.roleGrantsState.collectAsStateWithLifecycle()
    val aiChatsState by viewModel.aiChatsState.collectAsStateWithLifecycle()
    val aiTutorState by viewModel.aiTutorState.collectAsStateWithLifecycle()
    val calendarEventsState by viewModel.calendarEventsState.collectAsStateWithLifecycle()
    val attendanceSyncStatus by viewModel.attendanceSyncStatus.collectAsStateWithLifecycle()

    val assignments = (assignmentsState as? UiState.Success)?.data.orEmpty()
    val submissions = (submissionsState as? UiState.Success)?.data.orEmpty()
    val quizzes = (quizzesState as? UiState.Success)?.data.orEmpty()
    val quizAttempts = (quizAttemptsState as? UiState.Success)?.data.orEmpty()
    val examResults = (examResultsState as? UiState.Success)?.data.orEmpty()
    val attendance = (attendanceState as? UiState.Success)?.data.orEmpty()
    val materials = (materialsState as? UiState.Success)?.data.orEmpty()
    val earlyWarnings = (earlyWarningsState as? UiState.Success)?.data.orEmpty()
    val parentMessages = (parentMessagesState as? UiState.Success)?.data.orEmpty()
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
    val teachers = (teachersState as? UiState.Success)?.data.orEmpty()
    val students = (studentsState as? UiState.Success)?.data.orEmpty()
    val users = (usersState as? UiState.Success)?.data.orEmpty()
    val roleGrants = (roleGrantsState as? UiState.Success)?.data.orEmpty()
    val aiChats = (aiChatsState as? UiState.Success)?.data.orEmpty()
    val calendarEvents = (calendarEventsState as? UiState.Success)?.data.orEmpty()
    val availableMonths = remember(examResults, attendance) {
        val extracted = (examResults.map { it.monthLabel.trim() } + attendance.map { it.dateLabel.trim().take(7) })
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedDescending()
        listOf("Hunda") + extracted
    }
    var requestedAcademicSection by remember { mutableStateOf<com.example.ui.screens.AcademicSection?>(null) }

    val destinations = remember(profile.userRole, targetDashboard) {
        buildList {
            add(NavDestination(targetDashboard.routeId, "Cuunfaa", Icons.Filled.Dashboard))
            add(NavDestination(AppRoute.ACADEMICS.routeId, "Barnoota", Icons.AutoMirrored.Filled.MenuBook))
            add(NavDestination(AppRoute.AI_TUTOR.routeId, "AI Tutor", Icons.Filled.AutoAwesome))
            add(NavDestination(AppRoute.COMMS.routeId, "Qunnamtii", Icons.Filled.Forum))
            add(NavDestination(AppRoute.PERFORMANCE.routeId, "Raawwii", Icons.Filled.ShowChart))
            add(NavDestination(AppRoute.NOTIFICATIONS.routeId, "Beeksisa", Icons.Filled.NotificationsActive))
            if (profile.userRole == UserRole.ADMIN) {
                add(NavDestination(AppRoute.ADMIN_MANAGEMENT.routeId, "Bulchiinsa", Icons.Filled.AdminPanelSettings))
            }
            add(NavDestination(AppRoute.USER_MANUAL.routeId, "Maanuwaalii", Icons.AutoMirrored.Filled.MenuBook))
            add(NavDestination(AppRoute.DEVELOPER.routeId, "Qopheessaa", Icons.Filled.Code))
        }
    }

    var currentRoute by rememberSaveable { mutableStateOf(targetDashboard.routeId) }
    var lastGuardRoleName by rememberSaveable { mutableStateOf(profile.userRole.name) }

    val guardedNavigate: (String) -> Unit = { requestedRoute ->
        val resolution = RoleNavigationGuard.resolveRoute(requestedRoute, profile.userRole)
        currentRoute = resolution.targetRoute.routeId
        if (resolution.wasRedirected && resolution.reason == RedirectReason.UNAUTHORIZED_ROLE_ACCESS) {
            scope.launch { snackbarHostState.showSnackbar(localizedStrings.accessDeniedRedirect) }
        }
    }

    // Live Navigation Guard: redirects to the matching role dashboard on login or live role update in Firestore
    LaunchedEffect(profile.userRole, currentRoute) {
        val previousRole = UserRole.fromRaw(lastGuardRoleName)
        val resolution = RoleNavigationGuard.resolveRoute(
            requestedRouteId = currentRoute,
            role = profile.userRole,
            previousRole = previousRole
        )
        if (previousRole != profile.userRole) {
            lastGuardRoleName = profile.userRole.name
        }
        if (resolution.wasRedirected) {
            currentRoute = resolution.targetRoute.routeId
            if (resolution.reason == RedirectReason.UNAUTHORIZED_ROLE_ACCESS) {
                snackbarHostState.showSnackbar(localizedStrings.accessDeniedRedirect)
            }
        }
    }

    if (currentRoute != targetDashboard.routeId) {
        BackHandler { currentRoute = targetDashboard.routeId }
    }

    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 720

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (darkActive) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = localizedStrings.themeChooseTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { guardedNavigate(AppRoute.NOTIFICATIONS.routeId) },
                        modifier = Modifier.testTag("notifications_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = localizedStrings.notificationTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("student_profile_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = localizedStrings.studentProfileTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.testTag("language_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = localizedStrings.chooseLanguageTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = localizedStrings.signOutLabel,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!isWideScreen) {
                NavigationBar {
                    destinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { guardedNavigate(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = {
                                Text(
                                    text = dest.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_${dest.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWideScreen) {
                NavigationRail {
                    destinations.forEach { dest ->
                        NavigationRailItem(
                            selected = currentRoute == dest.route,
                            onClick = { guardedNavigate(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label, maxLines = 1) },
                            modifier = Modifier.testTag("rail_${dest.route}")
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                val safeRoute = RoleNavigationGuard.resolveRoute(currentRoute, profile.userRole).targetRoute
                when (safeRoute) {
                    AppRoute.ADMIN_DASHBOARD,
                    AppRoute.TEACHER_DASHBOARD,
                    AppRoute.STUDENT_DASHBOARD,
                    AppRoute.PARENT_DASHBOARD -> DashboardScreen(
                        profile = profile,
                        assignments = assignments,
                        submissions = submissions,
                        quizzes = quizzes,
                        examResults = examResults,
                        attendance = attendance,
                        materials = materials,
                        earlyWarnings = earlyWarnings,
                        announcements = announcements,
                        onNavigateToTab = guardedNavigate,
                        aiState = aiTutorState,
                        aiChats = aiChats,
                        onAskAiTutor = { prompt, subj, mode ->
                            viewModel.askAiTutor(
                                prompt = prompt,
                                subject = subj,
                                studyMode = mode,
                                language = currentLanguage,
                                gradeLevel = profile.gradeLevel
                            )
                        },
                        students = studentRoster,
                        onMarkAttendance = { sid, sname, pid, gr, dt, st, note ->
                            viewModel.recordAttendance(
                                sid, sname, pid, gr, dt, st, note, profile,
                                successMessage = localizedStrings.attendanceSavedToast
                            )
                        },
                        onMarkAllPresent = { dt, list, existing ->
                            viewModel.markAllPresentForDate(
                                dt, list, existing, profile,
                                successPrefix = localizedStrings.attendanceBatchSavedPrefix
                            )
                        },
                        onOpenAttendance = {
                            requestedAcademicSection = com.example.ui.screens.AcademicSection.ATTENDANCE
                            guardedNavigate(AppRoute.ACADEMICS.routeId)
                        },
                        quizAttempts = quizAttempts,
                        onRecordGrade = { sid, sname, pid, subj, gr, ex, mon, sc, rem ->
                            viewModel.recordExamResult(
                                sid, sname, pid, subj, gr, ex, mon, sc, rem, profile,
                                successMessage = localizedStrings.gradesSavedToast
                            )
                        },
                        onDeleteGrade = { resultId ->
                            viewModel.deleteExamResult(resultId, localizedStrings.gradesDeletedToast)
                        },
                        onCreateAssignment = { t, d, s, g, due ->
                            viewModel.createAssignment(t, d, s, g, due, profile)
                        },
                        onOpenAcademicSection = { section ->
                            requestedAcademicSection = section
                            guardedNavigate(AppRoute.ACADEMICS.routeId)
                        },
                        attendanceSyncStatus = attendanceSyncStatus,
                        onSyncAttendance = {
                            viewModel.syncAttendance(
                                profile = profile,
                                syncedMessage = localizedStrings.attendanceSyncCompletedToast,
                                offlineMessage = localizedStrings.attendanceSyncOfflineToast
                            )
                        },
                        availableReportMonths = availableMonths,
                        buildMonthlyReport = { month -> viewModel.buildMonthlyReport(month) },
                        calendarEvents = calendarEvents,
                        onCreateCalendarEvent = { t, d, dt, st, en, cat, gr, subj ->
                            viewModel.upsertCalendarEvent(
                                title = t,
                                description = d,
                                eventDateLabel = dt,
                                startTimeLabel = st,
                                endTimeLabel = en,
                                category = cat,
                                gradeLevel = gr,
                                subject = subj,
                                profile = profile,
                                successMessage = localizedStrings.calendarSavedToast
                            )
                        },
                        onSyncAcademicsToCalendar = {
                            viewModel.syncAcademicsToCalendar(
                                profile = profile,
                                successPrefix = localizedStrings.calendarSyncAcademicsToast
                            )
                        },
                        onShowStatus = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                    AppRoute.ACADEMICS -> AcademicsScreen(
                        profile = profile,
                        students = studentRoster,
                        assignments = assignments,
                        submissions = submissions,
                        quizzes = quizzes,
                        quizAttempts = quizAttempts,
                        examResults = examResults,
                        attendance = attendance,
                        materials = materials,
                        onCreateAssignment = { t, d, s, g, due ->
                            viewModel.createAssignment(t, d, s, g, due, profile)
                        },
                        onSubmitAssignment = { hw, ans ->
                            viewModel.submitAssignment(hw, profile, ans)
                        },
                        onGradeSubmission = { id, score, fb ->
                            viewModel.gradeSubmission(id, score, fb)
                        },
                        onCreateQuiz = { t, s, g, dur, qs ->
                            viewModel.createQuiz(t, s, g, dur, qs, profile)
                        },
                        onSubmitQuizAttempt = { quiz, sel ->
                            viewModel.submitQuizAttempt(quiz, profile, sel)
                        },
                        onRecordExamResult = { sid, sname, pid, subj, gr, ex, mon, sc, rem ->
                            viewModel.recordExamResult(
                                sid, sname, pid, subj, gr, ex, mon, sc, rem, profile,
                                successMessage = localizedStrings.gradesSavedToast
                            )
                        },
                        onRecordAttendance = { sid, sname, pid, gr, dt, st, note ->
                            viewModel.recordAttendance(
                                sid, sname, pid, gr, dt, st, note, profile,
                                successMessage = localizedStrings.attendanceSavedToast
                            )
                        },
                        onAddMaterial = { t, d, s, g, type, url, body ->
                            viewModel.addLearningMaterial(t, d, s, g, type, url, body, profile)
                        },
                        requestedSection = requestedAcademicSection,
                        onConsumeRequestedSection = { requestedAcademicSection = null },
                        onMarkAllPresent = { dt, list, existing ->
                            viewModel.markAllPresentForDate(
                                dt, list, existing, profile,
                                successPrefix = localizedStrings.attendanceBatchSavedPrefix
                            )
                        },
                        onDeleteAttendance = { attId ->
                            viewModel.deleteAttendanceRecord(attId, localizedStrings.attendanceDeletedToast)
                        },
                        onDeleteExamResult = { resultId ->
                            viewModel.deleteExamResult(resultId, localizedStrings.gradesDeletedToast)
                        },
                        attendanceSyncStatus = attendanceSyncStatus,
                        onSyncAttendance = {
                            viewModel.syncAttendance(
                                profile = profile,
                                syncedMessage = localizedStrings.attendanceSyncCompletedToast,
                                offlineMessage = localizedStrings.attendanceSyncOfflineToast
                            )
                        },
                        calendarEvents = calendarEvents,
                        onCreateCalendarEvent = { t, d, dt, st, en, cat, gr, subj ->
                            viewModel.upsertCalendarEvent(
                                title = t,
                                description = d,
                                eventDateLabel = dt,
                                startTimeLabel = st,
                                endTimeLabel = en,
                                category = cat,
                                gradeLevel = gr,
                                subject = subj,
                                profile = profile,
                                successMessage = localizedStrings.calendarSavedToast
                            )
                        },
                        onDeleteCalendarEvent = { eventId ->
                            viewModel.deleteCalendarEvent(eventId, localizedStrings.calendarDeletedToast)
                        },
                        onSyncAcademicsToCalendar = {
                            viewModel.syncAcademicsToCalendar(
                                profile = profile,
                                successPrefix = localizedStrings.calendarSyncAcademicsToast
                            )
                        },
                        onShowStatus = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                    AppRoute.AI_TUTOR -> AiTutorScreen(
                        aiState = aiTutorState,
                        savedChats = aiChats,
                        onAskTutor = { prompt, subj, mode, bmp ->
                            viewModel.askAiTutor(
                                prompt = prompt,
                                subject = subj,
                                studyMode = mode,
                                attachedBitmap = bmp,
                                language = currentLanguage,
                                gradeLevel = profile.gradeLevel
                            )
                        },
                        onDeleteChat = { chatId -> viewModel.deleteAiChat(chatId) },
                        onClearHistory = { viewModel.clearAllPrivateAiChats() }
                    )
                    AppRoute.COMMS -> CommunicationScreen(
                        profile = profile,
                        announcements = announcements,
                        parentMessages = parentMessages,
                        earlyWarnings = earlyWarnings,
                        teachers = teachers,
                        onPostAnnouncement = { t, b, c ->
                            viewModel.postAnnouncement(t, b, c, profile)
                        },
                        onSendParentMessage = { pid, pname, sid, sname, rrole, rid, subj, body ->
                            viewModel.sendParentMessage(profile, pid, pname, sid, sname, rrole, rid, subj, body)
                        },
                        onResolveWarning = { wid -> viewModel.resolveEarlyWarning(wid) }
                    )
                    AppRoute.ADMIN_MANAGEMENT -> AdminManagementScreen(
                        currentAdminUid = profile.userId,
                        users = users,
                        roleGrants = roleGrants,
                        teachers = teachers,
                        students = students,
                        availableMonths = availableMonths,
                        buildMonthlyReport = { month -> viewModel.buildMonthlyReport(month) },
                        onAddTeacherAssignment = { tid, tname, subj, gr, sch ->
                            viewModel.addTeacherAssignment(tid, tname, subj, gr, sch)
                        },
                        onRegisterStudent = { suid, sname, scode, gr, sec, pid, pname ->
                            viewModel.registerStudent(suid, sname, scode, gr, sec, pid, pname)
                        },
                        onIssueRoleGrant = { em, role, fname, gr, subj, lsid, lsname ->
                            viewModel.issueRoleGrant(em, role, fname, gr, subj, lsid, lsname)
                        },
                        onUpdateUserRole = { targetUser, newRole, subj, gr ->
                            viewModel.updateUserRole(targetUser, newRole, subj, gr)
                        },
                        onRevokeRoleGrant = { email ->
                            viewModel.revokeRoleGrant(email)
                        },
                        onShowStatus = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                    AppRoute.DEVELOPER -> DeveloperScreen()
                    AppRoute.USER_MANUAL -> com.example.ui.screens.UserManualScreen()
                    AppRoute.PERFORMANCE -> com.example.ui.screens.PerformanceDashboardScreen(
                        profile = profile,
                        examResults = examResults,
                        attendance = attendance,
                        earlyWarnings = earlyWarnings
                    )
                    AppRoute.NOTIFICATIONS -> com.example.ui.screens.NotificationScreen(
                        announcements = announcements,
                        earlyWarnings = earlyWarnings
                    )
                }
            }
        }
    }

    if (showProfileDialog) {
                        com.example.ui.screens.StudentProfileDialog(
                            profile = profile,
                            onDismiss = { showProfileDialog = false }
                        )
                    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(localizedStrings.chooseLanguageTitle) },
            text = {
                LanguageSwitcherBar(
                    selectedLanguage = currentLanguage,
                    onLanguageSelected = { lang ->
                        onLanguageSelected(lang)
                        showLanguageDialog = false
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(localizedStrings.closeLabel)
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(localizedStrings.themeChooseTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(
                        Triple(AppThemeMode.SYSTEM, localizedStrings.themeSystem, Icons.Filled.BrightnessAuto),
                        Triple(AppThemeMode.LIGHT, localizedStrings.themeLight, Icons.Filled.LightMode),
                        Triple(AppThemeMode.DARK, localizedStrings.themeDark, Icons.Filled.DarkMode)
                    )
                    options.forEach { (mode, label, icon) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = {
                                onThemeModeSelected(mode)
                                showThemeDialog = false
                            },
                            leadingIcon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("theme_option_${mode.storageKey}")
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(localizedStrings.closeLabel)
                }
            }
        )
    }
}
