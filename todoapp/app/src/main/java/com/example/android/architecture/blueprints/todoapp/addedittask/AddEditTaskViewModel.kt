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
package com.example.android.architecture.blueprints.todoapp.addedittask

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.support.annotation.StringRes
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.SingleLiveEvent
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

/**
 * ViewModel for the Add/Edit screen.
 *
 *
 * This ViewModel only exposes [ObservableField]s, so it doesn't need to extend
 * [android.databinding.BaseObservable] and updates are notified automatically. See
 * [com.example.android.architecture.blueprints.todoapp.statistics.StatisticsViewModel] for
 * how to deal with more complex scenarios.
 */
class AddEditTaskViewModel(
        context: Application,
        private val tasksRepository: TasksRepository,
        private val dispatcher: CoroutineDispatcher = UI // a different Dispatcher is used in unit tests
) : AndroidViewModel(context) {

    val title = ObservableField<String>("")
    val description = ObservableField<String>("")
    val dataLoading = ObservableBoolean(false)
    internal val snackbarMessage = SingleLiveEvent<Int>()
    internal val taskUpdatedEvent = SingleLiveEvent<Void>()
    private var taskId: String? = null
    private val isNewTask
        get() = taskId == null
    private var isDataLoaded = false
    private var taskCompleted = false

    fun start(taskId: String?) = launch(dispatcher, CoroutineStart.UNDISPATCHED) {
        if (dataLoading.get()) {
            // Already loading, ignore.
            return@launch
        }
        this@AddEditTaskViewModel.taskId = taskId
        if (isNewTask || isDataLoaded) {
            // No need to populate, it's a new task or it already has data
            return@launch
        }

        if (taskId != null) {
            dataLoading.set(true)
            val task = tasksRepository.getTask(taskId)
            dataLoading.set(false)
            if (task != null) {
                title.set(task.title)
                description.set(task.description)
                taskCompleted = task.isCompleted
                isDataLoaded = true

                // Note that there's no need to notify that the values changed because we're using
                // ObservableFields.
            }
        }
    }

    // Called when clicking on fab.
    fun saveTask() = launch(dispatcher, CoroutineStart.UNDISPATCHED) {
        val task = Task(title.get(), description.get())
        if (task.isEmpty) {
            showSnackbarMessage(R.string.empty_task_message)
            return@launch
        }
        if (isNewTask) {
            createTask(task)
        } else {
            taskId?.let {
                updateTask(Task(title.get(), description.get(), it)
                        .apply { isCompleted = taskCompleted })
            }
        }
    }


    private suspend fun createTask(newTask: Task) {
        tasksRepository.saveTask(newTask)
        taskUpdatedEvent.call()
    }

    private suspend fun updateTask(task: Task) {
        if (isNewTask) {
            throw RuntimeException("updateTask() was called but task is new.")
        }
        tasksRepository.saveTask(task)
        taskUpdatedEvent.call()
    }

    private fun showSnackbarMessage(@StringRes message: Int) {
        snackbarMessage.value = message
    }
}
