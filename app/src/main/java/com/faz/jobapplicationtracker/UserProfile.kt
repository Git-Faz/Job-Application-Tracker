package com.faz.jobapplicationtracker

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {

    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            processCsvFile(context, it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.hsl(255f, 0.37f, 0.88f))
            .padding(20.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
            }
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.hsl(255f, 0.37f, 0.85f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Email", color = Color.Gray)
                Text(user?.email ?: "Unknown")
            }
        }

        Spacer(Modifier.height(24.dp))

        // IMPORT SECTION
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.hsl(255f, 0.37f, 0.85f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text("Bulk Import Applications", style = MaterialTheme.typography.titleMedium)

                Text(
                    "Required CSV columns (in the same order as below):\nRole, Company, Status, Date, Location, MinSalary, MaxSalary, URL, Notes\n",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = { filePickerLauncher.launch("text/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Import CSV")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // PASSWORD SECTION
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.hsl(255f, 0.37f, 0.85f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text("Change Password", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = {
                            passwordVisible = !passwordVisible
                        }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Outlined.Visibility
                                else Icons.Outlined.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation()
                )

                Button(
                    onClick = {

                        if (newPassword != confirmPassword) {
                            message = "Passwords do not match"
                            return@Button
                        }

                        if (newPassword.length < 6) {
                            message = "Password must be at least 6 characters"
                            return@Button
                        }

                        user?.updatePassword(newPassword)
                            ?.addOnSuccessListener {
                                message = "Password updated successfully"
                                newPassword = ""
                                confirmPassword = ""
                            }
                            ?.addOnFailureListener {
                                message = it.message ?: "Error updating password"
                            }

                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Password")
                }

                if (message.isNotEmpty()) {
                    Text(message, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Logout")
        }
    }
}

fun processCsvFile(context: Context, uri: Uri) {

    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val inputStream = context.contentResolver.openInputStream(uri) ?: return
    val reader = inputStream.bufferedReader()

    val header = reader.readLine()?.split(",")?.map { it.trim() } ?: run {
        Toast.makeText(context, "Empty CSV file", Toast.LENGTH_LONG).show()
        return
    }

    val roleIndex = header.indexOf("Role")
    val companyIndex = header.indexOf("Company")

    if (roleIndex == -1 || companyIndex == -1) {
        Toast.makeText(context, "Invalid CSV format. Required columns missing.", Toast.LENGTH_LONG).show()
        return
    }

    val statusIndex = header.indexOf("Status")
    val dateIndex = header.indexOf("Date")
    val locationIndex = header.indexOf("Location")
    val minIndex = header.indexOf("MinSalary")
    val maxIndex = header.indexOf("MaxSalary")
    val urlIndex = header.indexOf("URL")
    val notesIndex = header.indexOf("Notes")

    var successCount = 0

    reader.forEachLine { line ->

        val parts = line.split(",")

        try {
            val job = Job(
                id = "",
                role = parts.getOrNull(roleIndex)?.trim() ?: "",
                company = parts.getOrNull(companyIndex)?.trim() ?: "",
                status = parts.getOrNull(statusIndex)?.trim() ?: "Applied",
                date = parts.getOrNull(dateIndex)?.trim() ?: "",
                location = parts.getOrNull(locationIndex)?.trim() ?: "",
                minSalary = parts.getOrNull(minIndex)?.toIntOrNull() ?: 0,
                maxSalary = parts.getOrNull(maxIndex)?.toIntOrNull() ?: 0,
                url = parts.getOrNull(urlIndex)?.trim() ?: "",
                notes = parts.getOrNull(notesIndex)?.trim() ?: ""
            )

            val docRef = db.collection("users")
                .document(userId)
                .collection("jobs")
                .document()

            docRef.set(job.copy(id = docRef.id))
            successCount++

        } catch (_: Exception) {
        }
    }

    reader.close()

    Toast.makeText(context, "Imported $successCount jobs", Toast.LENGTH_LONG).show()
}