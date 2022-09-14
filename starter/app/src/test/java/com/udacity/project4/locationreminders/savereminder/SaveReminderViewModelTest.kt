package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    lateinit var fakeDataSource: FakeDataSource
    lateinit var saveReminderViewModel : SaveReminderViewModel

    @Before
    fun init () {
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @After
    fun destroy () {
        stopKoin()
    }

    @Test
    fun check_reminderItem_validation () = runBlockingTest {
        val reminderWithoutTitle = createTestReminderDataItem()
        reminderWithoutTitle.title = ""

        val reminderWithoutLocation = createTestReminderDataItem()
        reminderWithoutLocation.location = ""

        val reminderWithoutDescription = createTestReminderDataItem()
        reminderWithoutDescription.description = ""

        assertThat(saveReminderViewModel.validateEnteredData(reminderWithoutTitle), `is`(false))
        assertThat(saveReminderViewModel.validateEnteredData(reminderWithoutLocation), `is`(false))
        assertThat(saveReminderViewModel.validateEnteredData(reminderWithoutDescription), `is`(true))
    }

    @Test
    fun check_savesInvalidReminder_returnsError () = runBlockingTest {
        fakeDataSource.deleteAllReminders()
        saveReminderViewModel.showSnackBarInt.value = null

        val reminderWithoutTitle = createTestReminderDataItem()
        reminderWithoutTitle.title = ""

        saveReminderViewModel.validateAndSaveReminder(reminderWithoutTitle)

        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun check_showLoading_isDisplayed () = runBlockingTest{
        val reminder = createTestReminderDataItem()
        mainCoroutineRule.pauseDispatcher()
        saveReminderViewModel.validateAndSaveReminder(reminder)

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    fun createTestReminderDataItem () : ReminderDataItem {
        return ReminderDataItem (
            "TEST",
            "This is a test reminder",
            "Somewhere",
            -0.123,
            - 0.123
        )
    }
}