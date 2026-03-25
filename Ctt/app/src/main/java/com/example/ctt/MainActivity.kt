package com.example.ctt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import com.example.ctt.User
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- Data Models ---
data class Song(
    val rank: Int,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val trend: String, // "rising", "falling", "steady"
    val country: String?,
    val videoUrl: String? = null
)

data class User(val email: String, val name: String)

enum class Screen(val title: String, val icon: ImageVector) {
    Feed("Trends", Icons.Default.TrendingUp),
    Dashboard("Profile", Icons.Default.Person)
}

data class ApiSongItem(
    val rank: Int? = null,
    val title: String? = null,
    val artist: String? = null,
    val trend: String? = null,
    val country: String? = null,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrlSnake: String? = null,
    val thumbnail: String? = null,
    val image: String? = null,
    val cover: String? = null,
    @SerializedName("videoUrl") val videoUrl: String? = null,
    @SerializedName("video_url") val videoUrlSnake: String? = null,
    val url: String? = null
)

// --- API Service ---
interface ScraperApi {
    @GET("api/songs")
    suspend fun getSongsJson(): ResponseBody

    @GET("songs/latest")
    suspend fun getLatestSongs(): ResponseBody

    @POST("fetch-songs")
    suspend fun triggerFetchSongs()

    // New endpoint to download the latest APK from the backend
    @GET("download/ctt.apk")
    @Streaming
    suspend fun downloadApk(): Response<ResponseBody>
}

object ApiClient {
    val scraperApi: ScraperApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ScraperApi::class.java)
    }

    /**
     * Helper to download the APK and write it to the app's cache directory.
     * Returns the absolute path of the saved file, or null on failure.
     */
    suspend fun downloadApkFile(cacheDir: File): String? {
        return try {
            val response = scraperApi.downloadApk()
            val body = response.body()
            if (!response.isSuccessful || body == null) return null

            val apkFile = File(cacheDir, "ctt_latest.apk")
            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            apkFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// --- ViewModel ---
class MusicViewModel : ViewModel() {
    private val gson = Gson()
    private val countryNames = mapOf(
        "AR" to "Argentina",
        "AU" to "Australia",
        "AT" to "Austria",
        "BH" to "Bahrain",
        "BD" to "Bangladesh",
        "BY" to "Belarus",
        "BE" to "Belgium",
        "BR" to "Brazil",
        "BG" to "Bulgaria",
        "KH" to "Cambodia",
        "CA" to "Canada",
        "CL" to "Chile",
        "CO" to "Colombia",
        "HR" to "Croatia",
        "CZ" to "Czechia",
        "DK" to "Denmark",
        "EG" to "Egypt",
        "EE" to "Estonia",
        "FI" to "Finland",
        "FR" to "France",
        "DE" to "Germany",
        "GR" to "Greece",
        "HU" to "Hungary",
        "IS" to "Iceland",
        "ID" to "Indonesia",
        "IQ" to "Iraq",
        "IE" to "Ireland",
        "IL" to "Israel",
        "IT" to "Italy",
        "JP" to "Japan",
        "JO" to "Jordan",
        "KZ" to "Kazakhstan",
        "KW" to "Kuwait",
        "LV" to "Latvia",
        "LB" to "Lebanon",
        "LT" to "Lithuania",
        "LU" to "Luxembourg",
        "MO" to "Macao",
        "MY" to "Malaysia",
        "MX" to "Mexico",
        "MA" to "Morocco",
        "MM" to "Myanmar",
        "NL" to "Netherlands",
        "NZ" to "New Zealand",
        "NG" to "Nigeria",
        "NO" to "Norway",
        "OM" to "Oman",
        "PK" to "Pakistan",
        "PE" to "Peru",
        "PH" to "Philippines",
        "PL" to "Poland",
        "PT" to "Portugal",
        "QA" to "Qatar",
        "RO" to "Romania",
        "RU" to "Russia",
        "SA" to "Saudi Arabia",
        "SG" to "Singapore",
        "SK" to "Slovakia",
        "ZA" to "South Africa",
        "KR" to "South Korea",
        "ES" to "Spain",
        "SE" to "Sweden",
        "CH" to "Switzerland",
        "TW" to "Taiwan",
        "TH" to "Thailand",
        "TR" to "Turkey",
        "UA" to "Ukraine",
        "AE" to "United Arab Emirates",
        "GB" to "United Kingdom",
        "UK" to "United Kingdom",
        "US" to "United States",
        "UZ" to "Uzbekistan",
        "VN" to "Vietnam"
    )

    private fun getFullCountryName(code: String?): String? {
        val upperCode = code?.trim()?.uppercase() ?: return null
        return countryNames[upperCode] ?: code
    }

    private fun toProxyThumbnailUrl(rawUrl: String?): String {
        val clean = rawUrl?.trim().orEmpty()
        if (clean.isEmpty()) return ""
        val encoded = URLEncoder.encode(clean, StandardCharsets.UTF_8.toString())
        return "${BuildConfig.BASE_URL}api/thumbnail?url=$encoded"
    }

    var countries by mutableStateOf<List<String>>(emptyList())
    var selectedCountry by mutableStateOf<String?>(null)
    private var allSongs by mutableStateOf<List<Song>>(emptyList())
    var songs by mutableStateOf<List<Song>>(emptyList())
    var isLoadingCountries by mutableStateOf(false)
    var isLoadingSongs by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun loadInitialData() {
        viewModelScope.launch {
            isLoadingCountries = true
            try {
                fetchSongsAndCountries()
            } catch (e: Exception) {
                error = e.message
                countries = emptyList()
                allSongs = emptyList()
                songs = emptyList()
            }
            isLoadingCountries = false
        }
    }

    fun onCountrySelected(country: String) {
        selectedCountry = country
        applyCountryFilter()
    }

    fun refreshCurrentCountrySongs() {
        viewModelScope.launch {
            isLoadingSongs = true
            error = null
            try {
                ApiClient.scraperApi.triggerFetchSongs()
                fetchSongsAndCountries()
            } catch (e: Exception) {
                error = e.message
            }
            isLoadingSongs = false
        }
    }

    /**
     * Download the latest APK from the backend and store it in the provided cache directory.
     * The returned path can be used to launch an install intent.
     */
    fun downloadLatestApk(cacheDir: File, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val path = ApiClient.downloadApkFile(cacheDir)
            onResult(path)
        }
    }

    private suspend fun fetchSongsAndCountries() {
        isLoadingSongs = true
        error = null
        try {
            val rawJson = runCatching { ApiClient.scraperApi.getSongsJson().string() }
                .getOrElse { ApiClient.scraperApi.getLatestSongs().string() }
            val parsedSongs = parseSongs(rawJson)
            allSongs = parsedSongs
            countries = parsedSongs.mapNotNull { getFullCountryName(it.country) }
                .distinct()
                .sorted()
            if (countries.isNotEmpty()) {
                if (selectedCountry !in countries) {
                    selectedCountry = if ("United States" in countries) "United States" else countries.first()
                }
            } else {
                selectedCountry = null
            }
            applyCountryFilter()
        } catch (e: Exception) {
            error = e.message
            allSongs = emptyList()
            songs = emptyList()
            countries = emptyList()
            selectedCountry = null
        } finally {
            isLoadingSongs = false
        }
    }

    private fun applyCountryFilter() {
        songs = if (selectedCountry == null) {
            allSongs
        } else {
            allSongs.filter { it.country == selectedCountry }
        }
    }

    private fun parseSongs(json: String): List<Song> {
        val root = JsonParser.parseString(json)
        val songsArray = extractSongsArray(root) ?: return emptyList()
        return songsArray.mapIndexedNotNull { index, element ->
            val item = runCatching { gson.fromJson(element, ApiSongItem::class.java) }.getOrNull()
                ?: return@mapIndexedNotNull null
            Song(
                rank = item.rank ?: (index + 1),
                title = item.title ?: "Unknown title",
                artist = item.artist ?: "Unknown artist",
                thumbnailUrl = toProxyThumbnailUrl(
                    item.thumbnailUrl
                        ?: item.thumbnailUrlSnake
                        ?: item.thumbnail
                        ?: item.image
                        ?: item.cover
                ),
                trend = item.trend ?: "steady",
                country = getFullCountryName(item.country),
                videoUrl = item.videoUrl ?: item.videoUrlSnake ?: item.url
            )
        }
    }

    private fun extractSongsArray(root: JsonElement): List<JsonElement>? {
        return when {
            root.isJsonArray -> root.asJsonArray.toList()
            root.isJsonObject -> {
                val obj = root.asJsonObject
                when {
                    obj.has("songs") && obj.get("songs").isJsonArray -> obj.getAsJsonArray("songs").toList()
                    obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data").toList()
                    else -> null
                }
            }
            else -> null
        }
    }
}

class AuthViewModel : ViewModel() {
    var user by mutableStateOf<User?>(null)
    var isSigningUp by mutableStateOf(false)
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    
    fun performLogin() {
        if (email.isNotBlank() && password.length >= 6) {
            user = User(email, email.split("@").first().replaceFirstChar { it.uppercase() })
        }
    }
    
    fun performSignUp() {
        if (email.isNotBlank() && password.length >= 6) {
            user = User(email, email.split("@").first().replaceFirstChar { it.uppercase() })
        }
    }
    
    fun logout() {
        user = null
        email = ""
        password = ""
    }
}

// --- UI Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassMusicApp(viewModel: MusicViewModel = viewModel(), authViewModel: AuthViewModel = viewModel()) {
    var regionExpanded by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(Screen.Feed) }
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF14141C),
                drawerContentColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(48.dp))
                
                // Drawer Header / User Info
                if (authViewModel.user != null) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF00FF88)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(authViewModel.user!!.name.take(1), style = MaterialTheme.typography.headlineMedium, color = Color.Black)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(authViewModel.user!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(authViewModel.user!!.email, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
                    }
                } else {
                    Text(
                        "Trends",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))

                Screen.values().forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF00FF88).copy(alpha = 0.1f),
                            selectedIconColor = Color(0xFF00FF88),
                            selectedTextColor = Color(0xFF00FF88),
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F17))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }

                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FF88)) // Live indicator
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (currentScreen == Screen.Feed) "Live Now" else "Account",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF00FF88),
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                if (currentScreen == Screen.Feed) "Trends" else "Dashboard",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Black,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.White, Color(0xFF00F2EA))
                                    ),
                                    letterSpacing = (-1).sp
                                )
                            )
                        }

                        if (currentScreen == Screen.Feed) {
                            // Region Selector
                            Box {
                                Surface(
                                    onClick = {
                                        if (viewModel.countries.isNotEmpty()) {
                                            regionExpanded = !regionExpanded
                                        }
                                    },
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = viewModel.selectedCountry ?: "Region",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 120.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = regionExpanded,
                                    onDismissRequest = { regionExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1A1A24))
                                ) {
                                    viewModel.countries.forEach { country ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    country,
                                                    color = if (country == viewModel.selectedCountry) Color(0xFF00FF88) else Color.White
                                                )
                                            },
                                            onClick = {
                                                viewModel.onCountrySelected(country)
                                                regionExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
        floatingActionButton = {
            if (currentScreen == Screen.Feed) {
                // Two FABs: one for refresh, one for APK download
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.refreshCurrentCountrySongs() },
                        containerColor = Color(0xFF6200EE),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    FloatingActionButton(
                        onClick = {
                            viewModel.downloadLatestApk(context.cacheDir) { apkPath ->
                                apkPath?.let { path ->
                                    val apkFile = File(path)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        apkFile
                                    )
                                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/vnd.android.package-archive")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(installIntent)
                                }
                            }
                        },
                        containerColor = Color(0xFF03DAC5),
                        contentColor = Color.Black,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download APK")
                    }
                }
            }
        },
        containerColor = Color(0xFF0F0F17) // Dark background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentScreen == Screen.Feed) {
                // Background Vibrant Accents
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 100.dp, y = (-50).dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF6200EE).copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (viewModel.error != null) {
                        item {
                            Text(
                                text = "Error: ${viewModel.error}",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (viewModel.isLoadingCountries || viewModel.isLoadingSongs) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }

                    items(viewModel.songs) { song ->
                        MusicCard(song)
                    }
                }
            } else {
                DashboardScreen(authViewModel)
            }
        }
    }
}
}

@Composable
fun DashboardScreen(authViewModel: AuthViewModel) {
    val user = authViewModel.user
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (user == null) {
            AuthCard(authViewModel)
        } else {
            ProfileCard(authViewModel)
        }
    }
}

@Composable
fun AuthCard(authViewModel: AuthViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                if (authViewModel.isSigningUp) "Sign Up" else "Log In",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            OutlinedTextField(
                value = authViewModel.email,
                onValueChange = { authViewModel.email = it },
                label = { Text("Email", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF88),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = authViewModel.password,
                onValueChange = { authViewModel.password = it },
                label = { Text("Password", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF88),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Button(
                onClick = { if (authViewModel.isSigningUp) authViewModel.performSignUp() else authViewModel.performLogin() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
            ) {
                Text(if (authViewModel.isSigningUp) "Create Account" else "Sign In", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = { authViewModel.isSigningUp = !authViewModel.isSigningUp },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (authViewModel.isSigningUp) "Already have an account? Log In" else "Don't have an account? Sign Up",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ProfileCard(authViewModel: AuthViewModel) {
    val user = authViewModel.user!!
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF00F2EA), Color(0xFF00FF88))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                user.name.take(1),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = Color.Black
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(user.name, style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text(user.email, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.6f))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Saved", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
                    Text("12", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Views", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
                    Text("4.2k", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rank", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
                    Text("#1", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF00FF88), fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = { authViewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.Red)
        ) {
            Text("Logout", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MusicCard(song: Song) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        onClick = {
            song.videoUrl?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Indicator
            Text(
                text = "#${song.rank}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White.copy(alpha = 0.8f)
                ),
                modifier = Modifier.width(50.dp)
            )

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }

            // Trend & Action
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val trendIcon = when (song.trend) {
                    "rising" -> "↗"
                    "falling" -> "↘"
                    else -> "→"
                }
                val trendColor = when (song.trend) {
                    "rising" -> Color(0xFF00FF88)
                    "falling" -> Color(0xFFFF3366)
                    else -> Color.White.copy(alpha = 0.5f)
                }

                Text(
                    text = trendIcon,
                    color = trendColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                if (song.videoUrl != null) {
                    Text(
                        text = "VIEW",
                        color = Color(0xFF00F2EA), // TikTok cyan-ish
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                GlassMusicApp()
            }
        }
    }
}
