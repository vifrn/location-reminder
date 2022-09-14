package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    var reminders: MutableList<ReminderDTO> = mutableListOf()
    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Everything is 'fine'. This is a test error.")
        }
        return Result.Success(ArrayList(reminders))
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Everything is 'fine'. This is a test error.")
        }

        for (rem in reminders) {
            if(rem.id == id) {
                return Result.Success(rem)
            }
        }
        return Result.Error("This reminder was not added")
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}