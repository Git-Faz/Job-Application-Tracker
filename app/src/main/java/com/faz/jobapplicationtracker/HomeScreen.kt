package com.faz.jobapplicationtracker

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Link
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.core.net.toUri

data class Job(
    val id: String = "",
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
    var showAuthMessage by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    val statusOptions = listOf(
        "All Status","Applied","Interview","Offer","Rejected","Cancelled","Unavailable"
    )

    // REAL-TIME LISTENER
    DisposableEffect(userId) {

        var listener: ListenerRegistration? = null

        if (userId != null) {
            listener = db.collection("users")
                .document(userId)
                .collection("jobs")
                .addSnapshotListener { snapshot, _ ->

                    if (snapshot != null) {
                        jobs = snapshot.documents.mapNotNull { doc ->
                            try {
                                Job(
                                    id = doc.id,
                                    role = doc.getString("role") ?: "",
                                    company = doc.getString("company") ?: "",
                                    status = doc.getString("status") ?: "",
                                    date = doc.getString("date") ?: "",
                                    location = doc.getString("location") ?: "",
                                    minSalary = doc.getLong("minSalary")?.toInt() ?: 0,
                                    maxSalary = doc.getLong("maxSalary")?.toInt() ?: 0,
                                    url = doc.getString("url") ?: "",
                                    notes = doc.getString("notes") ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }
        }

        onDispose { listener?.remove() }
    }

    val filteredJobs = jobs.filter { job ->

    val matchesStatus =
        selectedStatus == "All Status" || job.status == selectedStatus

    val matchesSearch =
        search.isBlank() ||
        job.company.contains(search, ignoreCase = true) ||
        job.role.contains(search, ignoreCase = true)

    matchesStatus && matchesSearch
}

    LaunchedEffect(showAuthMessage) {
        if (showAuthMessage) {
            snackbarHostState.showSnackbar("Please login to add applications")
            kotlinx.coroutines.delay(1200)
            showAuthMessage = false
            currentScreen = "auth"
        }
    }

    when (currentScreen) {

        "home" -> {

            Scaffold(
                containerColor = Color.hsl(255f, 0.37f, 0.87f),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center) // centered
                    )
                }
            ) { padding ->

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .padding(padding),
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
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user == null) {
                                showAuthMessage = true
                            } else {
                                editingJob = null
                                showDialog = true
                            }
                        }
                    }

                    items(filteredJobs) { job ->
                        JobCard(
                            job = job,
                            onEdit = {
                                editingJob = job
                                showDialog = true
                            },
                            onDelete = {
                                if (userId != null) {
                                    db.collection("users")
                                        .document(userId)
                                        .collection("jobs")
                                        .document(job.id)
                                        .delete()
                                }
                            }
                        )
                    }
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

    // ADD / EDIT DIALOG
    if (showDialog) {
        JobApplication(
            existingJob = editingJob,
            onDismiss = {
                showDialog = false
                editingJob = null
            },
            onSave = { updatedJob ->

                if (userId != null) {

                    if (editingJob == null) {

                        val docRef = db.collection("users")
                            .document(userId)
                            .collection("jobs")
                            .document()

                        val jobWithId = updatedJob.copy(id = docRef.id)
                        docRef.set(jobWithId)

                    } else {

                        db.collection("users")
                            .document(userId)
                            .collection("jobs")
                            .document(editingJob!!.id)
                            .set(updatedJob.copy(id = editingJob!!.id))
                    }
                }

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
            Text("Keep track of your job applications", color = Color.DarkGray)
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
            StatCard("Total", "$total", Icons.Outlined.WorkOutline, Modifier.weight(1f))
            StatCard("Applied", "$applied", Icons.Outlined.CalendarToday, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Interviews", "$interviews", Icons.Outlined.CheckCircle, Modifier.weight(1f))
            StatCard("Offers", "$offers", Icons.Outlined.StarOutline, Modifier.weight(1f))
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.hsl(255f, 0.37f, 0.83f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = Color.DarkGray)
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

@SuppressLint("UseKtx")
@Composable
fun JobCard(
    job: Job,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.hsl(255f, 0.37f, 0.83f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top section (Role + Company ONLY)
            Text(job.role, fontSize = 20.sp)
            Text(job.company, color = Color.DarkGray)

            Spacer(modifier = Modifier.height(8.dp))

            // Status + Actions in SAME ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(job.status) }
                )

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                    }
                }
            }

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

            val context = LocalContext.current

            if (job.url.isNotBlank()) {
                val formattedUrl = if (
                    job.url.startsWith("http://") || job.url.startsWith("https://")
                ) {
                    job.url
                } else {
                    "https://${job.url}"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, formattedUrl.toUri())
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("URL", "Invalid URL: $formattedUrl", e)
                            }
                        }
                ) {
                    Icon(Icons.Outlined.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Job",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

