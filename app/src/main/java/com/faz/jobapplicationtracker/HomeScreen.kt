package com.faz.jobapplicationtracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth

data class Job(
    val role: String,
    val company: String,
    val status: String,
    val date: String,
    val location: String,
    val minSalary: Int,
    val maxSalary: Int,
    val url: String,
    val notes: String
)

@Composable
fun HomeScreen() {

    var currentScreen by remember { mutableStateOf("home") }

    var search by remember { mutableStateOf("") }
    var jobs by remember { mutableStateOf(listOf<Job>()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingJob by remember { mutableStateOf<Job?>(null) }
    var selectedStatus by remember { mutableStateOf("All Status") }

    val statusOptions = listOf(
        "All Status","Applied","Interview","Offer","Cancelled","Unavailable"
    )

    val filteredJobs = if (selectedStatus == "All Status") jobs
    else jobs.filter { it.status == selectedStatus }

    when (currentScreen) {

        "home" -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    Header(
                        onProfileClick = {
                            val user = FirebaseAuth.getInstance().currentUser
                            currentScreen = if (user == null) "auth" else "profile"
                        }
                    )
                }

                item { StatsSection(filteredJobs) }

                item {
                    SearchSection(
                        search = search,
                        onChange = { search = it },
                        selectedStatus = selectedStatus,
                        statusOptions = statusOptions,
                        onStatusSelected = { selectedStatus = it }
                    )
                }

                item {
                    AddApplicationButton {
                        editingJob = null
                        showDialog = true
                    }
                }

                items(filteredJobs) { job ->
                    JobCard(
                        job = job,
                        onEdit = {
                            editingJob = job
                            showDialog = true
                        },
                        onDelete = { jobs = jobs - job }
                    )
                }
            }
        }

        "auth" -> {
            AuthScreen(
                onBack = { currentScreen = "home" },
                onSuccess = { currentScreen = "profile" }
            )
        }

        "profile" -> {
            UserProfileScreen(
                onBack = { currentScreen = "home" },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    currentScreen = "home"
                }
            )
        }
    }

    if (showDialog) {
        JobApplication(
            existingJob = editingJob,
            onDismiss = {
                showDialog = false
                editingJob = null
            },
            onSave = { updatedJob ->
                jobs = if (editingJob == null) jobs + updatedJob
                else jobs.map { if (it == editingJob) updatedJob else it }

                showDialog = false
                editingJob = null
            }
        )
    }
}

@Composable

fun Header(onProfileClick: () -> Unit) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {
            Text("JOBBIT", fontSize = 30.sp)
            Text("Keep track of your job applications", color = Color.Gray)
        }

        IconButton(onClick = onProfileClick) {
            Icon(
                Icons.Outlined.Person,
                null,
                modifier = Modifier
                    .background(Color.hsl(255f, 0.36f, 0.83f))
                    .padding(10.dp)
            )
        }
    }
}

@Composable
fun StatsSection(jobs: List<Job>) {

    val total = jobs.size
    val applied = jobs.count { it.status == "Applied" }
    val interviews = jobs.count { it.status == "Interview" }
    val offers = jobs.count { it.status == "Offer" }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Total Applications", "$total", Icons.Outlined.Work, Modifier.weight(1f))
            StatCard("Applied", "$applied", Icons.Outlined.CalendarToday, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Interviews", "$interviews", Icons.Outlined.CheckCircle, Modifier.weight(1f))
            StatCard("Offers", "$offers", Icons.Outlined.Star, Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = Color.Gray)
                Text(value, fontSize = 26.sp)
            }
            Icon(icon, null)
        }
    }
}

@Composable
fun SearchSection( search: String, onChange: (String) -> Unit, selectedStatus: String,
    statusOptions: List<String>, onStatusSelected: (String) -> Unit) {

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = onChange,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("Search by company or position...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        StatusFilter(
            selectedStatus = selectedStatus,
            statusOptions = statusOptions,
            onStatusSelected = onStatusSelected
        )
    }
}

@Composable
fun AddApplicationButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {

        Icon(Icons.Outlined.Add, null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add Application")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFilter(
    selectedStatus: String,
    statusOptions: List<String>,
    onStatusSelected: (String) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {

        OutlinedTextField(
            value = selectedStatus,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text("Status") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            statusOptions.forEach { status ->

                DropdownMenuItem(
                    text = { Text(status) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )

            }

        }
    }
}

@Composable
fun JobCard(
    job: Job,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(job.role, fontSize = 20.sp)
                    Text(job.company, color = Color.Gray)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, null)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, null)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            AssistChip(
                onClick = {},
                label = { Text(job.status) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarToday, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Applied: ${job.date}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(job.location)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CurrencyRupee, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("${job.minSalary} - ${job.maxSalary} LPA")
            }
        }
    }
}

