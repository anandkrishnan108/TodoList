package org.example.todolist.cache

import org.example.todolist.entity.TodoListItem

internal class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = AppDatabase(databaseDriverFactory.createDriver())
    private val dbQuery = database.appDatabaseQueries

    private fun mapSqlDelightTodoToEntity(dbTodo: org.example.todolist.cache.TodoList): TodoListItem {
        return TodoListItem(
            id = dbTodo.id,
            title = dbTodo.title,
            description = dbTodo.description,
            isCompleted = dbTodo.is_completed == 1L, // Convert Long (0 or 1) to Boolean
            dueDate = dbTodo.due_date
        )
    }
    private fun mapSqlDelightGetIncompleteTodosDueByToEntity(dbTodo: org.example.todolist.cache.GetIncompleteTodosDueBy): TodoListItem {
        return TodoListItem(
            id = dbTodo.id,
            title = dbTodo.title,
            description = dbTodo.description,
            isCompleted = dbTodo.is_completed == 1L, // This query filters for is_completed = 0, so this will be false
            dueDate = dbTodo.due_date // This is Long (not nullable) in GetIncompleteTodosDueBy
        )
    }

    fun getAllTodos(): List<TodoListItem> {
        return dbQuery.getAllTodos()
            .executeAsList()
            .map(::mapSqlDelightTodoToEntity)
    }

    fun getTodoById(id: Long): TodoListItem? {
        return dbQuery.getTodoById(id)
            .executeAsOneOrNull()
            ?.let(::mapSqlDelightTodoToEntity)
    }

    fun getIncompleteTodos(): List<TodoListItem> {
        // The generated query in AppDatabaseQueries.kt takes timestamp: Long?
        // However, your .sq query has "due_date IS NOT NULL AND due_date <= :timestamp"
        // So, a non-null timestamp is appropriate here.
        return dbQuery.getIncompleteTodos()
            .executeAsList()
            .map(::mapSqlDelightTodoToEntity) // You can reuse your existing mapper
    }

    fun getIncompleteTodosDueBy(timestamp: Long): List<TodoListItem> {
        // The generated query in AppDatabaseQueries.kt takes timestamp: Long?
        // However, your .sq query has "due_date IS NOT NULL AND due_date <= :timestamp"
        // So, a non-null timestamp is appropriate here.
        return dbQuery.getIncompleteTodosDueBy(timestamp)
            .executeAsList()
            .map(::mapSqlDelightGetIncompleteTodosDueByToEntity)
    }

    fun insertTodo(title: String, description: String?, dueDate: Long?) {
        dbQuery.insertTodo(
            title = title,
            description = description,
            due_date = dueDate
        )
        // Note: SQLDelight's basic insert doesn't return the ID of the newly inserted row by default for SQLite.
        // If you need the ID, you'd typically query `last_insert_rowid()` in a transaction
        // or use database features like RETURNING if supported and configured.
    }
    fun updateTodo(id: Long, title: String, description: String?, isCompleted: Boolean, dueDate: Long?) {
        dbQuery.updateTodo(
            id = id,
            title = title,
            description = description,
            is_completed = if (isCompleted) 1L else 0L, // Convert Boolean to Long (0 or 1)
            due_date = dueDate
        )
    }
    fun markAsCompleted(id: Long) {
        dbQuery.markAsCompleted(id = id)
    }
    fun markAsIncomplete(id: Long) {
        dbQuery.markAsIncomplete(id = id)
    }
    fun deleteTodoById(id: Long) {
        dbQuery.deleteTodoById(id = id)
    }
    fun deleteAllTodos(id: Long) {
        dbQuery.deleteTodoById(id = id);
    }
    fun deleteCompletedTodos() {
        dbQuery.deleteCompletedTodos()
    }
    fun <T> transactionWithResult(body: () -> T): T {
        return dbQuery.transactionWithResult {
            body()
        }
    }
    fun transaction(body: () -> Unit) {
        dbQuery.transaction {
            body()
        }
    }
}
