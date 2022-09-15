package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    @Before
    fun init () {
        fakeDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @After
    fun destroy () {
        stopKoin()
    }

    @Test
    fun check_noData_isDisplayed () = runBlockingTest {
        fakeDataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showNoData.value, `is`(true))
    }

    @Test
    fun check_noData_isNotDisplayed () = runBlockingTest {
        val reminder = createTestReminderDTO()
        fakeDataSource.deleteAllReminders()
        fakeDataSource.saveReminder(reminder)
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showNoData.value, `is` (false))
    }

    @Test
    fun check_showLoading_isDisplayed () = runBlockingTest{
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun check_showSnackBar_displaysErrorMessage () = runBlockingTest {
        fakeDataSource.setReturnError(true)
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Unable to get the reminders."))
    }

    fun createTestReminderDTO () : ReminderDTO {
        return ReminderDTO (
            "TEST",
            "This is a test reminder",
            "Somewhere",
            -0.123,
            - 0.123
        )
    }
}