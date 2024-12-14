package com.example.notificationcreator

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.notificationcreator.ui.theme.NotificationCreatorTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    companion object {
        const val CHANNEL_ID = "default_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            NotificationCreatorApp()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kanał domyślny"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun IconOption(@DrawableRes icon: Int, selectedIcon: Int, onClick: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .selectable(
                selected = icon == selectedIcon,
                onClick = { onClick(icon) }
            )
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun RadioButtonOption(option: String, selectedOption: String, onClick: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        RadioButton(
            selected = option == selectedOption,
            onClick = { onClick(option) }
        )
        Text(option)
    }
}

@Composable
fun NotificationCreatorApp() {
    NotificationCreatorTheme {
        val context = LocalContext.current
        var permissionGranted by remember { mutableStateOf(checkNotificationPermission(context)) }
        var title by remember { mutableStateOf(TextFieldValue("")) }
        var description by remember { mutableStateOf(TextFieldValue("")) }
        var expandedDescription by remember { mutableStateOf(TextFieldValue("")) }
        var selectedIcon by remember { mutableStateOf(R.drawable.clock1) }
        var notificationStyle by remember { mutableStateOf("Default") }

        // Zmienne dla godziny i minut alarmu
        var alarmHour by remember { mutableStateOf(7) }
        var alarmMinute by remember { mutableStateOf(0) }
        var showTimePicker by remember { mutableStateOf(false) }

        val requestPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionGranted = isGranted
            if (!isGranted) {
                Toast.makeText(
                    context,
                    "Brak uprawnień do powiadomień.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // Wyświetlanie TimePickerDialog
        if (showTimePicker) {
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    alarmHour = hourOfDay
                    alarmMinute = minute
                    showTimePicker = false
                },
                alarmHour,
                alarmMinute,
                true // 24-godzinny format
            ).show()
        }

        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (permissionGranted) {
                    // Pola tekstowe dla tytułu i opisów
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tytuł") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Opis") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = expandedDescription,
                        onValueChange = { expandedDescription = it },
                        label = { Text("Poszerzony opis") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Sekcja wyboru godziny alarmu
                    Text("Ustaw godzinę alarmu:")
                    Button(onClick = { showTimePicker = true }) {
                        Text("Wybierz godzinę")
                    }
                    Text("Wybrana godzina: $alarmHour:$alarmMinute")

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sekcja ikon i stylów
                    Text("Wybierz ikonę:")
                    Row {
                        IconOption(R.drawable.clock1, selectedIcon) { selectedIcon = it }
                        IconOption(R.drawable.clock800, selectedIcon) { selectedIcon = it }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Wybierz styl:")
                    Row {
                        RadioButtonOption("Default", notificationStyle) { notificationStyle = it }
                        RadioButtonOption("BigText", notificationStyle) { notificationStyle = it }
                        RadioButtonOption("BigPicture", notificationStyle) { notificationStyle = it }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Przycisk wysyłania powiadomienia
                    Button(onClick = {
                        sendNotification(
                            context = context,
                            title = title.text,
                            message = description.text,
                            expandedMessage = expandedDescription.text,
                            selectedIcon = selectedIcon,
                            style = notificationStyle,
                            alarmHour = alarmHour,
                            alarmMinute = alarmMinute
                        )
                    }) {
                        Text("Wyślij Powiadomienie")
                    }
                } else {
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }) {
                        Text("Poproś o uprawnienia")
                    }
                }
            }
        }
    }
}


@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
private fun sendNotification(
    context: Context,
    title: String,
    message: String,
    expandedMessage: String,
    @DrawableRes selectedIcon: Int,
    style: String,
    alarmHour: Int,
    alarmMinute: Int
) {
    val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_MESSAGE, "Budzik")
        putExtra(AlarmClock.EXTRA_HOUR, alarmHour)
        putExtra(AlarmClock.EXTRA_MINUTES, alarmMinute)
    }
    val alarmPendingIntent = PendingIntent.getActivity(
        context,
        0,
        alarmIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        val builder = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(selectedIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.clock1,
                "Ustaw nowy Alarm",
                alarmPendingIntent
            )

        when (style) {
            "BigText" -> builder.setStyle(NotificationCompat.BigTextStyle().bigText(expandedMessage))
            "BigPicture" -> {
                val bitmap = BitmapFactory.decodeResource(context.resources, selectedIcon)
                builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
            }
        }

        NotificationManagerCompat.from(context).notify(MainActivity.NOTIFICATION_ID, builder.build())
    } catch (e: Exception) {
        Toast.makeText(context, "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}


private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}



