@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.furina

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.furina.ui.theme.FurinaTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FurinaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(this@MainActivity)
                }
            }
        }
    }
}
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(activity: ComponentActivity) {
    // Obtener la instancia de autenticación de Firebase
    val auth = FirebaseAuth.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Furina Mensajes") },
                navigationIcon = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.furi3),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Spacer(modifier = Modifier.height(50.dp))
                // Pasar la instancia de autenticación a la función
                GreetingWithTextFieldAndImage(activity, auth)
            }
        }
    }
}



@Composable
fun GreetingWithTextFieldAndImage(activity: ComponentActivity, auth: FirebaseAuth) {
    var textFieldValue by remember { mutableStateOf("") }
    var greetings by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    val db = FirebaseFirestore.getInstance()
    var username by remember { mutableStateOf("") }
    var isFirstMessageSent by remember { mutableStateOf(false) }

    // Animation states
    var showImage by remember { mutableStateOf(false) }
    var removeItemIndex by remember { mutableStateOf(-1) }

    fun removingItem(index: Int, userName: String) {
        removeItemIndex = index
        db.collection("messages").whereEqualTo("text", greetings[index].first).whereEqualTo("username", userName)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
                removeItemIndex = -1
            }
    }

    LaunchedEffect(key1 = true) {
        // Retrieve saved messages from Firebase Firestore
        db.collection("messages")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.w(TAG, "Listen failed", exception)
                    return@addSnapshotListener
                }

                snapshot?.documents?.mapNotNull { it.getString("text")?.let { message -> Pair(message, it.getString("username") ?: "") } }?.let { messages ->
                    greetings = messages
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (!isFirstMessageSent) {
            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nombre de Usuario") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        TextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            label = { Text("Mensaje") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (textFieldValue.isNotBlank() && username.isNotBlank()) {
                    db.collection("messages").add(
                        mapOf(
                            "text" to textFieldValue,
                            "username" to username
                        )
                    ).addOnSuccessListener {
                        textFieldValue = ""
                        if (!isFirstMessageSent) {
                            isFirstMessageSent = true
                        }
                    }.addOnFailureListener { e ->
                        Log.w(TAG, "Error adding document", e)
                    }
                }
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.End)
        ) {
            Text("Enviar")
        }

        Button(
            onClick = { showImage = !showImage },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(if (showImage) "Esconder Imagen" else "Ver Imagen")
        }
        AnimatedVisibility(visible = showImage) {
            Image(
                painter = painterResource(id = R.drawable.furi),
                contentDescription = "Image",
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp)
                    .animateContentSize()
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(greetings.size) { index ->
                val message = greetings[index].first
                val userName = greetings[index].second
                AnimatedVisibility(
                    visible = removeItemIndex != index,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    GreetingListItem(
                        message = message,
                        userName = userName,
                        onDelete = { removingItem(index, userName) }
                    )
                }
            }
        }
    }
}




@Composable
fun GreetingListItem(
    message: String,
    userName: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$message - User: $userName",
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                )
            }
        }
    }
}

@Composable
fun AnimatedVisibility(
    visible: Boolean,
    enter: EnterTransition = fadeIn() + slideInHorizontally(),
    exit: ExitTransition = fadeOut() + slideOutHorizontally(),
    content: @Composable () -> Unit
) {
    val transition = updateTransition(targetState = visible, label = "")
    val animAlpha by transition.animateFloat(
        label = "alphaTransition"
    ) { if (it) 1f else 0f }
    Box(
        modifier = Modifier
            .background(Color.Transparent)
            .alpha(animAlpha)
    ) {
        if (visible) {
            content()
        }
    }
}


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if the message contains a notification
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            // Handle the received notification here
            // For example, you can show a notification in the notification bar

            // Create a notification channel if running on Android Oreo or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID, // Unique ID for the notification channel
                    "Application Notifications", // Name of the notification channel
                    NotificationManager.IMPORTANCE_DEFAULT // Importance of the channel (can be IMPORTANCE_HIGH, IMPORTANCE_LOW, etc.)
                )


                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            // Create and show the notification
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Notification Title")
                .setContentText(it.body)
                .setSmallIcon(R.drawable.furi2)
                .setAutoCancel(true)

            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Manejar la actualización del token de registro aquí
        Log.d(TAG, "Refreshed token: $token")
        // Aquí deberías enviar el token al servidor si es necesario
        // Por ejemplo, puedes enviar el token a tu servidor para enviar notificaciones
        // específicas a este dispositivo
    }

    companion object {
        private const val TAG = "MyFirebaseMessagingService"
        private const val CHANNEL_ID = "channel_id"
        private const val NOTIFICATION_ID = 123 // Identificador único para la notificación
    }
}
