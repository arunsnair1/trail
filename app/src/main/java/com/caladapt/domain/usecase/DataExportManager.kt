package com.caladapt.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.caladapt.data.db.dao.DailySummaryDao
import com.caladapt.data.db.dao.BodyMeasurementDao
import com.caladapt.data.db.dao.WeightLogDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val weightLogDao: WeightLogDao,
    private val dailySummaryDao: DailySummaryDao,
    private val bodyMeasurementDao: BodyMeasurementDao
) {

    /**
     * Exports all user data to CSV files in the app's external files directory
     * and returns a list of File objects.
     */
    suspend fun exportDataToCsv(): List<File> = withContext(Dispatchers.IO) {
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val weightFile = File(exportDir, "weight_log.csv")
        val calorieFile = File(exportDir, "calorie_log.csv")
        val measurementFile = File(exportDir, "measurements.csv")

        exportWeights(weightFile)
        exportCalories(calorieFile)
        exportMeasurements(measurementFile)

        return@withContext listOf(weightFile, calorieFile, measurementFile)
    }

    private suspend fun exportWeights(file: File) {
        val weights = weightLogDao.getAll()
        FileWriter(file).use { writer ->
            writer.append("Date,WeightKg,EMAWeight,IsWaterRetention\n")
            weights.forEach { log ->
                writer.append("${log.date},${log.weightKg},${log.emaWeight},${log.isCycleWaterRetention}\n")
            }
        }
    }

    private suspend fun exportCalories(file: File) {
        val summaries = dailySummaryDao.getAll()
        FileWriter(file).use { writer ->
            writer.append("Date,TotalCalories,TotalProtein,TotalCarbs,TotalFat\n")
            summaries.forEach { log ->
                writer.append("${log.date},${log.totalCalories},${log.totalProtein},${log.totalCarbs},${log.totalFat}\n")
            }
        }
    }

    private suspend fun exportMeasurements(file: File) {
        val measurements = bodyMeasurementDao.getAll()
        FileWriter(file).use { writer ->
            writer.append("Date,WaistCm,NeckCm,HipsCm\n")
            measurements.forEach { m ->
                writer.append("${m.date},${m.waistCm},${m.neckCm},${m.hipsCm ?: ""}\n")
            }
        }
    }
}
