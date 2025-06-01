package org.example.todolist
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.example.todolist.entity.TodoListItem
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TodoListItemViewModel(private val sdk: TodoListSDK) : ViewModel() {
    private val _state = mutableStateOf(TodoListScreenState())
    val state: State<TodoListScreenState> = _state
    fun loadIncompleteTodos() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, launches = emptyList())
            try {
                val launches = sdk.getIncompleteTodos()
                _state.value = _state.value.copy(isLoading = false, launches = launches)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, launches = emptyList())
            }
        }
    }

    fun addTodo(title: String, description: String?, dueDate: Long?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                sdk.addTodo(title, description, dueDate)
            } catch (e: Exception) {
                // Handle error
                Log.e("TodoVM", "Error adding todo", e)
            } finally {
                // Reload todos regardless of success or failure of the add operation itself,
                // unless you have more specific error handling that prevents a refresh.
                loadIncompleteTodos() // This will set isLoading to false eventually
            }
        }
    }
    fun updateTodo(item: TodoListItem) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                sdk.updateTodo(item)
            } catch (e: Exception) {
                // Handle error
                Log.e("TodoVM", "Error updating todo", e)
            } finally {
                loadIncompleteTodos()
            }
        }
    }
    fun toggleComplete(item: TodoListItem) {
        viewModelScope.launch {
            // Optimistically update the UI for a smoother experience
            val updatedItem = item.copy(isCompleted = !item.isCompleted)

            // Update the local state immediately for responsiveness
            // This is a simple optimistic update. For robust apps, consider how to handle SDK failures.
            // _state.value = _state.value.copy(
            //     launches = _state.value.launches.map { if (it.id == updatedItem.id) updatedItem else it }
            // )
            // For now, we'll rely on loadIncompleteTodos to refresh, which is simpler.

            _state.value = _state.value.copy(isLoading = true) // Show loading for the operation
            try {
                sdk.updateTodo(updatedItem) // SDK updates the backend/database
            } catch (e: Exception) {
                // Handle error, potentially revert optimistic update if implemented
                // Log.e("TodoVM", "Error toggling complete", e)
            } finally {
                // Reload all incomplete todos. If an item was marked complete, it will disappear.
                // If it was marked incomplete, it will appear (if it wasn't already shown).
                loadIncompleteTodos()
            }
        }
    }

    fun deleteTodo(item: TodoListItem) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                sdk.deleteTodo(item.id) // Assuming deleteTodo takes the ID
            } catch (e: Exception) {
                // Handle error
                // Log.e("TodoVM", "Error deleting todo", e)
            } finally {
                loadIncompleteTodos()
            }
        }
    }

    fun loadAllTodos() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, launches = emptyList())
            try {
                val launches = sdk.getAllTodos()
                _state.value = _state.value.copy(isLoading = false, launches = launches)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, launches = emptyList())
            }
        }
    }

    init {
        loadIncompleteTodos()
    }
}

data class TodoListScreenState(
    val isLoading: Boolean = false,
    val launches: List<TodoListItem> = emptyList()
)