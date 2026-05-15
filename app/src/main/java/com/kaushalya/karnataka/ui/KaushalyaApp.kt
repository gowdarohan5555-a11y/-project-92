package com.kaushalya.karnataka.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.ui.res.painterResource
import com.kaushalya.karnataka.R
import com.kaushalya.karnataka.model.AppState
import com.kaushalya.karnataka.model.AppUser
import com.kaushalya.karnataka.model.PortfolioPhoto
import com.kaushalya.karnataka.model.UserRole
import com.kaushalya.karnataka.model.WorkerService
import com.kaushalya.karnataka.ui.theme.AppPrimary
import com.kaushalya.karnataka.ui.theme.Zinc
import com.kaushalya.karnataka.ui.theme.ZincLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val trades = listOf("Electrician", "Plumber", "Carpenter", "Painter", "Mason")
private val serviceCategories = listOf("Maintenance", "Installation", "Carpentry", "Painting", "Masonry")

@Composable
fun KaushalyaApp(viewModel: KaushalyaViewModel) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.currentUser) {
        state.currentUser?.let { user ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let { viewModel.updateFcmToken(it) }
                }
            }
        }
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Splash) { SplashScreen(state, navController) }
            composable(Routes.Login) { LoginScreen(state, viewModel, navController) }
            composable(Routes.Register) { RegisterScreen(state, viewModel, navController) }
            composable(Routes.CustomerHome) { CustomerHomeScreen(state, navController, viewModel) }
            composable("${Routes.WorkerProfile}/{workerId}") { entry ->
                val worker = state.users.firstOrNull { it.id == entry.arguments?.getString("workerId") }
                if (worker != null) WorkerProfileScreen(worker, state, viewModel, navController)
            }
            composable(Routes.WorkerDashboard) { WorkerDashboardScreen(state, viewModel, navController) }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch { }
    }
}

@Composable
private fun SplashScreen(state: AppState, navController: NavHostController) {
    LaunchedEffect(state.currentUser) {
        delay(2000)
        navController.navigate(
            when (state.currentUser?.role) {
                UserRole.WORKER -> Routes.WorkerDashboard
                UserRole.CUSTOMER -> Routes.CustomerHome
                null -> Routes.Login
            }
        ) { popUpTo(Routes.Splash) { inclusive = true } }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Kaushalya Karnataka Logo",
                modifier = Modifier.size(260.dp)
            )
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(200.dp),
                color = AppPrimary,
                trackColor = ZincLight
            )
        }
    }
}

@Composable
private fun LoginScreen(state: AppState, viewModel: KaushalyaViewModel, navController: NavHostController) {
    var role by remember { mutableStateOf(UserRole.CUSTOMER) }
    var email by remember { mutableStateOf(if (role == UserRole.CUSTOMER) "customer@kk.demo" else "ramesh@kk.demo") }
    var password by remember { mutableStateOf("password") }

    LaunchedEffect(state.currentUser) {
        state.currentUser?.let {
            navController.navigate(if (it.role == UserRole.WORKER) Routes.WorkerDashboard else Routes.CustomerHome) {
                popUpTo(Routes.Login) { inclusive = true }
            }
        }
    }

    AuthSurface(title = "Welcome back") {
        RoleToggle(role) {
            role = it
            email = if (it == UserRole.CUSTOMER) "customer@kk.demo" else "ramesh@kk.demo"
        }
        OutlinedTextField(email, { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Person, null) })
        OutlinedTextField(password, { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        Button(
            onClick = {
                if (viewModel.login(email, password, role)) {
                    navController.navigate(if (role == UserRole.WORKER) Routes.WorkerDashboard else Routes.CustomerHome) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
        ) {
            Icon(Icons.AutoMirrored.Filled.Login, null)
            Spacer(Modifier.width(8.dp))
            Text("Login")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("New here?", color = Zinc)
            TextButton(onClick = { navController.navigate(Routes.Register) }) { Text("Register here") }
        }
    }
}

@Composable
private fun RegisterScreen(state: AppState, viewModel: KaushalyaViewModel, navController: NavHostController) {
    var role by remember { mutableStateOf(UserRole.WORKER) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var trade by remember { mutableStateOf(trades.first()) }

    LaunchedEffect(state.currentUser) {
        state.currentUser?.let {
            navController.navigate(if (it.role == UserRole.WORKER) Routes.WorkerDashboard else Routes.CustomerHome) {
                popUpTo(Routes.Login) { inclusive = true }
            }
        }
    }

    AuthSurface(title = "Create account") {
        RoleToggle(role) { role = it }
        OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Full name") })
        OutlinedTextField(email, { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Email") })
        OutlinedTextField(phone, { phone = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Phone") })
        OutlinedTextField(password, { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        if (role == UserRole.WORKER) ChipSelector(trades, trade) { trade = it }
        Button(
            onClick = {
                if (viewModel.register(name, email, phone, password, role, trade)) {
                    navController.navigate(if (role == UserRole.WORKER) Routes.WorkerDashboard else Routes.CustomerHome) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
        ) { Text("Create account") }
        TextButton(onClick = { navController.popBackStack() }) { Text("Back to login") }
    }
}

@Composable
private fun AuthSurface(title: String, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
                Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Local skilled services across Karnataka", color = Zinc)
                content()
            }
        }
    }
}

@Composable
private fun RoleToggle(role: UserRole, onRole: (UserRole) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(UserRole.WORKER to "Worker", UserRole.CUSTOMER to "Customer").forEach { (item, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onRole(item) }) {
                RadioButton(selected = role == item, onClick = { onRole(item) })
                Text(label)
            }
        }
    }
}

@Composable
private fun CustomerHomeScreen(state: AppState, navController: NavHostController, viewModel: KaushalyaViewModel) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val pickProfileImage = rememberImagePicker { viewModel.updateProfilePhoto(it) }
    val workers = state.workers.filter {
        (filter == "All" || it.category == filter) &&
            (query.isBlank() || it.name.contains(query, true) || it.location.contains(query, true))
    }
    Scaffold(
        bottomBar = { CustomerBottomBar(onLogout = { viewModel.logout(); navController.navigate(Routes.Login) { popUpTo(0) } }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                state.currentUser?.let { customer ->
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(customer, 58)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(customer.name, fontWeight = FontWeight.Bold)
                                Text(customer.email, color = Zinc)
                            }
                            TextButton(onClick = pickProfileImage) { Text("Photo") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Text("Find trusted local pros", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    query, { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search by name or location") },
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
            }
            item { ChipSelector(listOf("All") + trades, filter) { filter = it } }
            if (workers.isEmpty()) {
                item { EmptyState("No workers match this search") }
            } else {
                items(workers, key = { it.id }) { worker ->
                    WorkerCard(worker, onClick = { navController.navigate("${Routes.WorkerProfile}/${worker.id}") })
                }
            }
        }
    }
}

@Composable
private fun WorkerCard(worker: AppUser, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(worker)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${worker.category} • ${worker.location}", color = Zinc)
                RatingStars(worker.averageRating.toInt(), readOnly = true)
            }
            Text("${worker.reviewCount}\nreviews", color = AppPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WorkerProfileScreen(worker: AppUser, state: AppState, viewModel: KaushalyaViewModel, navController: NavHostController) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    val services = state.services.filter { it.workerId == worker.id }
    val photos = state.portfolio.filter { it.workerId == worker.id }
    val reviews = state.reviews.filter { it.workerId == worker.id }.sortedByDescending { it.timestamp }

    Scaffold(
        bottomBar = { CustomerBottomBar(onHome = { navController.navigate(Routes.CustomerHome) { launchSingleTop = true } }, onLogout = { viewModel.logout(); navController.navigate(Routes.Login) { popUpTo(0) } }) }
    ) { padding ->
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(Color.White)) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(worker, 88)
                    Text(worker.name, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("${worker.category} • ${worker.location}", color = Zinc)
                    RatingStars(worker.averageRating.toInt(), readOnly = true)
                    Text("${"%.1f".format(worker.averageRating)} (${worker.reviewCount} reviews)", color = Zinc)
                    Spacer(Modifier.height(10.dp))
                    Text(worker.bio)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { viewModel.hire(worker.id) }, colors = ButtonDefaults.buttonColors(containerColor = AppPrimary), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Handyman, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Hire Me")
                    }
                }
            }
        }
        item { SectionTitle("Portfolio") }
        item { PortfolioGrid(photos) }
        item { SectionTitle("Services") }
        items(services, key = { it.id }) { ServiceCard(it) }
        item { SectionTitle("Review Wall") }
        items(reviews, key = { it.id }) { review ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(review.reviewerName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        RatingStars(review.rating, readOnly = true)
                    }
                    Text(review.comment, color = Zinc)
                }
            }
        }
        item {
            SectionTitle("Leave a Review")
            RatingStars(rating, readOnly = false) { rating = it }
            OutlinedTextField(comment, { comment = it }, label = { Text("Comment") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.addReview(worker.id, rating, comment); comment = ""; rating = 0 }, colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)) {
                Text("Submit Review")
            }
        }
    }
    }
}

@Composable
private fun WorkerDashboardScreen(state: AppState, viewModel: KaushalyaViewModel, navController: NavHostController) {
    val worker = state.currentUser ?: return
    var tab by remember { mutableIntStateOf(0) }
    var dialogService by remember { mutableStateOf<WorkerService?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRequests by remember { mutableStateOf(false) }
    val services = state.services.filter { it.workerId == worker.id }
    val photos = state.portfolio.filter { it.workerId == worker.id }
    val requests = state.hireEvents.filter { it.workerId == worker.id }.sortedByDescending { it.timestamp }
    val pickProfileImage = rememberImagePicker { viewModel.updateProfilePhoto(it) }
    val pickPortfolioImage = rememberImagePicker { viewModel.addPortfolioPhoto(it) }

    Scaffold(
        floatingActionButton = {
            if (tab == 0) FloatingActionButton(onClick = { showAddDialog = true }, containerColor = AppPrimary) { Icon(Icons.Default.Add, null, tint = Color.White) }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") })
                NavigationBarItem(selected = false, onClick = { viewModel.logout(); navController.navigate(Routes.Login) { popUpTo(0) } }, icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) }, label = { Text("Logout") })
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(Color.White)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(worker, 64)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(worker.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("${worker.category} • ${worker.location}", color = Zinc)
                        Text("${requests.size} hire requests", color = AppPrimary, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showRequests = true }) { Text("Requests") }
                    TextButton(onClick = pickProfileImage) { Text("Photo") }
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Services") }, icon = { Icon(Icons.Default.Build, null) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Portfolio") }, icon = { Icon(Icons.Default.Image, null) })
            }
            Spacer(Modifier.height(12.dp))
            if (tab == 0) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (services.isEmpty()) item { EmptyState("Add your first service") }
                    items(services, key = { it.id }) { service ->
                        ServiceCard(service, onEdit = { dialogService = service }, onDelete = { viewModel.deleteService(service.id) })
                    }
                }
            } else {
                Button(onClick = pickPortfolioImage, colors = ButtonDefaults.buttonColors(containerColor = AppPrimary), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Photo")
                }
                Spacer(Modifier.height(12.dp))
                PortfolioGrid(photos)
            }
        }
    }
    if (showAddDialog || dialogService != null) {
        ServiceDialog(service = dialogService, onDismiss = { showAddDialog = false; dialogService = null }) { id, title, category, price, priceType ->
            viewModel.addOrUpdateService(id, title, category, price, priceType)
            showAddDialog = false
            dialogService = null
        }
    }
    if (showRequests) {
        AlertDialog(
            onDismissRequest = { showRequests = false },
            title = { Text("Hire Requests") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (requests.isEmpty()) {
                        Text("No hire requests yet", color = Zinc)
                    } else {
                        requests.forEach { request ->
                            val customer = state.users.firstOrNull { it.id == request.customerId }
                            Text(customer?.let { "${it.name} • ${it.phone}" } ?: "Customer ${request.customerId}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRequests = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun ServiceDialog(service: WorkerService?, onDismiss: () -> Unit, onSave: (String?, String, String, String, String) -> Unit) {
    var title by remember(service) { mutableStateOf(service?.title.orEmpty()) }
    var category by remember(service) { mutableStateOf(service?.category ?: serviceCategories.first()) }
    var price by remember(service) { mutableStateOf(service?.price.orEmpty()) }
    var priceType by remember(service) { mutableStateOf(service?.priceType ?: "fixed") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (service == null) "Add service" else "Edit service") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Service name") })
                ChipSelector(serviceCategories, category) { category = it }
                OutlinedTextField(price, { price = it }, label = { Text("Price, e.g. ₹300") })
                ChipSelector(listOf("fixed", "starting"), priceType) { priceType = it }
            }
        },
        confirmButton = { Button(onClick = { onSave(service?.id, title, category, price, priceType) }, colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ServiceCard(service: WorkerService, onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(3.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, null, tint = AppPrimary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(service.title, fontWeight = FontWeight.Bold)
                Text("${service.category} • ${service.priceType}", color = Zinc)
            }
            Text(service.price, color = AppPrimary, fontWeight = FontWeight.Bold)
            onEdit?.let { IconButton(onClick = it) { Icon(Icons.Default.Edit, null) } }
            onDelete?.let { IconButton(onClick = it) { Icon(Icons.Default.Delete, null) } }
        }
    }
}

@Composable
private fun ChipSelector(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
        items(options) { option ->
            FilterChip(selected = selected == option, onClick = { onSelected(option) }, label = { Text(option) })
        }
    }
}

@Composable
private fun RatingStars(rating: Int, readOnly: Boolean, onRating: (Int) -> Unit = {}) {
    Row {
        (1..5).forEach { index ->
            Icon(
                imageVector = if (index <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = AppPrimary,
                modifier = Modifier.size(22.dp).clickable(enabled = !readOnly) { onRating(index) }
            )
        }
    }
}

@Composable
private fun PortfolioGrid(photos: List<PortfolioPhoto>) {
    if (photos.isEmpty()) {
        EmptyState("No portfolio photos yet")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(260.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(photos, key = { it.id }) { photo ->
            AsyncImage(
                model = photo.imageUri.ifBlank { photo.imageRes },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun Avatar(worker: AppUser, size: Int = 56) {
    val imageModel: Any? = when {
        worker.avatarUrl.isNotBlank() -> worker.avatarUrl
        worker.avatarRes != 0 -> worker.avatarRes
        else -> null
    }
    if (imageModel != null) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier.size(size.dp).clip(CircleShape).background(AppPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(worker.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun EmptyState(text: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Brush, null, tint = Zinc, modifier = Modifier.size(42.dp))
        Text(text, color = Zinc)
    }
}

@Composable
private fun CustomerBottomBar(onHome: () -> Unit = {}, onLogout: () -> Unit) {
    NavigationBar {
        NavigationBarItem(selected = true, onClick = onHome, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
        NavigationBarItem(selected = false, onClick = onLogout, icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) }, label = { Text("Logout") })
    }
}

@Composable
private fun rememberImagePicker(onImagePicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
            onImagePicked(it.toString())
        }
    }
    return { launcher.launch(arrayOf("image/*")) }
}
