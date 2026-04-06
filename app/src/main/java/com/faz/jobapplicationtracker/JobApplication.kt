package com.faz.jobapplicationtracker

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplication(
    existingJob: Job? = null,
    onDismiss: () -> Unit,
    onSave: (Job) -> Unit
) {

    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current

    var isSaving by remember { mutableStateOf(false) }

    var company by remember { mutableStateOf(existingJob?.company ?: "") }
    var role by remember { mutableStateOf(existingJob?.role ?: "") }
    var location by remember { mutableStateOf(existingJob?.location ?: "") }
    var salary by remember { mutableStateOf("${existingJob?.minSalary ?: ""}-${existingJob?.maxSalary ?: ""}") }
    var url by remember { mutableStateOf(existingJob?.url ?: "") }
    var notes by remember { mutableStateOf(existingJob?.notes ?: "") }
    var date by remember { mutableStateOf(existingJob?.date ?: "") }
    var status by remember { mutableStateOf(existingJob?.status ?: "Applied") }

    var expanded by remember { mutableStateOf(false) }

    val statusOptions = listOf(
        "Applied","Interview","Offer","Rejected","Cancelled","Unavailable"
    )

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = {
            Text(if (existingJob == null) "Add New Application" else "Edit Application")
        },

        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {

                OutlinedTextField(company, { company = it }, label = { Text("Company *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))

                OutlinedTextField(role, { role = it }, label = { Text("Position *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statusOptions.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    status = it
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = salary,
                        onValueChange = { salary = it },
                        label = { Text("Salary") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                DatePickerField(
                    date = date,
                    onDateSelected = { date = it },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(url, { url = it }, label = { Text("Job URL") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))

                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
        },

        confirmButton = {

            Button(
                enabled = !isSaving,
                onClick = {

                    if (isSaving) return@Button
                    if (company.isBlank() || role.isBlank()) return@Button

                    isSaving = true

                    val parts = salary.split("-")

                    val min = parts.getOrNull(0)
                        ?.replace("₹", "")?.replace("L", "")?.trim()
                        ?.toIntOrNull() ?: 0

                    val max = parts.getOrNull(1)
                        ?.replace("₹", "")?.replace("L", "")?.trim()
                        ?.toIntOrNull() ?: 0

                    val job = Job(
                        role = role,
                        company = company,
                        status = status,
                        date = date,
                        location = location,
                        minSalary = min,
                        maxSalary = max,
                        url = url,
                        notes = notes
                    )

                    if (userId != null) {

                        if (existingJob == null) {

                            val docRef = db.collection("users")
                                .document(userId)
                                .collection("jobs")
                                .document()

                            val jobWithId = job.copy(id = docRef.id)

                            docRef.set(jobWithId)
                                .addOnSuccessListener {

                                    JobNotificationManager.scheduleFollowUp(context, jobWithId)

                                    // ❌ REMOVED onSave() → prevents duplication
                                    onDismiss()
                                }
                                .addOnFailureListener {
                                    isSaving = false
                                }

                        } else {

                            val updatedJob = job.copy(id = existingJob.id)

                            db.collection("users")
                                .document(userId)
                                .collection("jobs")
                                .document(existingJob.id)
                                .set(updatedJob)
                                .addOnSuccessListener {

                                    JobNotificationManager.showInstantNotification(
                                        context,
                                        "Application Updated",
                                        "${updatedJob.company} - ${updatedJob.status}"
                                    )

                                    // ❌ REMOVED onSave()
                                    onDismiss()
                                }
                                .addOnFailureListener {
                                    isSaving = false
                                }
                        }
                    } else {
                        Log.e("AUTH", "User not logged in")
                        isSaving = false
                    }
                }
            ) {

                if (isSaving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("Save")
                }
            }
        },

        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    date: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {

        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },

            confirmButton = {
                TextButton(
                    onClick = {

                        val millis = datePickerState.selectedDateMillis

                        if (millis != null) {
                            val formatter = java.text.SimpleDateFormat("dd/MM/yyyy")
                            val formattedDate =
                                formatter.format(java.util.Date(millis))

                            onDateSelected(formattedDate)
                        }

                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },

            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }

        ) {
            DatePicker(state = datePickerState)
        }
    }

    OutlinedTextField(
        value = date,
        onValueChange = {},
        readOnly = true,
        label = { Text("Applied Date") },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Outlined.CalendarToday, null)
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = shape
    )
}