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
import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.content.Context
import android.content.res.Resources
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.eq
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for the implementation of [TaskDetailViewModel]
 */
class TaskDetailViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule var instantExecutorRule = InstantTaskExecutorRule()
    @Mock private lateinit var tasksRepository: TasksRepository
    @Mock private lateinit var context: Application
    private lateinit var taskDetailViewModel: TaskDetailViewModel
    private lateinit var task: Task
    private val TITLE_TEST = "title"
    private val DESCRIPTION_TEST = "description"
    private val NO_DATA_STRING = "NO_DATA_STRING"
    private val NO_DATA_DESC_STRING = "NO_DATA_DESC_STRING"

    @Before fun setupTasksViewModel() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this)

        setupContext()

        task = Task(TITLE_TEST, DESCRIPTION_TEST)

        // Get a reference to the class under test
        taskDetailViewModel = TaskDetailViewModel(context, tasksRepository, Unconfined)
    }

    private fun setupContext() {
        `when`<Context>(context.applicationContext).thenReturn(context)
        `when`(context.getString(R.string.no_data)).thenReturn(NO_DATA_STRING)
        `when`(context.getString(R.string.no_data_description)).thenReturn(NO_DATA_DESC_STRING)
        `when`(context.resources).thenReturn(mock(Resources::class.java))
    }

    @Test fun getActiveTaskFromRepositoryAndLoadIntoView() = runBlocking<Unit> {
        setupViewModelRepositoryCallback()

        // Then verify that the view was notified
        assertEquals(taskDetailViewModel.task.get().title, task.title)
        assertEquals(taskDetailViewModel.task.get().description, task.description)
    }

    @Test fun deleteTask() = runBlocking<Unit> {
        setupViewModelRepositoryCallback()

        // When the deletion of a task is requested
        taskDetailViewModel.deleteTask()

        // Then the repository is notified
        verify<TasksRepository>(tasksRepository).deleteTask(task.id)
    }

    @Test fun completeTask() = runBlocking<Unit> {
        setupViewModelRepositoryCallback()

        // When the ViewModel is asked to complete the task
        taskDetailViewModel.setCompleted(true)

        // Then a request is sent to the task repository and the UI is updated
        verify<TasksRepository>(tasksRepository).completeTask(task)
        assertThat<Int>(taskDetailViewModel.snackbarMessage.value,
                `is`(R.string.task_marked_complete))
    }

    @Test fun activateTask() = runBlocking<Unit> {
        setupViewModelRepositoryCallback()

        // When the ViewModel is asked to complete the task
        taskDetailViewModel.setCompleted(false)

        // Then a request is sent to the task repository and the UI is updated
        verify<TasksRepository>(tasksRepository).activateTask(task)
        assertThat<Int>(taskDetailViewModel.snackbarMessage.value,
                `is`(R.string.task_marked_active))
    }

    @Test fun TaskDetailViewModel_repositoryError() = runBlocking<Unit> {
        // When the repository returns an error
        `when`(tasksRepository.getTask(eq(task.id))).thenReturn(null) // Trigger callback error

        // Given an initialized ViewModel with an active task
        taskDetailViewModel.start(task.id)

        verify<TasksRepository>(tasksRepository).getTask(eq(task.id))

        // Then verify that data is not available
        assertFalse(taskDetailViewModel.isDataAvailable)
    }

    private suspend fun setupViewModelRepositoryCallback() {
        // Given an initialized ViewModel with an active task
        `when`(tasksRepository.getTask(eq(task.id))).thenReturn(task)

        taskDetailViewModel.start(task.id)

        // Verify that the task repository has been called
        verify(tasksRepository).getTask(eq(task.id))
    }

    @Test fun updateSnackbar_nullValue() {
        // Before setting the Snackbar text, get its current value
        val snackbarText = taskDetailViewModel.snackbarMessage

        // Check that the value is null
        assertThat<Int>("Snackbar text does not match", snackbarText.value, `is`(nullValue()))
    }
}
