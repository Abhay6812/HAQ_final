package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.ui.MainHaqApp
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.HaqTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory Firebase App Check wiring with launch Intent before UI / ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            HaqTheme {
                HaqRootNavigation()
            }
        }
    }
}

@Composable
fun HaqRootNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ })
    } else {
        key(user.uid) {
            MainHaqApp(currentUserId = user.uid)
        }
    }
}
