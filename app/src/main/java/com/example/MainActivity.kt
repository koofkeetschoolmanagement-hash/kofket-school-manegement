package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.AppLanguage
import com.example.core.LocalAppLanguage
import com.example.core.ProvideAppLanguage
import com.example.core.configureAppCheck
import com.example.core.rememberAppLanguageState
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
import com.example.ui.screens.OnboardingScreen
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
            KoofkeetSchoolTheme {
                AppContent()
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
fun AppContent(auth: FirebaseAuth = Firebase.auth) {
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

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val schoolMeta by viewModel.schoolMeta.collectAsStateWithLifecycle()
    val pendingGrant by viewModel.pendingGrant.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    when (val state = profileState) {
        is UiState.Loading -> {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        is UiState.Error -> {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        is UiState.Success -> {
            val profile = state.data
            if (profile == null) {
                Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                    OnboardingScreen(
                        email = userEmail,
                        defaultName = userDisplayName,
                        schoolMeta = schoolMeta,
                        pendingGrant = pendingGrant,
                        isBusy = isBusy,
                        onComplete = { name, grade, claimAdmin ->
                            viewModel.completeOnboarding(name, grade, claimAdmin)
                        },
                        onSignOut = onSignOut,
                        onLanguageSelected = onLanguageSelected,
                        modifier = Modifier.padding(padding)
                    )
                }
            } else {
                KoofkeetMainShell(
                    profile = profile,
                    viewModel = viewModel,
                    onLanguageSelected = onLanguageSelected,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KoofkeetMainShell(
    profile: UserProfile,
    viewModel: SchoolViewModel,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentLanguage = LocalAppLanguage.current
    val localizedStrings = stringsFor(currentLanguage)
    var showLanguageDialog by remember { mutableStateOf(false) }

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
    val aiChatsState by viewModel.aiChatsState.collectAsStateWithLifecycle()
    val aiTutorState by viewModel.aiTutorState.collectAsStateWithLifecycle()

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
    val aiChats = (aiChatsState as? UiState.Success)?.data.orEmpty()

    val destinations = remember(profile.userRole) {
        buildList {
            add(NavDestination("dashboard", "Cuunfaa", Icons.Filled.Dashboard))
            add(NavDestination("academics", "Barnoota", Icons.AutoMirrored.Filled.MenuBook))
            add(NavDestination("ai_tutor", "AI Tutor", Icons.Filled.AutoAwesome))
            add(NavDestination("comms", "Qunnamtii", Icons.Filled.Forum))
            if (profile.userRole == UserRole.ADMIN) {
                add(NavDestination("admin", "Bulchiinsa", Icons.Filled.AdminPanelSettings))
            }
            add(NavDestination("developer", "Qopheessaa", Icons.Filled.Code))
        }
    }

    var currentRoute by rememberSaveable { mutableStateOf("dashboard") }
    if (currentRoute != "dashboard") {
        BackHandler { currentRoute = "dashboard" }
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
                            onClick = { currentRoute = dest.route },
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
                            onClick = { currentRoute = dest.route },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label, maxLines = 1) },
                            modifier = Modifier.testTag("rail_${dest.route}")
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                when (currentRoute) {
                    "dashboard" -> DashboardScreen(
                        profile = profile,
                        assignments = assignments,
                        submissions = submissions,
                        quizzes = quizzes,
                        examResults = examResults,
                        attendance = attendance,
                        materials = materials,
                        earlyWarnings = earlyWarnings,
                        announcements = announcements,
                        onNavigateToTab = { route -> currentRoute = route }
                    )
                    "academics" -> AcademicsScreen(
                        profile = profile,
                        students = students,
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
                            viewModel.recordExamResult(sid, sname, pid, subj, gr, ex, mon, sc, rem, profile)
                        },
                        onRecordAttendance = { sid, sname, pid, gr, dt, st, note ->
                            viewModel.recordAttendance(sid, sname, pid, gr, dt, st, note, profile)
                        },
                        onAddMaterial = { t, d, s, g, type, url, body ->
                            viewModel.addLearningMaterial(t, d, s, g, type, url, body, profile)
                        }
                    )
                    "ai_tutor" -> AiTutorScreen(
                        aiState = aiTutorState,
                        savedChats = aiChats,
                        onAskTutor = { prompt, subj, bmp ->
                            viewModel.askAiTutor(prompt, subj, bmp)
                        }
                    )
                    "comms" -> CommunicationScreen(
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
                    "admin" -> AdminManagementScreen(
                        teachers = teachers,
                        students = students,
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
                        onShowStatus = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                    "developer" -> DeveloperScreen()
                }
            }
        }
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
}
