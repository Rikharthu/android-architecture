/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.data

import android.support.annotation.VisibleForTesting
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource

/**
 * Implementation of a remote data source with static access to the data for easy testing.
 */
object FakeTasksRemoteDataSource : TasksDataSource {

    private var TASKS_SERVICE_DATA = mutableMapOf<String, Task>()

    override suspend fun getTasks(): List<Task>? = TASKS_SERVICE_DATA.values.toList()

    override suspend fun getTask(taskId: String): Task? = TASKS_SERVICE_DATA[taskId]

    override suspend fun saveTask(task: Task) {
        TASKS_SERVICE_DATA[task.id] = task
    }

    override suspend fun completeTask(task: Task) {
        val completedTask = Task(task.title, task.description, task.id)
        completedTask.isCompleted = true
        TASKS_SERVICE_DATA.put(task.id, completedTask)
    }

    override suspend fun completeTask(taskId: String) {
        // Not required for the remote data source.
    }

    override suspend fun activateTask(task: Task) {
        val activeTask = Task(task.title, task.description, task.id)
        TASKS_SERVICE_DATA[task.id] = activeTask
    }

    override suspend fun activateTask(taskId: String) {
        // Not required for the remote data source.
    }

    override suspend fun clearCompletedTasks() {
        TASKS_SERVICE_DATA = TASKS_SERVICE_DATA.filterValues {
            !it.isCompleted
        }.toMutableMap()
    }

    override fun refreshTasks() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    override suspend fun deleteTask(taskId: String) {
        TASKS_SERVICE_DATA.remove(taskId)
    }

    override suspend fun deleteAllTasks() {
        TASKS_SERVICE_DATA.clear()
    }

    @VisibleForTesting
    fun addTasks(vararg tasks: Task) {
        for (task in tasks) {
            TASKS_SERVICE_DATA.put(task.id, task)
        }
    }
}
