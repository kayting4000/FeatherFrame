package com.featherframe.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.featherframe.app.FeatherFrameApp
import com.featherframe.app.domain.ai.BirdClassifier
import com.featherframe.app.domain.camera.ManualCameraEngine
import com.featherframe.app.domain.location.GPSManager
import com.featherframe.app.domain.auth.SessionManager
import com.featherframe.app.domain.auth.GoogleAuthHelper
import com.featherframe.app.data.processing.ImageProcessor
import com.featherframe.app.ui.screens.*
import com.featherframe.app.ui.workers.SyncUploadWorker
import java.io.File

/**
 * MainActivity — Entry point for FeatherFrame.
 * Minimalist black & white outline theme.
 */
class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var googleAuthHelper: GoogleAuthHelper
    private lateinit var cameraEngine: ManualCameraEngine
    private lateinit var birdClassifier: BirdClassifier
    private lateinit var gpsManager: GPSManager
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var dngOutputDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(applicationContext)
        googleAuthHelper = GoogleAuthHelper(applicationContext)
        cameraEngine = ManualCameraEngine(applicationContext)
        birdClassifier = BirdClassifier(applicationContext)
        gpsManager = GPSManager(applicationContext)
        imageProcessor = ImageProcessor()

        dngOutputDir = File(filesDir, "dng_captures")
        if (!dngOutputDir.exists()) dngOutputDir.mkdirs()

        SyncUploadWorker.schedule(applicationContext)

        setContent {
            FeatherFrameTheme {
                FeatherFrameNavHost(
                    sessionManager = sessionManager,
                    googleAuthHelper = googleAuthHelper,
                    cameraEngine = cameraEngine,
                    birdClassifier = birdClassifier,
                    gpsManager = gpsManager,
                    imageProcessor = imageProcessor,
                    dngOutputDir = dngOutputDir,
                    isLoggedIn = sessionManager.isLoggedIn()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraEngine.release()
        birdClassifier.close()
    }
}

/**
 * Minimalist black & white outline Material3 theme.
 * Uses Inter font for clean typography.
 */
@Composable
fun FeatherFrameTheme(content: @Composable () -> Unit) {
    val interFontFamily = FontFamily.Default // In production, use FontFamily("Inter")

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color.Black,
            onPrimary = Color.White,
            primaryContainer = Color.White,
            onPrimaryContainer = Color.Black,
            secondary = Color.Black.copy(alpha = 0.6f),
            onSecondary = Color.Black,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color.White,
            onSurfaceVariant = Color.Black.copy(alpha = 0.6f),
            outline = Color.Black.copy(alpha = 0.15f),
            outlineVariant = Color.Black.copy(alpha = 0.08f),
            error = Color.Black
        ),
        typography = Typography(
            displayLarge = TextStyle(
                fontFamily = interFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 36.sp,
                color = Color.Black
            ),
            headlineLarge = TextStyle(
                fontFamily = interFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                color = Color.Black
            ),
            headlineMedium = TextStyle(
                fontFamily = interFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                color = Color.Black
            ),
            titleLarge = TextStyle(
                fontFamily = interFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = Color.Black
            ),
            titleMedium = TextStyle(
                fontFamily = interFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color.Black
            ),
            bodyLarge = TextStyle(
                fontFamily = interFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = Color.Black
            ),
            bodyMedium = TextStyle(
                fontFamily = interFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = Color.Black
            ),
            bodySmall = TextStyle(
                fontFamily = interFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.6f)
            ),
            labelLarge = TextStyle(
                fontFamily = interFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.Black
            ),
            labelSmall = TextStyle(
                fontFamily = interFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Color.Black.copy(alpha = 0.5f)
            )
        ),
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp)
        ),
        content = content
    )
}

/**
 * Navigation destinations.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Camera : Screen("camera")
    object NewsFeed : Screen("newsfeed")
    object Profile : Screen("profile")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: @Composable () -> Unit
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.NewsFeed, "Feed", { Icon(Icons.Default.Home, contentDescription = "Feed") }),
    BottomNavItem(Screen.Camera, "Camera", { Icon(Icons.Default.CameraAlt, contentDescription = "Camera") }),
    BottomNavItem(Screen.Profile, "Profile", { Icon(Icons.Default.Person, contentDescription = "Profile") })
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatherFrameNavHost(
    sessionManager: SessionManager,
    googleAuthHelper: GoogleAuthHelper,
    cameraEngine: ManualCameraEngine,
    birdClassifier: BirdClassifier,
    gpsManager: GPSManager,
    imageProcessor: ImageProcessor,
    dngOutputDir: File,
    isLoggedIn: Boolean
) {
    val navController = rememberNavController()
    var isUserLoggedIn by remember { mutableStateOf(isLoggedIn) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route != Screen.Login.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                item.icon()
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = Color.Black,
                                unselectedIconColor = Color.Black.copy(alpha = 0.3f),
                                unselectedTextColor = Color.Black.copy(alpha = 0.3f),
                                indicatorColor = Color.Black.copy(alpha = 0.06f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (isUserLoggedIn) Screen.NewsFeed.route else Screen.Login.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    sessionManager = sessionManager,
                    onLoginSuccess = {
                        isUserLoggedIn = true
                        navController.navigate(Screen.NewsFeed.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Camera.route) {
                CameraScreen(
                    cameraEngine = cameraEngine,
                    birdClassifier = birdClassifier,
                    gpsManager = gpsManager,
                    imageProcessor = imageProcessor,
                    dngOutputDir = dngOutputDir,
                    onNavigationToFeed = {
                        navController.navigate(Screen.NewsFeed.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.NewsFeed.route) {
                val captureFlow = remember {
                    FeatherFrameApp.instance.database
                        .birdCaptureDao()
                        .getAllCaptures()
                }
                val captures by captureFlow.collectAsState(initial = emptyList())

                NewsFeedScreen(
                    captures = captures,
                    photographerName = sessionManager.getFullName() ?: "Photographer",
                    onCameraClick = {
                        navController.navigate(Screen.Camera.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    sessionManager = sessionManager,
                    googleAuthHelper = googleAuthHelper,
                    onLogout = {
                        isUserLoggedIn = false
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
