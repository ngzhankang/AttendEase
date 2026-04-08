package com.example.attendease.data.local;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.attendease.util.Constants;
import com.example.attendease.util.HashUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

//  basically you instantiate a local database in the phone for persistent data
@Database(entities = {User.class, Module.class, Lesson.class, Enrollment.class, Attendance.class},
          version = 3, exportSchema = false)

//  DAO = Data Access Objects = Methods for app to interact with the data in the `user` table
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract ModuleDao moduleDao();
    public abstract LessonDao lessonDao();
    public abstract EnrollmentDao enrollmentDao();
    public abstract AttendanceDao attendanceDao();

    //  volatile = non access modifier to ensure values are consistent across threads. prevents cache issue
    private static volatile AppDatabase INSTANCE;

    //  this part is to instantiate the local database on the phone
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "attendease_db"
                    )
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // Use the raw db directly — calling DAOs here would
                            // re-enter getDatabase() and throw IllegalStateException.
                            seedDatabase(db);
                        }
                    }).build();
                }
            }
        }
        return INSTANCE;
    }

    // ── Seed helpers ─────────────────────────────────────────────────

    private static long lastInsertRowId(SupportSQLiteDatabase db) {
        Cursor c = db.query("SELECT last_insert_rowid()", new Object[0]);
        c.moveToFirst();
        long id = c.getLong(0);
        c.close();
        return id;
    }

    private static void exec(SupportSQLiteDatabase db, String sql, Object... args) {
        db.execSQL(sql, args);
    }

    // ── Seed data ────────────────────────────────────────────────────

    private static void seedDatabase(SupportSQLiteDatabase db) {
        String pwdHash = HashUtils.sha256("password");
        long now = System.currentTimeMillis();

        // --- Users ---
        exec(db,
            "INSERT INTO users (student_or_staff_id,name,password_hash,role,card_lid) VALUES (?,?,?,?,?)",
            "1000001", "Dr. Alice Tan", pwdHash, Constants.ROLE_LECTURER, null);
        exec(db,
            "INSERT INTO users (student_or_staff_id,name,password_hash,role,card_lid) VALUES (?,?,?,?,?)",
            "1000002", "Dr. Bob Lim", pwdHash, Constants.ROLE_LECTURER, null);
        exec(db,
            "INSERT INTO users (student_or_staff_id,name,password_hash,role,card_lid) VALUES (?,?,?,?,?)",
            "1006001", "Chen Wei", pwdHash, Constants.ROLE_STUDENT, "1188110000101141");
        exec(db,
            "INSERT INTO users (student_or_staff_id,name,password_hash,role,card_lid) VALUES (?,?,?,?,?)",
            "1006002", "Priya Sharma", pwdHash, Constants.ROLE_STUDENT, "22AA3B0000201242");
        exec(db,
            "INSERT INTO users (student_or_staff_id,name,password_hash,role,card_lid) VALUES (?,?,?,?,?)",
            "1006003", "Marcus Ng", pwdHash, Constants.ROLE_STUDENT, "33BB4C0000301343");
        exec(db,
            "INSERT INTO users (student_or_staff_id,name,password_hash,role,card_lid) VALUES (?,?,?,?,?)",
            "1006004", "Aisha Rahman", pwdHash, Constants.ROLE_STUDENT, "44CC5D0000401444");
        exec(db,
            "INSERT INTO users (student_or_staff_id,name,password_hash,role,card_lid) VALUES (?,?,?,?,?)",
            "1006005", "Raj Patel", pwdHash, Constants.ROLE_STUDENT, "55DD6E0000501545");

        // --- Modules ---
        exec(db,
            "INSERT INTO modules (module_name,lecturer_id) VALUES (?,?)",
            "50.001 Information Systems", "1000001");
        long moduleId1 = lastInsertRowId(db);

        exec(db,
            "INSERT INTO modules (module_name,lecturer_id) VALUES (?,?)",
            "50.002 Computation Structures", "1000001");
        long moduleId2 = lastInsertRowId(db);

        exec(db,
            "INSERT INTO modules (module_name,lecturer_id) VALUES (?,?)",
            "50.004 Algorithms", "1000002");
        long moduleId3 = lastInsertRowId(db);

        // --- Enrollments ---
        String[] allStudents  = {"1006001","1006002","1006003","1006004","1006005"};
        String[] partStudents = {"1006001","1006002","1006003","1006004"};

        for (String sid : allStudents) {
            exec(db, "INSERT INTO enrollments (student_id,module_id) VALUES (?,?)", sid, moduleId1);
            exec(db, "INSERT INTO enrollments (student_id,module_id) VALUES (?,?)", sid, moduleId2);
        }
        for (String sid : partStudents) {
            exec(db, "INSERT INTO enrollments (student_id,module_id) VALUES (?,?)", sid, moduleId3);
        }

        // --- Lessons: 48 per module (Mon/Wed/Thu × 16 weeks from 2026-01-12) ---
        Object[][] modCfg = {
            {moduleId1, "10:00", "14:30", "LT1"},
            {moduleId2, "14:00", "16:00", "LT2"},
            {moduleId3, "20:45", "10:30", "ESC Lab"}
        };

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        int[] dayOffsets = {0, 2, 3, 6}; // Mon, Wed, Thu relative to week start (Monday)

        for (Object[] cfg : modCfg) {
            long modId     = (long)   cfg[0];
            String start   = (String) cfg[1];
            String end     = (String) cfg[2];
            String venue   = (String) cfg[3];

            Calendar weekStart = Calendar.getInstance();
            weekStart.set(2026, Calendar.JANUARY, 12);

            int lessonNum = 1;
            for (int week = 1; week <= 16; week++) {
                String type;
                if      (week <= 7)  type = Constants.LESSON_TYPE_LECTURE;
                else if (week == 8)  type = Constants.LESSON_TYPE_RECESS;
                else if (week <= 15) type = Constants.LESSON_TYPE_LECTURE;
                else                 type = Constants.LESSON_TYPE_EXAM;

                for (int offset : dayOffsets) {
                    Calendar cal = (Calendar) weekStart.clone();
                    cal.add(Calendar.DAY_OF_YEAR, offset);
                    String date = sdf.format(cal.getTime());
                    exec(db,
                        "INSERT INTO lessons (module_id,lesson_number,date,start_time,end_time,venue,type,is_active)" +
                        " VALUES (?,?,?,?,?,?,?,0)",
                        modId, lessonNum, date, start, end, venue, type);
                    lessonNum++;
                }
                weekStart.add(Calendar.WEEK_OF_YEAR, 1);
            }
        }

        // --- Pre-existing Attendance (demo data) ---
        // Lesson IDs: module1 → 1–48, module2 → 49–96, module3 → 97–144
        // 1006001: attended lessons 1–30 of module1, 49–73 of module2
        for (long lid = 1; lid <= 30; lid++) {
            exec(db, "INSERT OR IGNORE INTO attendance (lesson_id,student_id,timestamp,method) VALUES (?,?,?,?)",
                lid, "1006001", now, Constants.ATTENDANCE_METHOD_NFC);
        }
        for (long lid = 49; lid <= 73; lid++) {
            exec(db, "INSERT OR IGNORE INTO attendance (lesson_id,student_id,timestamp,method) VALUES (?,?,?,?)",
                lid, "1006001", now, Constants.ATTENDANCE_METHOD_NFC);
        }
        // 1006002: attended lessons 1–38 of module1
        for (long lid = 1; lid <= 38; lid++) {
            exec(db, "INSERT OR IGNORE INTO attendance (lesson_id,student_id,timestamp,method) VALUES (?,?,?,?)",
                lid, "1006002", now, Constants.ATTENDANCE_METHOD_NFC);
        }
    }
}
