package com.example.collectproverb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ProverbDB";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "proverbs";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PROVERB = "proverb";
    private static final String COLUMN_SPEAKER = "speaker";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_TYPE_ID = "type_id";
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
        insertProverb(db, "成功する秘訣は、成功するまでやり続けることである。", "トーマス・エジソン", "positive", 1);
        insertProverb(db, "行動しなければ何も変わらない。", "ベンジャミン・フランクリン", "positive", 2);
        insertProverb(db, "追い続ける勇気があるのなら、全ての夢は必ず実現する。", "ウォルト・ディズニー", "positive", 3);
        insertProverb(db, "一番大事なことは、自分の心と直感に従う勇気を持つことだ。", "ウォルト・ディズニー", "positive", 4);

        insertProverb(db, "失敗と不可能とは違う。", "スーザン・B・アンソニー", "encouragement", 1);
        insertProverb(db, "上を向いている限り、絶対にいいことがある。", "三浦知良", "encouragement", 2);
        insertProverb(db, "いつかこの日さえも、楽しく思い出すことがあるだろう。", "ウェルギリウス", "encouragement", 3);
        insertProverb(db, "不良とは、優しさの事ではないかしら。", "太宰治", "encouragement", 4);

        insertProverb(db, "休むことも大切だ。焦らなくていい。", "老子", "rest", 1);
        insertProverb(db, "休息なしに成長なし。", "レオナルド・ダ・ヴィンチ", "rest", 2);
        insertProverb(db, "明日が素晴らしい日だといけないから、うんと休息するのさ。", "スヌーピー", "rest", 3);
        insertProverb(db, "疑う余地のない純粋の歓びの一つは、勤勉の後の休息である。", "イマヌエル・カント", "rest", 4);
    }

    // 格言を挿入するメソッド
    private void insertProverb(SQLiteDatabase db, String proverb, String speaker, String type, int typeId) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVERB, proverb);
        values.put(COLUMN_SPEAKER, speaker);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_TYPE_ID, typeId);
        db.insert(TABLE_NAME, null, values);
    }

    // 指定されたタイプからランダムな格言を取得するメソッド
    public String getRandomProverbByType(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME +
                        " WHERE type = ? ORDER BY RANDOM() LIMIT 1",
                new String[]{type});

        if (cursor.moveToFirst()) {
            String proverb = cursor.getString(cursor.getColumnIndex(COLUMN_PROVERB));
            String speaker = cursor.getString(cursor.getColumnIndex(COLUMN_SPEAKER));
            cursor.close();
            db.close();
            return proverb + " - " + speaker;
        }

        cursor.close();
        db.close();
        return null;
    }
}
