package com.example.velo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.velo.ui.theme.VeloTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Step 1: Data Class for clean ride information
data class RideInfo(
    val app: String = "",
    val price: String = "N/A",
    val eta: String = "N/A"
)

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("359627819018-t7gkv59be1l7qp9v9vmdh2kf575u5e5l.apps.googleusercontent.com")
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            VeloTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F0F0F)) {
                    var user by remember { mutableStateOf(auth.currentUser) }

                    Crossfade(targetState = user, animationSpec = tween(1000)) { currentState ->
                        if (currentState == null) {
                            AnimatedSplashScreen {
                                val signInIntent = googleSignInClient.signInIntent
                                googleSignInLauncher.launch(signInIntent)
                            }
                        } else {
                            VeloHomeScreen()
                        }
                    }
                }
            }
        }

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        val intent = intent
                        finish()
                        startActivity(intent)
                    }
                }
            } catch (e: ApiException) {}
        }
    }
}

// Step 2: Improved Parsing Logic
fun parseRideJson(jsonString: String?): RideInfo {
    if (jsonString == null || !jsonString.contains("{")) return RideInfo(price = "0.0")

    return try {
        // This looks for "price", skips whatever is next, and grabs the text inside the next quotes
        val price = jsonString.substringAfter("\"price\"").substringAfter(":").substringAfter("\"").substringBefore("\"").trim()
        val eta = jsonString.substringAfter("\"eta\"").substringAfter(":").substringAfter("\"").substringBefore("\"").trim()
        val app = jsonString.substringAfter("\"app\"").substringAfter(":").substringAfter("\"").substringBefore("\"").trim()

        RideInfo(app, price, eta)
    } catch (e: Exception) {
        // Emergency Fallback: If the JSON is weird, just find the first number in the string
        val fallbackPrice = Regex("(\\d+)").find(jsonString ?: "")?.value ?: "0.0"
        RideInfo(price = fallbackPrice)
    }
}

fun extractPriceValue(result: String): Double {
    val info = parseRideJson(result)
    // Removes ₹, spaces, and commas so it doesn't crash during conversion to Double
    val cleanPrice = info.price.replace(Regex("[^0-9.]"), "")
    return cleanPrice.toDoubleOrNull() ?: Double.MAX_VALUE
}
@Composable
fun AnimatedSplashScreen(onSignInClick: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(300)
        startAnimation = true
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF00E676).copy(alpha = pulseAlpha), Color.Transparent),
                    radius = 1500f
                )
            )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(
                visible = startAnimation,
                enter = scaleIn(animationSpec = tween(800)) + fadeIn(animationSpec = tween(800))
            ) {
                Text(text = "Velo", fontSize = 80.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E676))
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 400))
            ) {
                Text(text = "Move With Ease", fontSize = 18.sp, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(80.dp))

            AnimatedVisibility(
                visible = startAnimation,
                enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(800, delayMillis = 800)) +
                        fadeIn(animationSpec = tween(800, delayMillis = 800))
            ) {
                Button(
                    onClick = onSignInClick,
                    modifier = Modifier.width(260.dp).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
                ) {
                    Text("Continue with Google", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

fun openUberWithDestination(context: Context, destination: String) {
    val encodedDestination = Uri.encode(destination)
    val uriString = "uber://?action=setPickup&dropoff[formatted_address]=$encodedDestination"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
    intent.setPackage("com.ubercab")
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://m.uber.com")))
    }
}

fun openOlaWithDestination(context: Context, destination: String) {
    val encodedDestination = Uri.encode(destination)
    val uriString = "olacabs://app/launch?drop=$encodedDestination"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
    intent.setPackage("com.olacabs.customer")
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.olacabs.com")))
    }
}

@Composable
fun VeloHomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val auth = FirebaseAuth.getInstance()
    val userName = auth.currentUser?.displayName?.split(" ")?.get(0) ?: "User"

    var destination by remember { mutableStateOf(TextFieldValue("")) }
    var uberImageUri by remember { mutableStateOf<Uri?>(null) }
    var olaImageUri by remember { mutableStateOf<Uri?>(null) }
    var uberResult by remember { mutableStateOf<String?>(null) }
    var olaResult by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var transportMode by remember { mutableStateOf("Cab") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            val limitedUris = uris.take(2)
            uberImageUri = limitedUris.getOrNull(0)
            olaImageUri = limitedUris.getOrNull(1)

            scope.launch {
                isAnalyzing = true
                if (uberImageUri != null) {
                    val base64 = ScreenshotUploader.convertUriToBase64(context, uberImageUri!!)
                    if (base64 != null) {
                        uberResult = ScreenshotUploader.callBackend(base64, transportMode)
                    }
                }
                if (olaImageUri != null) {
                    val base64 = ScreenshotUploader.convertUriToBase64(context, olaImageUri!!)
                    if (base64 != null) {
                        olaResult = ScreenshotUploader.callBackend(base64, transportMode)
                    }
                }
                isAnalyzing = false
            }
        }
    }

    val backgroundBrush = Brush.verticalGradient(colors = listOf(Color(0xFF1A1A1A), Color(0xFF0A0A0A)))

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(backgroundBrush))

        Box(
            modifier = Modifier.size(300.dp).offset(x = (-100).dp, y = (-100).dp)
                .background(Color(0xFF00E676).copy(alpha = 0.05f), RoundedCornerShape(150.dp))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp).verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            Text(text = "Velo", fontSize = 72.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp, color = Color(0xFF00E676))
            Text(text = "Move With Ease", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, color = Color.White.copy(alpha = 0.4f))
            Text(text = "Hi, $userName! 👋", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))

            Spacer(modifier = Modifier.height(80.dp))

            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                placeholder = { Text("Where to?", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E676)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedBorderColor = Color(0xFF00E676),
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color(0xFF00E676),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    if (destination.text.trim().isNotEmpty()) {
                        openUberWithDestination(context, destination.text.trim())
                        openOlaWithDestination(context, destination.text.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("Compare Prices", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(25.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TransportCard(
                    label = "Auto",
                    emoji = "🛺",
                    isSelected = transportMode == "Auto",
                    onClick = { transportMode = "Auto" },
                    modifier = Modifier.weight(1f)
                )
                TransportCard(
                    label = "Cab",
                    emoji = "🚗",
                    isSelected = transportMode == "Cab",
                    onClick = { transportMode = "Cab" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(25.dp))

            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                border = BorderStroke(2.dp, Color(0xFF00E676))
            ) {
                Text("Upload Screenshots (Uber & Ola)", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
            }

            if (uberImageUri != null || olaImageUri != null) {
                Spacer(modifier = Modifier.height(30.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    uberImageUri?.let { uri ->
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Column {
                                Image(painter = rememberAsyncImagePainter(uri), contentDescription = "Uber", modifier = Modifier.height(200.dp).fillMaxWidth().background(Color.Black))
                                Text("Uber", modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    olaImageUri?.let { uri ->
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Column {
                                Image(painter = rememberAsyncImagePainter(uri), contentDescription = "Ola", modifier = Modifier.height(200.dp).fillMaxWidth().background(Color.Black))
                                Text("Ola", modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (uberResult != null && olaResult != null) {
                    Spacer(modifier = Modifier.height(30.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(3.dp, Color(0xFF00E676))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Price Comparison", color = Color(0xFF00E676), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.CenterHorizontally))
                            Spacer(modifier = Modifier.height(20.dp))

                            // Step 4: Use new extraction logic for comparison
                            val uberPrice = uberResult?.let { extractPriceValue(it) } ?: Double.MAX_VALUE
                            val olaPrice = olaResult?.let { extractPriceValue(it) } ?: Double.MAX_VALUE
                            val isUberCheaper = uberPrice < olaPrice
                            val isOlaCheaper = olaPrice < uberPrice
                            val cheapestApp = if (isUberCheaper) "Uber" else if (isOlaCheaper) "Ola" else "Same"

                            RideOptionRow(
                                appName = "Uber",
                                rawResult = uberResult ?: "",
                                isCheapest = isUberCheaper,
                                onBook = { openUberWithDestination(context, destination.text.trim()) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            RideOptionRow(
                                appName = "Ola",
                                rawResult = olaResult ?: "",
                                isCheapest = isOlaCheaper,
                                onBook = { openOlaWithDestination(context, destination.text.trim()) }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Cheapest: $cheapestApp 🚀", color = Color(0xFF00E676), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }

        AnimatedVisibility(visible = isAnalyzing, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF00E676))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Velo AI is comparing prices...", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun TransportCard(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFF1E1E1E),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Text(
                text = label,
                color = if (isSelected) Color(0xFF00E676) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// Step 3: Beautifully Formatted Result Row
@Composable
fun RideOptionRow(appName: String, rawResult: String, isCheapest: Boolean, onBook: () -> Unit) {
    val ride = parseRideJson(rawResult)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCheapest) Color(0xFF003300) else Color(0xFF1E1E1E)
        ),
        border = if (isCheapest) BorderStroke(2.dp, Color(0xFF00E676)) else null,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(appName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${ride.price}",
                        color = Color(0xFF00E676),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "🕒 ${ride.eta}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }

            Button(
                onClick = onBook,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Book", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}