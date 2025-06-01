package org.example.todolist.entity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TodoListItem(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String?,
    @SerialName("isCompleted") val isCompleted: Boolean,
    @SerialName("dueDate") val dueDate: Long?
)
