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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
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

                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            refreshTimetableIfStale(db);
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

    private static List<String> queryStrings(SupportSQLiteDatabase db, String sql, Object... args) {
        List<String> values = new ArrayList<>();
        Cursor c = db.query(sql, args);
        while (c.moveToNext()) values.add(c.getString(0));
        c.close();
        return values;
    }

    // ── Seed data ────────────────────────────────────────────────────

    private static void seedDatabase(SupportSQLiteDatabase db) {
        String pwdHash = HashUtils.sha256("password");

        // --- Users ---
        // Fictional demo accounts. Students start with no card linked: the lecturer
        // links a card by scanning it in NfcScanActivity.
        exec(db,
            "INSERT INTO users (student_or_staff_id,name,password_hash,role,card_lid) VALUES (?,?,?,?,?)",
            "1000001", "Dr. Alice Tan", pwdHash, Constants.ROLE_LECTURER, null);
        exec(db,
            "INSERT INTO users (student_or_staff_id,name,password_hash,role,card_lid) VALUES (?,?,?,?,?)",
            "1000002", "Dr. Bob Lim", pwdHash, Constants.ROLE_LECTURER, null);
        String[][] students = {
            {"1006001", "Chen Wei"},
            {"1006002", "Priya Sharma"},
            {"1006003", "Marcus Lee"},
            {"1006004", "Aisha Rahman"},
            {"1006005", "Daniel Koh"}
        };
        for (String[] st : students) {
            exec(db,
                "INSERT INTO users (student_or_staff_id,name,password_hash,role,card_lid) VALUES (?,?,?,?,?)",
                st[0], st[1], pwdHash, Constants.ROLE_STUDENT, null);
        }
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
        seedTimetable(db);
    }

    /**
     * Demo timetable: 48 lessons per module (3 per week × 16 weeks), plus demo
     * attendance for lessons already past. Reads the modules and enrollments
     * back from the database so it can be rebuilt on its own.
     */
    private static void seedTimetable(SupportSQLiteDatabase db) {
        long now = System.currentTimeMillis();
        List<String> moduleIds = queryStrings(db, "SELECT moduleId FROM modules ORDER BY moduleId");
        if (moduleIds.size() < 3) return;
        long moduleId1 = Long.parseLong(moduleIds.get(0));
        long moduleId2 = Long.parseLong(moduleIds.get(1));
        long moduleId3 = Long.parseLong(moduleIds.get(2));
        String enrolledSql = "SELECT student_id FROM enrollments WHERE module_id = ? ORDER BY student_id";
        // The term is anchored to today so the demo always looks current:
        // week 1 starts 14 Mondays before this week, which puts today in week 15,
        // late enough that the attendance overview shows a spread of colours.
        Calendar today = Calendar.getInstance();
        int todayOffset = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7;  // days since Monday
        Calendar termStart = (Calendar) today.clone();
        termStart.add(Calendar.DAY_OF_YEAR, -todayOffset - 14 * 7);
        // 50.001 always has a lesson today, so a visitor can start
        // an attendance session straight away.
        int[] mod1Days = sortedDays(todayOffset, (todayOffset + 2) % 7, (todayOffset + 4) % 7);
        Object[][] modCfg = {
            {moduleId1, "10:00", "12:00", "LT1",     mod1Days},
            {moduleId2, "14:00", "16:00", "LT2",     new int[]{0, 2, 3}},  // Mon, Wed, Thu
            {moduleId3, "09:00", "10:30", "ESC Lab", new int[]{1, 3, 4}}   // Tue, Thu, Fri
        };
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayStr = sdf.format(today.getTime());
        for (Object[] cfg : modCfg) {
            long modId     = (long)   cfg[0];
            String start   = (String) cfg[1];
            String end     = (String) cfg[2];
            String venue   = (String) cfg[3];
            int[] days     = (int[])  cfg[4];
            String[] enrolled = queryStrings(db, enrolledSql, modId).toArray(new String[0]);
            Calendar weekStart = (Calendar) termStart.clone();
            int lessonNum = 1;
            for (int week = 1; week <= 16; week++) {
                String type;
                if      (week <= 7)  type = Constants.LESSON_TYPE_LECTURE;
                else if (week == 8)  type = Constants.LESSON_TYPE_RECESS;
                else if (week <= 15) type = Constants.LESSON_TYPE_LECTURE;
                else                 type = Constants.LESSON_TYPE_EXAM;
                for (int offset : days) {
                    Calendar cal = (Calendar) weekStart.clone();
                    cal.add(Calendar.DAY_OF_YEAR, offset);
                    String date = sdf.format(cal.getTime());
                    exec(db,
                        "INSERT INTO lessons (module_id,lesson_number,date,start_time,end_time,venue,type,is_active)" +
                        " VALUES (?,?,?,?,?,?,?,0)",
                        modId, lessonNum, date, start, end, venue, type);
                    long lessonId = lastInsertRowId(db);
                    // --- Pre-existing Attendance (demo data) for lessons before today ---
                    if (date.compareTo(todayStr) < 0 && !Constants.LESSON_TYPE_RECESS.equals(type)) {
                        seedAttendance(db, lessonId, lessonNum, enrolled, now);
                    }
                    lessonNum++;
                }
                weekStart.add(Calendar.WEEK_OF_YEAR, 1);
            }
        }
    }

    /**
     * The timetable is generated around the day it was seeded, so after a few
     * days the demo has no lesson to start attendance on. When 50.001 has no
     * lesson today, rebuild the lessons around today. Accounts, enrollments and
     * linked cards are kept; demo attendance is regenerated.
     */
    private static void refreshTimetableIfStale(SupportSQLiteDatabase db) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        List<String> todayLessons = queryStrings(db,
            "SELECT lessonId FROM lessons WHERE date = ? " +
            "AND module_id = (SELECT MIN(moduleId) FROM modules)", today);
        if (!todayLessons.isEmpty()) return;
        db.beginTransaction();
        try {
            exec(db, "DELETE FROM attendance");
            exec(db, "DELETE FROM lessons");
            seedTimetable(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static int[] sortedDays(int... days) {
        int[] sorted = days.clone();
        Arrays.sort(sorted);
        return sorted;
    }

    /**
     * Gives each demo student a different attendance rate so the overview
     * bars show green, amber and red: they attend roughly 100%, 90%, 75%, 65%
     * and 50% of past lessons.
     */
    private static void seedAttendance(SupportSQLiteDatabase db, long lessonId, int lessonNum,
                                       String[] studentIds, long timestamp) {
        int[] missEvery = {0, 10, 4, 3, 2};
        for (int i = 0; i < studentIds.length; i++) {
            boolean missed = missEvery[i] > 0 && lessonNum % missEvery[i] == 0;
            if (missed) continue;
            exec(db, "INSERT OR IGNORE INTO attendance (lesson_id,student_id,timestamp,method) VALUES (?,?,?,?)",
                lessonId, studentIds[i], timestamp, Constants.ATTENDANCE_METHOD_NFC);
        }
    }
}
