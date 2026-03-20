package com.faz.jobapplicationtracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        "Applied","Interview","Offer","Cancelled","Unavailable"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingJob == null) "Add New Application" else "Edit Application")
        },

        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Position *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

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

                DatePickerField(date = date, onDateSelected = { date = it })

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = salary,
                        onValueChange = { salary = it },
                        label = { Text("Salary") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(url, { url = it }, label = { Text("Job URL") })
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
            }
        },

        confirmButton = {
            Button(onClick = {

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

                // 🔥 SAVE TO FIRESTORE
                if (userId != null) {
                    db.collection("users")
                        .document(userId)
                        .collection("jobs")
                        .add(job)
                        .addOnSuccessListener {
                            onSave(job)
                        }
                }

            }) {
                Text("Save")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(date: String, onDateSelected: (String) -> Unit) {

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

                            val formatter =
                                java.text.SimpleDateFormat("dd/MM/yyyy")

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
            IconButton(
                onClick = { showDatePicker = true }
            ) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = "Select Date")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}