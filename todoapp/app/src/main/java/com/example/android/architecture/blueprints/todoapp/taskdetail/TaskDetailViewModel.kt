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
package com.example.android.architecture.blueprints.todoapp.taskdetail

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.support.annotation.StringRes
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.SingleLiveEvent
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.tasks.TasksFragment
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


/**
 * Listens to user actions from the list item in ([TasksFragment]) and redirects them to the
 * Fragment's actions listener.
 */
class TaskDetailViewModel(
        context: Application,
        private val tasksRepository: TasksRepository,
        private val dispatcher: CoroutineDispatcher = UI // a different Dispatcher is used in unit tests
) : AndroidViewModel(context) {

    val task = ObservableField<Task>()
    val completed = ObservableBoolean()
    val editTaskCommand = SingleLiveEvent<Void>()
    val deleteTaskCommand = SingleLiveEvent<Void>()
    val snackbarMessage = SingleLiveEvent<Int>()
    var isDataLoading = false
        private set
    val isDataAvailable
        get() = task.get() != null

    fun deleteTask() = launch(dispatcher, CoroutineStart.UNDISPATCHED) {
        task.get()?.let {
            tasksRepository.deleteTask(it.id)
            deleteTaskCommand.call()
        }
    }

    fun editTask() {
        editTaskCommand.call()
    }

    fun setCompleted(completed: Boolean) = launch(dispatcher, CoroutineStart.UNDISPATCHED) {
        if (isDataLoading) {
            return@launch
        }
        val task = this@TaskDetailViewModel.task.get().apply {
            isCompleted = completed
        }
        if (completed) {
            tasksRepository.completeTask(task)
            showSnackbarMessage(R.string.task_marked_complete)
        } else {
            tasksRepository.activateTask(task)
            showSnackbarMessage(R.string.task_marked_active)
        }
    }

    fun start(taskId: String?) = launch(dispatcher, CoroutineStart.UNDISPATCHED) {
        if (taskId != null) {
            isDataLoading = true
            val task = tasksRepository.getTask(taskId)
            isDataLoading = false
            setTask(task)
        }
    }

    private fun setTask(task: Task?) {
        this.task.set(task)
        if (task != null) {
            completed.set(task.isCompleted)
        }
    }

    fun onRefresh() {
        if (task.get() != null) {
            start(task.get().id)
        }
    }

    private fun showSnackbarMessage(@StringRes message: Int) {
        snackbarMessage.value = message
    }
}
