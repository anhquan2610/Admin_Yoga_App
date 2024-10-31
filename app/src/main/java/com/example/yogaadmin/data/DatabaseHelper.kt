package com.example.yogaadmin.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

data class YogaClass(
    val classId: Int = 0,
    val courseId: Int,
    val teacherName: String,
    val classDate: String,
    val className: String,
    val description: String,
    var isSynced: Boolean = false,
    var firestoreClassId: String? = null,
    val courseFirestoreId: String?
)



class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "yoga.db"
        private const val DATABASE_VERSION = 24



        // Courses table constants
        private const val TABLE_COURSES = "courses"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DAYOFWEEK = "dayOfWeek"
        private const val COLUMN_PRICE = "price"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_DURATION_COURSE = "duration"
        private const val COLUMN_FIRESTORE_ID = "firestoreId"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_CAPACITY = "capacity"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_TYPE = "courseType"





        // Classes table constants
        private const val TABLE_CLASSES = "classes"
        private const val COLUMN_CLASS_ID = "classId"
        private const val COLUMN_FIRESTORECLASS_ID = "firestoreClassId"
        private const val COLUMN_NAME_CLASS = "className"
        private const val COLUMN_COURSE_ID = "courseId"
        private const val COLUMN_TEACHER_NAME = "teacherName"
        private const val COLUMN_CLASS_DATE = "classDate"
        private const val COLUMN_FEE = "fee"
        private const val COLUMN_DESCRIPTION_CLASS = "description"
        private const val COLUMN_DAYS = "days"
        private const val COLUMN_DURATION = "classDuration"
        private const val COLUMN_FIRESTORE_COURSE_ID = "courseFirestoreId"


    }


    override fun onCreate(db: SQLiteDatabase?) {
        val createCoursesTable = """
            CREATE TABLE $TABLE_COURSES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT,
                $COLUMN_DAYOFWEEK TEXT,
                $COLUMN_DURATION_COURSE INTEGER,
                $COLUMN_TIME TEXT,
                $COLUMN_PRICE REAL,
                $COLUMN_CAPACITY INTEGER,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_TYPE TEXT,
                $COLUMN_FIRESTORE_ID TEXT
            )
        """.trimIndent()

        val createClassesTable = """
            CREATE TABLE $TABLE_CLASSES (
                $COLUMN_CLASS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_COURSE_ID INTEGER,
                $COLUMN_TEACHER_NAME TEXT,
                $COLUMN_CLASS_DATE TEXT,
                $COLUMN_FEE REAL,
                $COLUMN_NAME_CLASS TEXT,
                $COLUMN_DESCRIPTION_CLASS TEXT,  
                $COLUMN_DAYS TEXT,  
                $COLUMN_DURATION TEXT,
                $COLUMN_FIRESTORECLASS_ID TEXT,
                $COLUMN_FIRESTORE_COURSE_ID TEXT,
                FOREIGN KEY($COLUMN_COURSE_ID) REFERENCES $TABLE_COURSES($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db?.execSQL(createCoursesTable)
        db?.execSQL(createClassesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 15) {
            db?.execSQL("ALTER TABLE $TABLE_CLASSES ADD COLUMN $COLUMN_DESCRIPTION_CLASS TEXT")
        }
        if (oldVersion < 16) {
            db?.execSQL("ALTER TABLE $TABLE_CLASSES ADD COLUMN $COLUMN_DAYS TEXT")
        }
        if (oldVersion < 17) {
            db?.execSQL("ALTER TABLE $TABLE_CLASSES ADD COLUMN $COLUMN_DURATION TEXT")
        }
        if (oldVersion < 19) {
            db?.execSQL("ALTER TABLE $TABLE_COURSES ADD COLUMN $COLUMN_FIRESTORE_ID TEXT")
        }
        if (oldVersion < 20) {
            db?.execSQL("ALTER TABLE $TABLE_CLASSES ADD COLUMN firestoreClassId TEXT")
        }
        if (oldVersion < 21) {
            db?.execSQL("ALTER TABLE $TABLE_CLASSES ADD COLUMN COLUMN_FIRESTORE_ID TEXT")
        }
        if (oldVersion < 22) {
            db?.execSQL("ALTER TABLE $TABLE_CLASSES ADD COLUMN COLUMN_DAYOFWEEK  TEXT")
        }
        if (oldVersion < 23) {
            db?.execSQL("ALTER TABLE $TABLE_CLASSES ADD COLUMN COLUMN_DURATION_COURSE INTEGER")
            db?.execSQL("ALTER TABLE $TABLE_CLASSES ADD COLUMN COLUMN_TIME TEXT")
            db?.execSQL("ALTER TABLE $TABLE_CLASSES ADD COLUMN COLUMN_PRICE REAL")
        }
    }


    fun logAllCourseIds() {
        val db = readableDatabase
        val cursor = db.query(TABLE_COURSES, arrayOf(COLUMN_ID, COLUMN_FIRESTORE_ID, COLUMN_NAME), null, null, null, null, null)


        val columnIndexId = cursor.getColumnIndex(COLUMN_ID)
        val columnIndexName = cursor.getColumnIndex(COLUMN_NAME)
        val columnIndexFirestoreId = cursor.getColumnIndex(COLUMN_FIRESTORE_ID)

        Log.d("CourseDebug", "Column Indexes - ID: $columnIndexId, Name: $columnIndexName, FirestoreID: $columnIndexFirestoreId")

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(columnIndexId)
                val name = cursor.getString(columnIndexName)
                val firebaseId = cursor.getString(columnIndexFirestoreId)
                Log.d("CourseInfo", "ID: $id, Name: $name, FirestoreID: $firebaseId")
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
    }


    // Course methods
    fun  addCourse(course: Course) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, course.name)
            put(COLUMN_CAPACITY, course.capacity)
            put(COLUMN_DESCRIPTION, course.description)
            put(COLUMN_TYPE, course.courseType)
            put(COLUMN_FIRESTORE_ID, course.firestoreId)
            put(COLUMN_DAYOFWEEK, course.dayOfWeek)
            put(COLUMN_PRICE, course.price)
            put(COLUMN_DURATION_COURSE, course.duration)
            put(COLUMN_TIME, course.time)
        }
        db.insert(TABLE_COURSES, null, values)

        db.close()
    }

    fun updateCourse(course: Course) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, course.name)
            put(COLUMN_CAPACITY, course.capacity)
            put(COLUMN_DESCRIPTION, course.description)
            put(COLUMN_TYPE, course.courseType)
            put(COLUMN_DAYOFWEEK, course.dayOfWeek)
            put(COLUMN_DURATION_COURSE, course.duration)
            put(COLUMN_TIME, course.time)
            put(COLUMN_PRICE, course.price)
        }
        db.update(TABLE_COURSES, values, "$COLUMN_ID = ?", arrayOf(course.id.toString()))
        db.close()
    }

    fun getAllCourses(): List<Course> {
        val courseList = mutableListOf<Course>()
        val db = readableDatabase
        val cursor = db.query(TABLE_COURSES, null, null, null, null, null, null)

        cursor.use {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                val dayOfWeek = cursor.getString((cursor.getColumnIndexOrThrow(COLUMN_DAYOFWEEK)))
                val capacity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAPACITY))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
                val courseType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE))
                val firestoreId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRESTORE_ID))
                val price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE))
                val duration = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DURATION_COURSE))
                val time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME))

                courseList.add(Course(id, name, dayOfWeek, time, duration, price,  capacity,  description, courseType, false,  firestoreId,))
            }
        }
        db.close()
        return courseList
    }

    fun getCourseById(courseId: Int): Course? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_COURSES,
            null,
            "$COLUMN_ID = ?",
            arrayOf(courseId.toString()),
            null,
            null,
            null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID))
                val name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME))
                val dayOfWeek = it.getString(it.getColumnIndexOrThrow(COLUMN_DAYOFWEEK))
                val time = it.getString(it.getColumnIndexOrThrow(COLUMN_TIME))
                val duration = it.getInt(it.getColumnIndexOrThrow(COLUMN_DURATION_COURSE))
                val price = it.getDouble(it.getColumnIndexOrThrow(COLUMN_PRICE))
                val capacity = it.getInt(it.getColumnIndexOrThrow(COLUMN_CAPACITY))
                val description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
                val courseType = it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE))


                val firestoreIdColumnIndex = it.getColumnIndex(COLUMN_FIRESTORE_ID)
                val firestoreId = if (firestoreIdColumnIndex != -1) {
                    it.getString(firestoreIdColumnIndex)
                } else {
                    null
                }


                Course(id, name, dayOfWeek, time, duration, price ,capacity, description, courseType, false, firestoreId)
            } else {
                null
            }
        }.also {
            db.close()
        }
    }



    fun deleteCourse(courseId: Int): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_COURSES, "$COLUMN_ID = ?", arrayOf(courseId.toString()))
        db.close()
        return result > 0
    }


    // Class methods
    fun logAllClassIds() {
        val db = readableDatabase
        val cursor = db.query(TABLE_CLASSES, arrayOf(COLUMN_CLASS_ID,
            COLUMN_COURSE_ID ,COLUMN_FIRESTORECLASS_ID, COLUMN_NAME_CLASS), null, null, null, null, null)


        val columnIndexId = cursor.getColumnIndex(COLUMN_CLASS_ID)
        val columnIndexCourseId = cursor.getColumnIndex(COLUMN_COURSE_ID)
        val columnIndexName = cursor.getColumnIndex(COLUMN_NAME_CLASS)
        val columnIndexFirestoreClassId = cursor.getColumnIndex(COLUMN_FIRESTORECLASS_ID)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(columnIndexId)
                val courseID = cursor.getInt(columnIndexCourseId)
                val name = cursor.getString(columnIndexName)
                val firebaseId = cursor.getString(columnIndexFirestoreClassId)
                Log.d("CourseInfo", "ID: $id, CourseID: $courseID ,Name: $name, FirestoreID: $firebaseId")
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
    }



    fun addClass(yogaClass: YogaClass): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COURSE_ID, yogaClass.courseId)
            put(COLUMN_TEACHER_NAME, yogaClass.teacherName)
            put(COLUMN_CLASS_DATE, yogaClass.classDate)
            put(COLUMN_NAME_CLASS, yogaClass.className)
            put(COLUMN_DESCRIPTION_CLASS, yogaClass.description)
            put(COLUMN_FIRESTORECLASS_ID, yogaClass.firestoreClassId)
            put(COLUMN_FIRESTORE_COURSE_ID, yogaClass.courseFirestoreId)

        }
        val result = db.insert("Classes", null, values)
        db.close()

        return result != -1L
    }

    fun getClassesByCourse(courseId: Int): List<YogaClass> {
        val classList = mutableListOf<YogaClass>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CLASSES,
            null,
            "$COLUMN_COURSE_ID = ?",
            arrayOf(courseId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            while (cursor.moveToNext()) {
                val classId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLASS_ID))
                val className = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_CLASS))
                val teacherName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEACHER_NAME))
                val classDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_DATE))
                val fee = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_FEE))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION_CLASS))
                val days = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS))
                val duration = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DURATION))
                val firestoreClassIdColumnIndex = it.getColumnIndex(COLUMN_FIRESTORECLASS_ID)
                val firestoreClassId = if (firestoreClassIdColumnIndex != -1) {
                    it.getString(firestoreClassIdColumnIndex)
                } else {
                    null // Nếu không có, trả về null
                }
                val firestoreCourseIdColumnIndex = it.getColumnIndex((COLUMN_FIRESTORE_COURSE_ID))
                val courseFirestoreId = if (firestoreCourseIdColumnIndex != -1) {
                    it.getString(firestoreClassIdColumnIndex)
                } else {
                    null // Nếu không có, trả về null
                }
                classList.add(YogaClass(classId, courseId, teacherName, classDate,  className, description, false,  firestoreClassId, courseFirestoreId)) // Thêm mô tả lớp học vào đối tượng YogaClass
            }
        }
        db.close()
        return classList
    }

    // Thêm phương thức cập nhật trong DatabaseHelper
    fun updateClass(yogaClass: YogaClass): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COURSE_ID, yogaClass.courseId)
            put(COLUMN_TEACHER_NAME, yogaClass.teacherName)
            put(COLUMN_CLASS_DATE, yogaClass.classDate)
            put(COLUMN_NAME_CLASS, yogaClass.className)
            put(COLUMN_DESCRIPTION_CLASS, yogaClass.description)
            put(COLUMN_FIRESTORE_COURSE_ID, yogaClass.courseFirestoreId)
        }

        val result = db.update(
            TABLE_CLASSES,
            values,
            "$COLUMN_CLASS_ID = ?",
            arrayOf(yogaClass.classId.toString())
        )
        db.close()
        return result > 0
    }



    fun getClassById(classId: Int): YogaClass? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CLASSES,
            null,
            "$COLUMN_CLASS_ID = ?",
            arrayOf(classId.toString()),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            val courseId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COURSE_ID))
            val teacherName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEACHER_NAME))
            val classDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_DATE))
            val className = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_CLASS))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION_CLASS))
            val courseFirestoreId = cursor.getString(cursor.getColumnIndexOrThrow(
                COLUMN_FIRESTORE_COURSE_ID))

            YogaClass(
                classId = classId,
                courseId = courseId,
                teacherName = teacherName,
                classDate = classDate,
                className = className,
                description = description,
                courseFirestoreId = courseFirestoreId
            )
        } else {
            null
        }.also {
            cursor.close()
            db.close()
        }
    }



    fun deleteClass(classId: Int): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_CLASSES, "$COLUMN_CLASS_ID = ?", arrayOf(classId.toString()))
        db.close()
        return result > 0
    }

    fun updateClassFirestoreId(className: String, firestoreClassId: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("firestoreClassId", firestoreClassId)
        }
        val result = db.update("classes", contentValues, "className = ?", arrayOf(className))
        return result > 0
    }


    fun updateClassSyncStatus(classId: Int, firestoreClassId: String?) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FIRESTORECLASS_ID, firestoreClassId)
        }

        val result = db.update(
            TABLE_CLASSES,
            values,
            "$COLUMN_CLASS_ID = ?",
            arrayOf(classId.toString())
        )
        if (result > 0) {
            Log.d("Database", "Class sync status updated successfully for classId: $classId")
        } else {
            Log.d("Database", "Failed to update class sync status for classId: $classId")
        }
        db.close()
    }


    fun resetDatabase() {
        writableDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_CLASSES")
        writableDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_COURSES")
        onCreate(writableDatabase)
    }
}

