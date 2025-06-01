package org.example.todolist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday // Icon for Date Picker
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState // Specific import for DatePickerState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.example.todolist.entity.TodoListItem
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Helper function to format Long timestamp (milliseconds) to String
fun Long?.toFormattedDateString(pattern: String = "yyyy-MM-dd"): String {
    if (this == null) return "Not set"
    return try {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC") // DatePicker uses UTC milliseconds
        sdf.format(Date(this))
    } catch (e: Exception) {
        "Invalid Date"
    }
}

// Helper function to parse String to Long timestamp (milliseconds)
// Returns null if parsing fails. Assumes input is "yyyy-MM-dd".
fun String?.parseDateToMillis(): Long? {
    if (this.isNullOrBlank()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC") // Assume input date is in local context, convert to UTC midnight
        val date = sdf.parse(this)
        // To get UTC midnight for the selected day
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = date ?: return null
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    } catch (e: Exception) {
        null
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val viewModel = koinViewModel<TodoListItemViewModel>()
    val state by viewModel.state
    val coroutineScope = rememberCoroutineScope()
    var isRefreshingManual by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // State for Add/Edit Dialog
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoListItem?>(null) }
    var todoTitleInput by remember { mutableStateOf(TextFieldValue("")) }
    var todoDescriptionInput by remember { mutableStateOf(TextFieldValue("")) }
    var selectedDueDateMillis by remember { mutableStateOf<Long?>(null) }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    // State for Delete Confirmation Dialog
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TodoListItem?>(null) }


    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Tasks") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    editingTodo = null
                    todoTitleInput = TextFieldValue("")
                    todoDescriptionInput = TextFieldValue("")
                    selectedDueDateMillis = null
                    showAddEditDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add new task")
                }
            }
        ) { padding ->
            PullToRefreshBox(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                state = pullToRefreshState,
                isRefreshing = isRefreshingManual,
                onRefresh = {
                    isRefreshingManual = true
                    coroutineScope.launch {
                        viewModel.loadIncompleteTodos()
                        isRefreshingManual = false
                    }
                }
            ) {
                when {
                    state.isLoading && state.launches.isEmpty() && !isRefreshingManual -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.launches.isEmpty() && !state.isLoading -> {
                        Text(
                            "No tasks yet! Tap the '+' button to add one.",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(state.launches, key = { it.id }) { todoItem ->
                                TodoItemRow(
                                    todo = todoItem,
                                    onToggleComplete = {
                                        viewModel.toggleComplete(todoItem)
                                    },
                                    onEdit = {
                                        editingTodo = todoItem
                                        todoTitleInput = TextFieldValue(todoItem.title)
                                        todoDescriptionInput = TextFieldValue(todoItem.description ?: "")
                                        selectedDueDateMillis = todoItem.dueDate?.times(1000L)
                                        showAddEditDialog = true
                                    },
                                    onDeleteRequest = { // Changed from onDelete
                                        taskToDelete = todoItem
                                        showDeleteConfirmDialog = true
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        if (showAddEditDialog) {
            AddEditTodoDialog(
                editingTodo = editingTodo,
                currentTitle = todoTitleInput,
                currentDescription = todoDescriptionInput,
                selectedDueDateMillis = selectedDueDateMillis,
                onTitleChange = { todoTitleInput = it },
                onDescriptionChange = { todoDescriptionInput = it },
                onShowDatePicker = { showDatePickerDialog = true },
                onClearDueDate = { selectedDueDateMillis = null },
                onDismiss = { showAddEditDialog = false },
                onConfirm = { title, description, dueDateMillis ->
                    val dueDateSeconds = dueDateMillis?.div(1000L)
                    if (editingTodo == null) {
                        viewModel.addTodo(title, description, dueDateSeconds)
                        println("Adding: $title, Desc: $description, Due: ${dueDateSeconds?.toFormattedDateString()}")
                    } else {
                        viewModel.updateTodo(
                            editingTodo!!.copy(
                                title = title,
                                description = description,
                                dueDate = dueDateSeconds
                            )
                        )
                        println("Updating: ${editingTodo?.title} to $title, Desc: $description, Due: ${dueDateSeconds?.toFormattedDateString()}")
                    }
                    showAddEditDialog = false
                }
            )
        }

        if (showDatePickerDialog) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDueDateMillis ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedDueDateMillis = datePickerState.selectedDateMillis
                        showDatePickerDialog = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Conditionally display the Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            DeleteConfirmationDialog(
                taskName = taskToDelete?.title ?: "this task",
                onConfirmDelete = {
                    taskToDelete?.let { viewModel.deleteTodo(it) }
                    showDeleteConfirmDialog = false
                    taskToDelete = null // Reset after deletion
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                    taskToDelete = null // Reset if dismissed
                }
            )
        }
    }
}

@Composable
fun TodoItemRow(
    todo: TodoListItem,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit // Changed from onDelete to onDeleteRequest
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onEdit),
        headlineContent = {
            Text(
                text = todo.title,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Column {
                todo.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                todo.dueDate?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Due date",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Due: ${it.times(1000L).toFormattedDateString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        leadingContent = {
            IconButton(onClick = onToggleComplete) {
                Icon(
                    imageVector = if (todo.isCompleted) Icons.Filled.TaskAlt else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (todo.isCompleted) "Mark as incomplete" else "Mark as complete",
                    tint = if (todo.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit task", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onDeleteRequest, modifier = Modifier.size(40.dp)) { // Changed from onDelete
                    Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTodoDialog(
    editingTodo: TodoListItem?,
    currentTitle: TextFieldValue,
    currentDescription: TextFieldValue,
    selectedDueDateMillis: Long?,
    onTitleChange: (TextFieldValue) -> Unit,
    onDescriptionChange: (TextFieldValue) -> Unit,
    onShowDatePicker: () -> Unit,
    onClearDueDate: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String?, dueDateMillis: Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingTodo == null) "Add New Task" else "Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Task Title*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = currentDescription,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    maxLines = 3
                )
                Text("Due Date", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onShowDatePicker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Select Due Date", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(selectedDueDateMillis.toFormattedDateString())
                    }
                    if (selectedDueDateMillis != null) {
                        IconButton(onClick = onClearDueDate) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear Due Date")
                        }
                    }
                }
                Text(
                    "* Title is required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentTitle.text.isNotBlank()) {
                        onConfirm(
                            currentTitle.text,
                            currentDescription.text.takeIf { it.isNotBlank() },
                            selectedDueDateMillis
                        )
                    }
                },
                enabled = currentTitle.text.isNotBlank()
            ) {
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

// New Composable for the Delete Confirmation Dialog
@Composable
fun DeleteConfirmationDialog(
    taskName: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Delete") },
        text = { Text("Are you sure you want to delete \"$taskName\"?") },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // Use error color for delete
            ) {
                Text("Yes, Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}
