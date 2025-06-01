package org.example.todolist
import org.example.todolist.cache.AndroidDatabaseDriverFactory
import org.example.todolist.cache.TodoList
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<TodoListSDK> {
        TodoListSDK(
            databaseDriverFactory = AndroidDatabaseDriverFactory(
                androidContext()
            )
        )
    }
    viewModel { TodoListItemViewModel(sdk = get()) }
}