package com.faz.jobapplicationtracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {

    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.hsl(255f, 0.37f, 0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.hsl(255f, 0.37f, 0.85f))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {

            // Option 1: Standard padding (Recommended)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                    //.padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.offset(x = (-12).dp) // Optional: Pulls the back button slightly left to align the icon itself with the text below
                ) {
                    Icon(Icons.Outlined.ArrowBack, null)
                }
                Text(
                    text = if (isLogin) "Login" else "Create Account",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isLogin) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { error = it.message ?: "Error" }
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { error = it.message ?: "Error" }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isLogin) "Login" else "Sign Up")
            }

            TextButton(onClick = { isLogin = !isLogin }) {
                Text(if (isLogin) "Create account" else "Already have account?")
            }

            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}