package org.example.todolist
import org.example.todolist.entity.TodoListItem
import org.example.todolist.cache.Database
import org.example.todolist.cache.DatabaseDriverFactory

class TodoListSDK(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = Database(databaseDriverFactory)
    @Throws(Exception::class)
    suspend fun getIncompleteTodos(): List<TodoListItem> {
        val cachedTodos = database.getIncompleteTodos()
        return cachedTodos
    }
    fun getAllTodos(): List<TodoListItem> {
        val cachedTodos = database.getAllTodos()
        return cachedTodos
    }
    fun addTodo(title: String, description: String?, dueDate: Long?) {
        database.insertTodo(title, description, dueDate)
    }
    fun updateTodo(item: TodoListItem) {
        database.updateTodo(item.id, item.title, item.description, item.isCompleted, item.dueDate)
    }
    fun deleteTodo(id: Long) {
        database.deleteTodoById(id)
    }
}