package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

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
    }

    @After
    fun closeDB () {
        database.close()
    }

    @Test
    fun saveReminders () = runBlocking {
        for (reminder in reminders) {
            database.reminderDao().saveReminder(reminder)
        }
        assertThat(database.reminderDao().getReminders(), `is`(notNullValue()))
        assertThat(database.reminderDao().getReminders(), `is`(not(emptyList())))
        assertThat(database.reminderDao().getReminders().size, `is`(reminders.size))
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
        val reminderFromDB = database.reminderDao().getReminderById(id)

        assertThat(reminderFromDB, `is`(notNullValue()))
        assertThat(reminderFromDB?.title, `is`(reminder.title))
    }

    @Test
    fun deleteAll () = runBlocking {
        for (reminder in reminders) {
            database.reminderDao().saveReminder(reminder)
        }

        database.reminderDao().deleteAllReminders()
        assertThat(database.reminderDao().getReminders(), `is`(emptyList()))
    }

}