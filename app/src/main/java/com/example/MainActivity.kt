package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.ui.screens.ProjectWorkspaceScreen
import com.example.ui.screens.ProjectsListScreen
import com.example.ui.screens.ProvidersScreen
import com.example.ui.screens.StudioCreateScreen
import com.example.ui.theme.AiVideoCreatorTheme
import com.example.ui.theme.ElectricCyan
import com.example.ui.viewmodel.ProjectWorkspaceViewModel
import com.example.ui.viewmodel.ProvidersViewModel
import com.example.ui.viewmodel.StudioViewModel

object StudioRoutes {
    const val CREATE = "studio_create"
    const val PROJECTS = "studio_projects"
    const val PROVIDERS = "studio_providers"
    const val WORKSPACE = "project_workspace/{projectId}"

    fun workspace(projectId: String) = "project_workspace/$projectId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiVideoCreatorTheme(darkTheme = true) {
                AiVideoCreatorApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVideoCreatorApp(
    studioViewModel: StudioViewModel = viewModel(),
    providersViewModel: ProvidersViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: StudioRoutes.CREATE

    val appLanguage by studioViewModel.appLanguage.collectAsStateWithLifecycle()
    val draft by studioViewModel.draft.collectAsStateWithLifecycle()
    val projects by studioViewModel.projects.collectAsStateWithLifecycle()
    val strings = stringsFor(appLanguage)

    val isWorkspaceRoute = currentRoute.startsWith("project_workspace")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 700.dp && !isWorkspaceRoute

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (!isWorkspaceRoute) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        title = {
                            Column {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.studioSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                        },
                        actions = {
                            OutlinedButton(
                                onClick = { studioViewModel.toggleAppLanguage() },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .testTag("language_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Switch Language",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (appLanguage == AppLanguage.ENGLISH) "हिंदी / EN" else "EN / हिंदी",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!isWorkspaceRoute && !useNavigationRail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == StudioRoutes.CREATE,
                            onClick = {
                                navController.navigate(StudioRoutes.CREATE) {
                                    popUpTo(StudioRoutes.CREATE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = strings.navCreate) },
                            label = { Text(strings.navCreate) },
                            modifier = Modifier.testTag("nav_create")
                        )
                        NavigationBarItem(
                            selected = currentRoute == StudioRoutes.PROJECTS,
                            onClick = {
                                navController.navigate(StudioRoutes.PROJECTS) {
                                    popUpTo(StudioRoutes.CREATE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.FolderCopy, contentDescription = strings.navProjects) },
                            label = { Text("${strings.navProjects} (${projects.size})") },
                            modifier = Modifier.testTag("nav_projects")
                        )
                        NavigationBarItem(
                            selected = currentRoute == StudioRoutes.PROVIDERS,
                            onClick = {
                                navController.navigate(StudioRoutes.PROVIDERS) {
                                    popUpTo(StudioRoutes.CREATE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Hub, contentDescription = strings.navProviders) },
                            label = { Text(strings.navProviders) },
                            modifier = Modifier.testTag("nav_providers")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = currentRoute == StudioRoutes.CREATE,
                            onClick = {
                                navController.navigate(StudioRoutes.CREATE) {
                                    popUpTo(StudioRoutes.CREATE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = strings.navCreate) },
                            label = { Text(strings.navCreate) }
                        )
                        NavigationRailItem(
                            selected = currentRoute == StudioRoutes.PROJECTS,
                            onClick = {
                                navController.navigate(StudioRoutes.PROJECTS) {
                                    popUpTo(StudioRoutes.CREATE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.FolderCopy, contentDescription = strings.navProjects) },
                            label = { Text(strings.navProjects) }
                        )
                        NavigationRailItem(
                            selected = currentRoute == StudioRoutes.PROVIDERS,
                            onClick = {
                                navController.navigate(StudioRoutes.PROVIDERS) {
                                    popUpTo(StudioRoutes.CREATE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Hub, contentDescription = strings.navProviders) },
                            label = { Text(strings.navProviders) }
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = StudioRoutes.CREATE,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(StudioRoutes.CREATE) {
                        StudioCreateScreen(
                            draft = draft,
                            strings = strings,
                            appLanguage = appLanguage,
                            viewModel = studioViewModel,
                            onProjectCreated = { projectId ->
                                navController.navigate(StudioRoutes.workspace(projectId))
                            }
                        )
                    }
                    composable(StudioRoutes.PROJECTS) {
                        ProjectsListScreen(
                            projects = projects,
                            strings = strings,
                            appLanguage = appLanguage,
                            onOpenProject = { id ->
                                navController.navigate(StudioRoutes.workspace(id))
                            },
                            onDeleteProject = { id ->
                                studioViewModel.deleteProject(id)
                            },
                            onCreateNewClick = {
                                navController.navigate(StudioRoutes.CREATE) {
                                    popUpTo(StudioRoutes.CREATE) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(StudioRoutes.PROVIDERS) {
                        ProvidersScreen(
                            viewModel = providersViewModel,
                            strings = strings,
                            appLanguage = appLanguage
                        )
                    }
                    composable(
                        route = StudioRoutes.WORKSPACE,
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                    ) { entry ->
                        val projectId = entry.arguments?.getString("projectId").orEmpty()
                        val app = LocalContext.current.applicationContext as Application
                        val workspaceVm: ProjectWorkspaceViewModel = viewModel(
                            key = projectId,
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return ProjectWorkspaceViewModel(app, projectId) as T
                                }
                            }
                        )
                        ProjectWorkspaceScreen(
                            viewModel = workspaceVm,
                            strings = strings,
                            appLanguage = appLanguage,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
