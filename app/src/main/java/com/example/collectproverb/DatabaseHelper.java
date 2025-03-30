package com.example.collectproverb;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ProverbDB";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_NAME = "proverbs";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PROVERB = "proverb";
    private static final String COLUMN_SPEAKER = "speaker";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_TYPE_ID = "type_id";
    private static final String COLUMN_DRAWABLE_PATH = "drawable_path";
    private static final String COLUMN_DRAWABLE_BOOl = "drawable_bool";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PROVERB + " TEXT NOT NULL, " +
                COLUMN_SPEAKER + " TEXT, " +
                COLUMN_TYPE + " TEXT NOT NULL, " +
                COLUMN_TYPE_ID + " INTEGER NOT NULL, " +
                COLUMN_DRAWABLE_PATH + " INTEGER NOT NULL," +
                COLUMN_DRAWABLE_BOOl + " INTEGER NOT NULL," +
                COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createTableQuery);

        // 初期データを挿入
        insertInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // 初期データを挿入するメソッド
    private void insertInitialData(SQLiteDatabase db) {
        insertProverb(db, "成功する秘訣は、成功するまでやり続けることである。", "トーマス・エジソン", "positive", 1, R.drawable.red_edison, 0);
        insertProverb(db, "行動しなければ何も変わらない。", "ベンジャミン・フランクリン", "positive", 2, R.drawable.red_benjamin, 0);
        insertProverb(db, "追い続ける勇気があるのなら、全ての夢は必ず実現する。", "ウォルト・ディズニー", "positive", 3, R.drawable.red_disney, 0);
        insertProverb(db, "一番大事なことは、自分の心と直感に従う勇気を持つことだ。", "スティーブ・ジョブズ", "positive", 4, R.drawable.red_jobs, 0);

        insertProverb(db, "失敗と不可能とは違う。", "スーザン・B・アンソニー", "encouragement", 1, R.drawable.green_anthony, 0);
        insertProverb(db, "上を向いている限り、絶対にいいことがある。", "三浦知良", "encouragement", 2, R.drawable.green_kingkaz, 0);
        insertProverb(db, "いつかこの日さえも、楽しく思い出すことがあるだろう。", "ウェルギリウス", "encouragement", 3, R.drawable.green_vergilius, 0);
        insertProverb(db, "不良とは、優しさの事ではないかしら。", "太宰治", "encouragement", 4, R.drawable.green_dazai, 0);

        insertProverb(db, "ことしは、計画的になまけていたんだ。", "野比のび太", "rest", 1, R.drawable.blue_nobi, 0);
        insertProverb(db, "もっと早く終わるように、少し休め。", "ジョージ・ハーバート", "rest", 2, R.drawable.blue_herbert, 0);
        insertProverb(db, "明日が素晴らしい日だといけないから、うんと休息するのさ。", "スヌーピー", "rest", 3, R.drawable.blue_snoopy, 0);
        insertProverb(db, "疑う余地のない純粋の歓びの一つは、勤勉の後の休息である。", "イマヌエル・カント", "rest", 4, R.drawable.blue_kant, 0);
    }

    // 格言を挿入するメソッド
    private void insertProverb(SQLiteDatabase db, String proverb, String speaker, String type, int typeId, int drawable_path, int drawable_bool) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVERB, proverb);
        values.put(COLUMN_SPEAKER, speaker);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_TYPE_ID, typeId);
        values.put(COLUMN_DRAWABLE_PATH, drawable_path);
        values.put(COLUMN_DRAWABLE_BOOl, drawable_bool);
        db.insert(TABLE_NAME, null, values);
    }

    // 指定されたタイプからランダムな格言を取得するメソッド
    public String getRandomProverbByType(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME +
                        " WHERE type = ? ORDER BY RANDOM() LIMIT 1",
                new String[]{type});

        if (cursor.moveToFirst()) {
            @SuppressLint("Range") String proverb = cursor.getString(cursor.getColumnIndex(COLUMN_PROVERB));
            @SuppressLint("Range") String speaker = cursor.getString(cursor.getColumnIndex(COLUMN_SPEAKER));
            cursor.close();
            db.close();
            return proverb + " - " + speaker;
        }

        cursor.close();
        db.close();
        return null;
    }

    // 取得した格言のパスを取得するメソッド
    public Integer getDrawablePathBySpeaker(String speaker) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME +
                        " WHERE speaker = ?",
                new String[]{speaker});

        if (cursor.moveToFirst()) {
            @SuppressLint("Range") Integer drawable_path = cursor.getInt(cursor.getColumnIndex(COLUMN_DRAWABLE_PATH));
            cursor.close();
            db.close();
            return drawable_path;
        }

        cursor.close();
        db.close();
        return null;
    }

    // 取得したパスのboolを変更するメソッド
    public void toggleDrawableBoolByPath(int path) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        int currentBoolValue = -1; // 初期値として -1 (不正な値) を設定

        try {
            // 現在の drawable_bool の値を取得
            cursor = db.rawQuery("SELECT drawable_bool FROM " + TABLE_NAME + " WHERE drawable_path = ?",
                    new String[]{String.valueOf(path)});

            if (cursor.moveToFirst()) { // レコードが存在する場合
                currentBoolValue = cursor.getInt(0); // drawable_bool の値を取得
            }
        } finally {
            if (cursor != null) {
                cursor.close(); // Cursor を閉じる
            }
        }

        // 取得した値が 0 の場合のみ 1 に変更
        if (currentBoolValue == 0) {
            ContentValues values = new ContentValues();
            values.put("drawable_bool", 1); // 0 → 1 に変更

            int rowsAffected = db.update(TABLE_NAME, values, "drawable_path = ?",
                    new String[]{String.valueOf(path)});

            Log.d("DB_UPDATE", "Rows affected: " + rowsAffected);
        } else {
            Log.d("DB_UPDATE", "No update performed. Current value: " + currentBoolValue);
        }

        db.close(); // DB を閉じる
    }

    // idを取得するメソッド
    public Integer getIdByPath(int path) {
        SQLiteDatabase db = this.getReadableDatabase();

        try (Cursor cursor = db.rawQuery("SELECT id FROM " + TABLE_NAME + " WHERE drawable_path = ?",
                new String[]{String.valueOf(path)})) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        // Cursor を閉じる

        db.close();
        return null;

    }

}
