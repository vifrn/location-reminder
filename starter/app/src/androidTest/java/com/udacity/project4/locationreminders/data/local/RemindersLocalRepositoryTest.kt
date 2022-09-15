package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var repository: RemindersLocalRepository
    lateinit var database : RemindersDatabase
    val reminders : MutableList<ReminderDTO> = mutableListOf()

    @Before
    fun init () {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java).build()

        for (i in 1..5) {
            reminders.add(
                ReminderDTO(
                    "TEST$i",
                    "This is the test number $i",
                    "Somewhere St, $i",
                    i * 2 + 0.12345,
                    i * 2 + 0.12345,
                    i.toString()))
        }

        repository = RemindersLocalRepository(database.reminderDao())
    }

    @After
    fun closeDB () {
        database.close()
    }

    @Test
    fun saveReminders () = runBlocking {
        for (reminder in reminders) {
            repository.saveReminder(reminder)
        }

        val reminderFromDB = database.reminderDao().getReminders()

        assertThat(reminderFromDB, `is`(notNullValue()))
        assertThat(reminderFromDB, `is`(not(emptyList())))
        assertThat(reminderFromDB.size, `is`(reminders.size))
    }

    @Test
    fun getRemindersById () = runBlocking {
        val reminder = ReminderDTO(
            "TEST",
            "This is the test number",
            "Somewhere St",
            0.12345,
            0.12345)
        database.reminderDao().saveReminder(reminder)

        val id = reminder.id
        val result = repository.getReminder(id)

        assertThat(result is Result.Success, `is`(true))

        if (result is Result.Success) {
            val repoReminder = result.data
            assertThat(repoReminder.title, `is`(reminder.title))
        }
    }

    @Test
    fun getInexistentReminder () = runBlocking {
        val result = repository.getReminder("999")

        assertThat(result is Result.Error, `is`(true))

        if(result is Result.Error) {
            assertThat(result.message, `is`("Reminder not found!"))
        }
    }

    @Test
    fun deleteAll () = runBlocking {
        for (reminder in reminders) {
            database.reminderDao().saveReminder(reminder)
        }

        repository.deleteAllReminders()

        assertThat(database.reminderDao().getReminders(), `is`(emptyList()))
    }

}