package com.collectproverb.collectproverb;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ProverbDB";
    private static final int DATABASE_VERSION = 41;
    private static final String Proverb_TABLE_NAME = "proverbs";
    private static final String Button_Bool_Table_Name = "button_bool";
    private static final String Daily_Proverb_Table_Name = "daily_proverb";
    private static final String Proverb_TIMESTAMP_Trigger = "update_proverb_timestamp";
    private static final String Bool_TIMESTAMP_Trigger = "update_bool_timestamp";
    private static final Integer FalseBool = 0;
    private static final Integer EnableBool = 1;
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PROVERB = "proverb";
    private static final String COLUMN_SPEAKER = "speaker";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_TYPE_ID = "type_id";
    private static final String COLUMN_COUNT = "count";
    private static final String COLUMN_DRAWABLE_PATH = "drawable_path";
    private static final String COLUMN_DRAWABLE_BOOl = "drawable_bool";
    private static final String COLUMN_BUTTON_BOOL = "bool";
    private static final String COLUMN_GET_TIME = "get_time";
    private static final String COLUMN_FIRST_GET_TIME = "first_get_time";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String KEY_ALIAS = "MyKeyAlias";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final KeyStore keyStore;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize KeyStore", e);
        }
        createKey();
    }

    private void createKey() {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

                KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(false)
                        .build();

                keyGenerator.init(keyGenParameterSpec);
                keyGenerator.generateKey();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create key", e);
        }
    }

    private String encrypt(String data) {
        try {
            // 固定IVを指定
            byte[] fixedIv = new byte[12]; // GCMでは通常12バイトのIVを使用
            Arrays.fill(fixedIv, (byte) 0); // 固定値を入れる（例えばゼロ）

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // 固定のIVを使って暗号化
            GCMParameterSpec spec = new GCMParameterSpec(128, fixedIv);
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(KEY_ALIAS, null), spec);

            byte[] encryptedData = cipher.doFinal(data.getBytes());

            // IVと暗号化データを結合
            byte[] combined = new byte[fixedIv.length + encryptedData.length];
            System.arraycopy(fixedIv, 0, combined, 0, fixedIv.length);
            System.arraycopy(encryptedData, 0, combined, fixedIv.length, encryptedData.length);

            // 結果をBase64でエンコードして返す（改行なし）
            return Base64.encodeToString(combined, Base64.NO_WRAP).trim();
        } catch (Exception e) {
            Log.e("Encryption", "Failed to encrypt data", e);
            return null;
        }
    }


    private String decrypt(String encryptedData) {
        try {
            if (encryptedData == null || encryptedData.isEmpty()) {
                Log.e("Decryption", "Encrypted data is null or empty");
                return null;
            }

            // Base64でデコードしてデータを取得
            byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT);

            // 暗号化データ長が適切かチェック
            if (combined.length < 12) {
                Log.e("Decryption", "Invalid encrypted data length: " + combined.length);
                return null;
            }

            // 最初の12バイトがIV（固定IV）
            byte[] iv = new byte[12];
            System.arraycopy(combined, 0, iv, 0, 12);

            // 残りのデータが暗号化されたバイト列
            byte[] encryptedBytes = new byte[combined.length - 12];
            System.arraycopy(combined, 12, encryptedBytes, 0, combined.length - 12);

            // 復号化のためのCipherを設定
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // GCMParameterSpecを使って復号化
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, keyStore.getKey(KEY_ALIAS, null), spec);

            // データを復号化
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            // 復号化されたバイト配列を文字列に変換して返す
            return new String(decryptedBytes);
        } catch (Exception e) {
            Log.e("Decryption", "Failed to decrypt data", e);
            return null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createProverbTableQuery = "CREATE TABLE " + Proverb_TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PROVERB + " TEXT NOT NULL, " +
                COLUMN_SPEAKER + " TEXT NOT NULL, " +
                COLUMN_TYPE + " TEXT NOT NULL, " +
                COLUMN_TYPE_ID + " INTEGER NOT NULL, " +
                COLUMN_COUNT + " INTEGER NOT NULL, " +
                COLUMN_DRAWABLE_PATH + " INTEGER NOT NULL," +
                COLUMN_DRAWABLE_BOOl + " INTEGER NOT NULL," +
                COLUMN_FIRST_GET_TIME + " TEXT NOT NULL," +
                COLUMN_CREATED_AT + " TEXT DEFAULT (DATETIME('now', '+9 hours')), " +
                COLUMN_UPDATED_AT + " TEXT DEFAULT (DATETIME('now', '+9 hours')))";
        db.execSQL(createProverbTableQuery);

        String createButtonBoolTableQuery = "CREATE TABLE " + Button_Bool_Table_Name + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_BUTTON_BOOL + " INTEGER NOT NULL, " +
                COLUMN_CREATED_AT + " TEXT DEFAULT (DATETIME('now', '+9 hours')), " +
                COLUMN_UPDATED_AT + " TEXT DEFAULT (DATETIME('now', '+9 hours')))";
        db.execSQL(createButtonBoolTableQuery);

        String createDailyProverbTableQuery = "CREATE TABLE " + Daily_Proverb_Table_Name + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PROVERB + " TEXT NOT NULL, " +
                COLUMN_SPEAKER + " TEXT NOT NULL, " +
                COLUMN_GET_TIME + " TEXT NOT NULL, " +
                COLUMN_CREATED_AT + " TEXT DEFAULT (DATETIME('now', '+9 hours')), " +
                COLUMN_UPDATED_AT + " TEXT DEFAULT (DATETIME('now', '+9 hours')))";
        db.execSQL(createDailyProverbTableQuery);


        // proverbsテーブルのupdated_atを自動更新するトリガーを設定
        String createProverbTriggerQuery = "CREATE TRIGGER " + Proverb_TIMESTAMP_Trigger + " "
                + "AFTER UPDATE ON " + Proverb_TABLE_NAME + " "
                + "FOR EACH ROW "
                + "WHEN OLD." + COLUMN_UPDATED_AT + " = NEW." + COLUMN_UPDATED_AT + " "
                + "BEGIN "
                + "UPDATE " + Proverb_TABLE_NAME + " SET " + COLUMN_UPDATED_AT + " = DATETIME('now', '+9 hours') "
                + "WHERE " + COLUMN_ID + " = OLD." + COLUMN_ID + "; "
                + "END;";
        db.execSQL(createProverbTriggerQuery);

        // button_boolテーブルのupdated_atを自動更新するトリガーを設定
        String createButtonBoolTriggerQuery = "CREATE TRIGGER " + Bool_TIMESTAMP_Trigger + " "
                + "AFTER UPDATE ON " + Button_Bool_Table_Name + " "
                + "FOR EACH ROW "
                + "WHEN OLD." + COLUMN_UPDATED_AT + " = NEW." + COLUMN_UPDATED_AT + " "
                + "BEGIN "
                + "UPDATE " + Button_Bool_Table_Name + " SET " + COLUMN_UPDATED_AT + " = DATETIME('now', '+9 hours') "
                + "WHERE " + COLUMN_ID + " = OLD." + COLUMN_ID + "; "
                + "END;";
        db.execSQL(createButtonBoolTriggerQuery);

        // Proverbの初期データを挿入
        insertInitialProverbsData(db);
        // BOOLの初期データを挿入
        insertInitialBoolData(db);
        // 昨日の日付で初期データを挿入
        insertInitialDailyProverbsData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 古いテーブルを削除
        db.execSQL("DROP TABLE IF EXISTS " + Proverb_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Button_Bool_Table_Name);
        db.execSQL("DROP TABLE IF EXISTS " + Daily_Proverb_Table_Name);
        db.execSQL("DROP TABLE IF EXISTS " + Proverb_TIMESTAMP_Trigger);
        db.execSQL("DROP TABLE IF EXISTS " + Bool_TIMESTAMP_Trigger);

        // テーブル再作成
        onCreate(db);
    }

    // 格言を挿入するメソッド
    private void insertProverb(SQLiteDatabase db, String proverb, String speaker, String type, int typeId, int count, int drawable_path, int drawable_bool, String first_get_time) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVERB, encrypt(proverb));
        values.put(COLUMN_SPEAKER, encrypt(speaker));
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_TYPE_ID, typeId);
        values.put(COLUMN_COUNT, count);
        values.put(COLUMN_DRAWABLE_PATH, drawable_path);
        values.put(COLUMN_DRAWABLE_BOOl, drawable_bool);
        values.put(COLUMN_FIRST_GET_TIME, first_get_time != null ? encrypt(first_get_time) : null);
        db.insert(Proverb_TABLE_NAME, null, values);
    }

    // 初期データを挿入するメソッド
    private void insertInitialProverbsData(SQLiteDatabase db) {
        insertProverb(db, "成功する秘訣は、\n成功するまでやり続けることである。", "トーマス・エジソン", "positive", 1, 0, R.drawable.red_edison, 0, "yet");
        insertProverb(db, "行動しなければ何も変わらない。", "ベンジャミン・フランクリン", "positive", 2, 0, R.drawable.red_benjamin, 0, "yet");
        insertProverb(db, "追い続ける勇気があるのなら、\n全ての夢は必ず実現する。", "ウォルト・ディズニー", "positive", 3, 0, R.drawable.red_disney, 0, "yet");
        insertProverb(db, "一番大事なことは、\n自分の心と直感に従う勇気を持つことだ。", "スティーブ・ジョブズ", "positive", 4, 0, R.drawable.red_jobs, 0, "yet");

        insertProverb(db, "失敗と不可能とは違う。", "スーザン・B・アンソニー", "encouragement", 1, 0, R.drawable.green_anthony, 0, "yet");
        insertProverb(db, "上を向いている限り、\n絶対にいいことがある。", "三浦知良", "encouragement", 2, 0, R.drawable.green_kingkaz, 0, "yet");
        insertProverb(db, "いつかこの日さえも、\n楽しく思い出すことがあるだろう。", "ウェルギリウス", "encouragement", 3, 0, R.drawable.green_vergilius, 0, "yet");
        insertProverb(db, "不良とは、\n優しさの事ではないかしら。", "太宰治", "encouragement", 4, 0, R.drawable.green_dazai, 0, "yet");

        insertProverb(db, "ことしは、\n計画的になまけていたんだ。", "野比のび太", "rest", 1, 0, R.drawable.blue_nobi, 0, "yet");
        insertProverb(db, "もっと早く終わるように、\n少し休め。", "ジョージ・ハーバート", "rest", 2, 0, R.drawable.blue_herbert, 0, "yet");
        insertProverb(db, "明日が素晴らしい日だといけないから、\nうんと休息するのさ。", "スヌーピー", "rest", 3, 0, R.drawable.blue_snoopy, 0, "yet");
        insertProverb(db, "疑う余地のない純粋の歓びの一つは、\n勤勉の後の休息である。", "イマヌエル・カント", "rest", 4, 0, R.drawable.blue_kant, 0, "yet");
    }

    // ボタンboolを挿入
    private void insertInitialBoolData(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_BUTTON_BOOL, EnableBool);
        db.insert(Button_Bool_Table_Name, null, values);
    }


    //ボタンboolを変更(有効化)
    public void EnableButtonBool(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_BUTTON_BOOL, EnableBool);
        db.update(Button_Bool_Table_Name, values, null, null);
    }

    //ボタンboolを変更(無効化)
    public void FalseButtonBool(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_BUTTON_BOOL, FalseBool);
        db.update(Button_Bool_Table_Name, values, null, null);
    }

    //ボタンboolを取得するメソッド
    public int getButtonBool() {
        SQLiteDatabase db = this.getReadableDatabase();
        int buttonBool = -1; // 初期値 (-1: 取得失敗時の値)

        // クエリを実行
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_BUTTON_BOOL + " FROM " + Button_Bool_Table_Name, null);

        // データが存在すれば取得
        if (cursor.moveToFirst()) {
            buttonBool = cursor.getInt(0); // 0番目のカラム (COLUMN_BUTTON_BOOL) の値を取得
        }

        cursor.close(); // カーソルを閉じる

        return buttonBool; // 取得した値を返す
    }

    // カウントアップ関数
    public void CountUp(Integer id) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_COUNT + " FROM " + Proverb_TABLE_NAME + " WHERE id = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();

        // カウントアップ処理
        db = this.getWritableDatabase();
        db.execSQL("UPDATE " + Proverb_TABLE_NAME + " SET " + COLUMN_COUNT + " = ? WHERE id = ?", new Object[]{count + 1, id});
    }

    //ボタンboolのupdated_atを取得するメソッド
    @SuppressLint("Range")
    public String getBoolUpdatedAt() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String rawDate = null;

        try {
            cursor = db.rawQuery("SELECT " + COLUMN_UPDATED_AT + " FROM " + Button_Bool_Table_Name, null);

            if (cursor.moveToFirst()) {
                // 日付文字列を取得
                rawDate = cursor.getString(cursor.getColumnIndex(COLUMN_UPDATED_AT));
            }
        } finally {
            // リソース解放を確実に実行
            if (cursor != null) cursor.close();
        }

        // 日付加工処理（3つの方法から選択）
        return rawDate;
    }

    // 指定されたタイプからランダムな格言を取得するメソッド
    public String getRandomProverbByType(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + Proverb_TABLE_NAME +
                        " WHERE type = ? ORDER BY RANDOM() LIMIT 1",
                new String[]{type});

        if (cursor.moveToFirst()) {
            @SuppressLint("Range") String proverb = decrypt(cursor.getString(cursor.getColumnIndex(COLUMN_PROVERB)));
            @SuppressLint("Range") String speaker = decrypt(cursor.getString(cursor.getColumnIndex(COLUMN_SPEAKER)));
            cursor.close();
            return proverb + " - " + speaker;
        }

        cursor.close();
        return null;
    }

    // 取得した格言のパスを取得するメソッド
    public Integer getDrawablePathBySpeaker(String speaker) {
        SQLiteDatabase db = this.getReadableDatabase();
        speaker = encrypt(speaker);
        Cursor cursor = db.rawQuery("SELECT * FROM " + Proverb_TABLE_NAME +
                        " WHERE speaker = ?",
                new String[]{speaker});
        Integer num = cursor.getCount();


        if (cursor.moveToFirst()) {
            @SuppressLint("Range") Integer drawable_path = cursor.getInt(cursor.getColumnIndex(COLUMN_DRAWABLE_PATH));
            cursor.close();
            return drawable_path;
        }

        cursor.close();
        return null;
    }

    // 取得したパスのboolを変更するメソッド
    public void toggleDrawableBoolByPath(int path) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        int currentBoolValue = -1; // 初期値として -1 (不正な値) を設定

        try {
            // 現在の drawable_bool の値を取得
            cursor = db.rawQuery("SELECT drawable_bool FROM " + Proverb_TABLE_NAME + " WHERE drawable_path = ?",
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

            int rowsAffected = db.update(Proverb_TABLE_NAME, values, "drawable_path = ?",
                    new String[]{String.valueOf(path)});

            Log.d("DB_UPDATE", "Rows affected: " + rowsAffected);
        } else {
            Log.d("DB_UPDATE", "No update performed. Current value: " + currentBoolValue);
        }
    }

    // idを取得するメソッド
    public Integer getIdByPath(int path) {
        SQLiteDatabase db = this.getReadableDatabase();

        try (Cursor cursor = db.rawQuery("SELECT id FROM " + Proverb_TABLE_NAME + " WHERE drawable_path = ?",
                new String[]{String.valueOf(path)})) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return null;

    }

    // drawable_boolを取得する
    public Map<String, int[]> getAllIdAndDrawablePathByDrawableBool() {
        ArrayList<Integer> idList = new ArrayList<>();
        ArrayList<Integer> drawablePathList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            // クエリ実行
            cursor = db.rawQuery("SELECT id, drawable_path FROM " + Proverb_TABLE_NAME + " WHERE drawable_bool = 1", null);

            // 結果をリストに追加
            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("id"));
                    @SuppressLint("Range") int drawablePath = cursor.getInt(cursor.getColumnIndex("drawable_path"));
                    idList.add(id);
                    drawablePathList.add(drawablePath);
                } while (cursor.moveToNext());
            }
        } finally {
            // リソース解放
            if (cursor != null) cursor.close();
        }

        // ArrayListをint配列に変換
        int[] idArray = new int[idList.size()];
        for (int i = 0; i < idList.size(); i++) {
            idArray[i] = idList.get(i);
        }

        int[] drawablePathArray = new int[drawablePathList.size()];
        for (int i = 0; i < drawablePathList.size(); i++) {
            drawablePathArray[i] = drawablePathList.get(i);
        }

        // 結果をMapに格納して返す
        Map<String, int[]> resultMap = new HashMap<>();
        resultMap.put("id", idArray);
        resultMap.put("drawable_path", drawablePathArray);

        return resultMap;
    }


    public ArrayMap<String, Object> getAllById(int id) {
        ArrayMap<String, Object> resultMap = new ArrayMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + Proverb_TABLE_NAME + " WHERE id = ?", new String[]{String.valueOf(id)});

            if (cursor.moveToFirst()) {
                @SuppressLint("Range") String proverb = decrypt(cursor.getString(cursor.getColumnIndex(COLUMN_PROVERB)));
                @SuppressLint("Range") String speaker = decrypt(cursor.getString(cursor.getColumnIndex(COLUMN_SPEAKER)));
                @SuppressLint("Range") String type = cursor.getString(cursor.getColumnIndex(COLUMN_TYPE));
                @SuppressLint("Range") int typeId = cursor.getInt(cursor.getColumnIndex(COLUMN_TYPE_ID));
                @SuppressLint("Range") int count = cursor.getInt(cursor.getColumnIndex(COLUMN_COUNT));
                @SuppressLint("Range") int drawablePath = cursor.getInt(cursor.getColumnIndex(COLUMN_DRAWABLE_PATH));
                @SuppressLint("Range") int drawableBool = cursor.getInt(cursor.getColumnIndex(COLUMN_DRAWABLE_BOOl));
                @SuppressLint("Range") String firstGetTime = decrypt(cursor.getString(cursor.getColumnIndex(COLUMN_FIRST_GET_TIME)));
                @SuppressLint("Range") String createdAt = cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_AT));
                @SuppressLint("Range") String updatedAt = cursor.getString(cursor.getColumnIndex(COLUMN_UPDATED_AT));

                resultMap.put(COLUMN_ID, id);
                resultMap.put(COLUMN_PROVERB, proverb);
                resultMap.put(COLUMN_SPEAKER, speaker);
                resultMap.put(COLUMN_TYPE, type);
                resultMap.put(COLUMN_TYPE_ID, typeId);
                resultMap.put(COLUMN_COUNT, count);
                resultMap.put(COLUMN_DRAWABLE_PATH, drawablePath);
                resultMap.put(COLUMN_DRAWABLE_BOOl, drawableBool);
                resultMap.put(COLUMN_FIRST_GET_TIME, firstGetTime);
                resultMap.put(COLUMN_CREATED_AT, createdAt);
                resultMap.put(COLUMN_UPDATED_AT, updatedAt);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return resultMap;
    }

    // proverbsテーブルのidとdrawable_boolを全て取得するメソッド
    public ArrayMap<Integer, Object> getAllIdAndBool() {
        ArrayMap<Integer, Object> resultMap = new ArrayMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            // クエリ実行：idとdrawable_bool列を取得
            cursor = db.rawQuery("SELECT id, " + COLUMN_DRAWABLE_BOOl + " FROM " + Proverb_TABLE_NAME, null);

            // 結果をArrayMapに追加
            if (cursor.moveToFirst()) {
                do {
                    // 各行のデータを取得
                    @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("id")); // id列の値を取得
                    @SuppressLint("Range") int drawableBool = cursor.getInt(cursor.getColumnIndex(COLUMN_DRAWABLE_BOOl)); // drawable_bool列の値を取得

                    // drawable_boolが1ならtrue、0ならfalseに変換
                    boolean boolValue = drawableBool == 1;

                    // idをキー、boolValueを値として追加
                    resultMap.put(id, boolValue);
                } while (cursor.moveToNext());
            }
        } finally {
            // リソース解放
            if (cursor != null) cursor.close();
        }

        return resultMap; // 結果を返す
    }

    // 初回取得日を取得
    public String getFirstTime(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String firstGetTime = null;

        // SQLクエリを実行して COLUMN_FIRST_GET_TIME を取得
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_FIRST_GET_TIME +
                        " FROM " + Proverb_TABLE_NAME +
                        " WHERE id = ?",
                new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            // COLUMN_FIRST_GET_TIME の値を取得
            firstGetTime = cursor.getString(0);
            firstGetTime = decrypt(firstGetTime);
        }
        cursor.close(); // カーソルを閉じる

        // 日付を整形
        if (firstGetTime != null) {
            try {
                // yyyy-MM-dd HH:mm:ss形式のSimpleDateFormat（入力フォーマット）
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                // yyyy年MM月dd日形式のSimpleDateFormat（出力フォーマット）
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());

                // 日付文字列をDate型に変換し、再フォーマット
                firstGetTime = outputFormat.format(inputFormat.parse(firstGetTime));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return firstGetTime; // 値を返す（nullの場合もあり得る）
    }

    // 初回取得日を挿入
    @SuppressLint("Range")
    public void InsertFirstGetTime(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String currentTime = null;

        try {
            // SQLiteで現在時刻を取得（JST）
            Cursor cursor = db.rawQuery("SELECT DATETIME('now', '+9 hours')", null);

            if (cursor.moveToFirst()) {
                // 現在時刻を文字列として取得
                currentTime = cursor.getString(0); // インデックス 0 を指定
            }
            cursor.close();

            // 暗号化処理を適用
            if (currentTime != null) {
                String encryptedTime = encrypt(currentTime);

                // SQL文で更新処理
                if (encryptedTime != null) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_FIRST_GET_TIME, encryptedTime);
                    int rowsAffected = db.update(Proverb_TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
                    Log.d("InsertFirstGetTime", "Rows affected: " + rowsAffected + " for id: " + id);
                } else {
                    Log.e("InsertFirstGetTime", "Failed to encrypt current time for id: " + id);
                }
            } else {
                Log.e("InsertFirstGetTime", "Failed to get current time for id: " + id);
            }
        } catch (Exception e) {
            Log.e("InsertFirstGetTime", "Error inserting first get time for id: " + id, e);
        }
    }

    private void insertInitialDailyProverbsData(SQLiteDatabase db) {
        // 昨日の日付を取得
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date yesterday = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String yesterdayDate = sdf.format(yesterday);

        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVERB, encrypt("初期の格言"));
        values.put(COLUMN_SPEAKER, encrypt("初期のスピーカー"));
        values.put(COLUMN_GET_TIME, yesterdayDate);

        // 例: 昨日の日付で初期データを挿入
        //insertDailyProverb("初期の格言", "初期のスピーカー", yesterdayDate);
        db.insert(Daily_Proverb_Table_Name, null, values);
    }

    // 一日ごとの格言を挿入
    public void insertDailyProverb(String proverb, String speaker, String day) throws ParseException { // dayはyyyy-MM-ddの形式
        // SQLiteDatabaseインスタンスを取得
        SQLiteDatabase db = getWritableDatabase();

        // ContentValuesを使用してデータを挿入
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVERB, encrypt(proverb)); // COLUMN_PROVERB に格言を設定
        values.put(COLUMN_SPEAKER, encrypt(speaker)); // COLUMN_SPEAKER に発言者を設定
        values.put(COLUMN_GET_TIME, encrypt(day));   // COLUMN_GET_TIME に日付を設定

        // データベースに挿入
        long result = db.insert(Daily_Proverb_Table_Name, null, values);

        if (result == -1) {
            // 挿入失敗時の処理
            Log.e("DatabaseError", "Failed to insert data into " + Daily_Proverb_Table_Name);
        } else {
            // 挿入成功時の処理
            Log.d("DatabaseSuccess", "Data inserted successfully into " + Daily_Proverb_Table_Name);
        }
    }

    // 対応する日付の格言を取得
    public ArrayMap<String, String> getProverbAndSpeakerByDate(String date) {
        date = encrypt(date);
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayMap<String, String> proverb = new ArrayMap<>();
        Cursor cursor = null;

        try {
            if (date != null) {
                String query = "SELECT " + COLUMN_PROVERB + ", " + COLUMN_SPEAKER +
                        " FROM " + Daily_Proverb_Table_Name +
                        " WHERE " + COLUMN_GET_TIME + " = ?";

                cursor = db.rawQuery(query, new String[]{date});
                Integer num = cursor.getCount();

                if (/*cursor != null &&*/ cursor.moveToFirst()) {
                    @SuppressLint("Range") String mProverb = decrypt(cursor.getString(cursor.getColumnIndex(COLUMN_PROVERB)));
                    @SuppressLint("Range") String mSpeaker = decrypt(cursor.getString(cursor.getColumnIndex(COLUMN_SPEAKER)));
                    proverb.put("proverb", mProverb);
                    proverb.put("speaker", mSpeaker);
                }
            } else {
                Log.e("getProverbAndSpeakerByDate", "Date is null");
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting proverb by date", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return proverb;
    }
}
