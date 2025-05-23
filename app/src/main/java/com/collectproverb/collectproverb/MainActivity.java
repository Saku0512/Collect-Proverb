package com.collectproverb.collectproverb;

import static com.collectproverb.collectproverb.AlarmHelper.setAlarm;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.app.Dialog;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.TimeZone;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class MainActivity extends AppCompatActivity {
    private static final String PREF_NAME = "ProverbAppPreferences"; // SharedPreferencesにデータを保存するキー
    private static final String PREF_LAST_CLICK_DATE = ""; // "yyyy-MM-dd"が格納される
    private int questionIndex = 0; // 現在の質問のインデックス
    private int score = 0; // Yesの回数をカウント
    private DatabaseHelper databaseHelper; // SQLiteデータベースのヘルパークラス
    private SQLiteDatabase db; // データベースオブジェクトをMainActivityのメンバ変数として保持

    // 質問リスト
    private final String[] questions = {
            "最近疲れを感じていますか？",
            "やる気が出ないことが多いですか？",
            "リラックスする時間はとれていますか？"
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // 画面に端から端までコンテンツを表示するための設定
        setContentView(R.layout.activity_main); // activity_main.xmlを画面に設定

        setAlarm(this);
        // 初回起動判定
        if (PreferencesUtil.isFirstLaunch(this)) {
            showNotificationPermissionDialog();
            PreferencesUtil.setFirstLaunch(this, false); // 初回起動フラグを更新
        }

        // DatabaseHelperのインスタンスを作成(これでデータベースにアクセスする)
        databaseHelper = new DatabaseHelper(this);
        db = databaseHelper.getWritableDatabase(); // MainActivityでデータベースを開く

        Button getButton = findViewById(R.id.get_button);
        TextView homeText = findViewById(R.id.homeText);
        TextView today_proverb = findViewById(R.id.today_proverb);
        TextView today_proverb_author = findViewById(R.id.today_proverb_author);

        Map<String, int[]> resultMap = databaseHelper.getAllIdAndDrawablePathByDrawableBool();

        int[] ids = resultMap.get("id");
        int[] drawable_paths = resultMap.get("drawable_path");

// idsとdrawable_pathsの対応する画像を切り替える処理
        for (int i = 0; i < Objects.requireNonNull(ids).length; i++) {
            int id = ids[i]; // 現在のid
            assert drawable_paths != null;
            int drawablePath = drawable_paths[i]; // 対応するdrawable_path

            // ImageViewを取得
            ImageView badge = findViewById(getResources().getIdentifier(
                    "unopened_badge_" + id, // 例: "unopened_badge_5"
                    "id",
                    getPackageName()
            ));

            if (badge != null) { // ImageViewが存在する場合のみ処理を行う
                badge.setImageResource(drawablePath); // drawable_pathに対応する画像に切り替え
            } else {
                Log.e("ImageViewError", "ImageView with id unopened_badge_" + id + " not found.");
            }
        }

        // 画面に出力する格言の処理　既に格言を取得している場合に発火
        String nowDate = getNowDate();
        Map<String, String> result = databaseHelper.getProverbAndSpeakerByDate(nowDate);
        if (result != null) {
            String proverb = result.get("proverb");
            String speaker = result.get("speaker");

            if (proverb != null && !proverb.isEmpty() && speaker != null && !speaker.isEmpty()) {
                homeText.setVisibility(View.GONE);
                today_proverb.setText(proverb);
                today_proverb_author.setText("- " + speaker);
            }
        }

        // 取得ボタンの状態確認
        checkButtonState(getButton);

        // 取得ボタンが押されたときの処理
        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ポップアップを表示
                showPopupDialog(homeText, today_proverb, today_proverb_author, getButton);

                // ボタンが押された時点で現在の日付を保存
                saveLastClickDate();
            }
        });

        // 画像が押された時の処理
        setupBadgeClickListeners();


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private final Map<Integer, Boolean> badgeStates = new HashMap<>(); // バッジの状態を管理

    private void setupBadgeClickListeners() {
        // バッジの状態を初期化
        for (int i = 1; i <= 12; i++) {
            badgeStates.put(i, false); // 初期状態はすべて未開放
        }

        //DBで有効化されたバッジを格納
        ArrayMap<Integer, Object> data = databaseHelper.getAllIdAndBool();
        // データベースの値で上書き
        for (Map.Entry<Integer, Object> entry : data.entrySet()) {
            int id = entry.getKey();
            boolean state = (Boolean) entry.getValue();

            // idが1〜12の範囲かチェック
            if (id >= 1 && id <= 12) {
                badgeStates.put(id, state);
            }
        }


        for (int i = 1; i <= 12; i++) { // バッジのIDが1～12まであると仮定
            String badgeId = "unopened_badge_" + i; // ID文字列を生成
            int resId = getResources().getIdentifier(badgeId, "id", getPackageName()); // IDリソースを取得

            ImageView badge = findViewById(resId); // ImageViewを取得
            if (badge != null) { // バッジが存在する場合のみ処理
                int badgeNumber = i; // 現在のバッジ番号を保持
                badge.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // バッジが未開放の場合は処理を中断
                        if (!badgeStates.get(badgeNumber)) { // 状態がfalseなら未開放
                            Log.d("BadgeClick", "Badge " + badgeNumber + " is unopened. No action taken.");
                            return; // 処理を中断
                        }

                        Log.d("BadgeClick", "Clicked Badge ID: " + badgeNumber); // デバッグログに出力

                        try {
                            handleBadgeClick(badgeNumber); // クリックされたバッジ番号で処理を実行
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        }
    }

    private void handleBadgeClick(int badgeNumber) throws ParseException {
        // badgeNumberでDB検索してデータを取得
        ArrayMap<String, Object> data = databaseHelper.getAllById(badgeNumber);

        // ポップアップ準備
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.badge_popup, null);
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(popupView);

        // ダイアログが外をタッチしても閉じないようにする
        dialog.setCancelable(false);

        // UI要素
        ImageView imageView = popupView.findViewById(R.id.popup_image);
        TextView popup_author = popupView.findViewById(R.id.popup_proverb_author);
        TextView popup_proverb_content = popupView.findViewById(R.id.popup_proverb_content);
        TextView popup_get_day = popupView.findViewById(R.id.popup_get_day);
        TextView closeButton = popupView.findViewById(R.id.close_button);
        TextView count = popupView.findViewById(R.id.popup_count);
        String maybeYet = (String) data.get("first_get_time");
        if (Objects.equals(maybeYet, "yet")) {
            Integer id = databaseHelper.getIdByPath((Integer) data.get("drawable_path"));
            databaseHelper.InsertFirstGetTime(id);
            maybeYet = databaseHelper.getFirstTime(id);
        } else {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
            maybeYet = outputFormat.format(Objects.requireNonNull(inputFormat.parse(maybeYet)));
        }

        imageView.setImageResource((Integer) data.get("drawable_path"));
        popup_author.setText((String) data.get("speaker"));
        popup_proverb_content.setText((String) data.get("proverb"));
        popup_get_day.setText(maybeYet);
        Integer IntCount = (Integer) data.get("count");
        String StringCount = String.valueOf(IntCount);
        count.setText(StringCount);

        // ウィンドウのレイアウトパラメータを取得
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());

            // 画面の幅に対する割合でサイズを設定する
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            // 画面幅の90%
            layoutParams.width = (int) (displayMetrics.widthPixels * 0.9);
            // 角の丸さを設定
            float cornerRadius = getResources().getDimension(R.dimen.dialog_corner_radius);
            window.setBackgroundDrawable(new GradientDrawable() {{
                setShape(GradientDrawable.RECTANGLE);
                setCornerRadius(cornerRadius);
            }});

            // 変更を適用
            window.setAttributes(layoutParams);
        }

        // Closeボタンの処理
        closeButton.setOnClickListener(view -> dialog.dismiss());

        // ポップアップを表示
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null && db.isOpen()) {
            db.close(); // MainActivityが破棄されるときにデータベースを閉じる
        }
    }

    // ボタンが押された日付を保存する
    private void saveLastClickDate() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // 現在の日付の「日」部分のみを保存
        String currentDay = new SimpleDateFormat("dd", Locale.getDefault()).format(new Date());
        editor.putString(PREF_LAST_CLICK_DATE, currentDay);
        editor.apply();
    }

    // ボタンの有効化/無効化を設定
    private void checkButtonState(Button getButton) {
        // 書き込み用のデータベースを取得
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        // ボタンが押された日と現在の日付を比較  yyyy-MM-dd hh:mm:ss
        String savedDateTime = databaseHelper.getBoolUpdatedAt();
        int ButtonBool = databaseHelper.getButtonBool();
        // 今日の日付を取得
        String currentDay = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss",
                Locale.getDefault()).format(new Date());

        // savedDateTimeからddを抽出
        String savedDay = extractDayFromDate(savedDateTime);
        // currentDayからddを抽出
        String currentDayOnly = extractDayFromDate(currentDay);

        if (savedDay.equals(currentDayOnly) && (ButtonBool == 0)) {
            // 今日押された場合
            getButton.setEnabled(false); // ボタン無効化
            getButton.setText("今日はもう押せません");
            databaseHelper.FalseButtonBool(db);
        } else {
            // 違う日付の場合
            getButton.setEnabled(true); // ボタン有効化
            getButton.setText("今日の格言を表示");
            databaseHelper.EnableButtonBool(db);
        }
    }

    private String extractDayFromDate(String dateTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateTime);

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd", Locale.getDefault());
            return outputFormat.format(date); // `dd` のみを返す
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // 変換失敗時は null
        }
    }

    // ポップアップを表示する関数
    private void showPopupDialog(TextView homeText, TextView today_proverb, TextView today_proverb_author, Button getButton) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_layout, null); // popup_layout.xmlの要素をセット
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(popupView);

        // ダイアログが外をタッチしても閉じないようにする
        dialog.setCancelable(false);

        // UI要素を取得
        TextView questionTextView = popupView.findViewById(R.id.questionTextView);
        TextView quoteTextView = popupView.findViewById(R.id.quoteTextView);
        TextView quoteTextName = popupView.findViewById(R.id.quoteTextName);
        Button buttonYes = popupView.findViewById(R.id.buttonYes);
        Button buttonNo = popupView.findViewById(R.id.buttonNo);
        Button closeButton_final = popupView.findViewById(R.id.close_button_final);
        TextView closeButton = popupView.findViewById(R.id.close_button);

        // ウィンドウのレイアウトパラメータを取得
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());

            // 画面の幅に対する割合でサイズを設定する
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = (int) (displayMetrics.widthPixels * 0.9); // 画面幅の90%
            layoutParams.width = width;

            // "Yes"ボタンの左マージンを設定
            LinearLayout.LayoutParams ButtonYesParams = (LinearLayout.LayoutParams) buttonYes.getLayoutParams();
            ButtonYesParams.setMargins((int) (width * 0.10), 0, 0, 0); // 左マージンを10%に設定
            buttonYes.setLayoutParams(ButtonYesParams);

            // "No"ボタンの左マージンを設定
            LinearLayout.LayoutParams ButtonNoParams = (LinearLayout.LayoutParams) buttonNo.getLayoutParams();
            ButtonNoParams.setMargins((int) (width * 0.03), 0, 0, 0); // 左マージンを3%に設定
            buttonNo.setLayoutParams(ButtonNoParams);

            // 角の丸さを設定
            float cornerRadius = getResources().getDimension(R.dimen.dialog_corner_radius);
            window.setBackgroundDrawable(new GradientDrawable() {{
                setShape(GradientDrawable.RECTANGLE);
                setCornerRadius(cornerRadius);
            }});

            // 変更を適用
            window.setAttributes(layoutParams);
        }

        // 初期設定
        questionIndex = 0;
        score = 0;
        questionTextView.setText(questions[questionIndex]);

        // Yesボタンの処理
        buttonYes.setOnClickListener(view -> {
            score++;
            nextQuestion(questionTextView, quoteTextView, quoteTextName, buttonYes, buttonNo, closeButton, closeButton_final, homeText,
                    dialog, today_proverb, today_proverb_author, getButton);
        });

        // Noボタンの処理
        buttonNo.setOnClickListener(view -> {
            nextQuestion(questionTextView, quoteTextView, quoteTextName, buttonYes, buttonNo, closeButton, closeButton_final, homeText,
                    dialog, today_proverb, today_proverb_author, getButton);
        });

        // Closeボタンの処理
        closeButton.setOnClickListener(view -> dialog.dismiss());

        // ポップアップを表示
        dialog.show();
    }

    // 次の質問へ進む処理
    private void nextQuestion(TextView questionTextView, TextView quoteTextView, TextView quoteTextName, Button buttonYes, Button buttonNo,
                              TextView closeButton, Button closeButton_final, TextView homeText, Dialog dialog, TextView today_proverb,
                              TextView today_proverb_author, Button getButton) {
        questionIndex++;
        if (questionIndex < questions.length) {
            // 質問を切り替える
            questionTextView.setText(questions[questionIndex]);
        } else {
            // 結果を表示
            displayResult(questionTextView, quoteTextView, quoteTextName, buttonYes, buttonNo, closeButton, closeButton_final,
                    homeText, dialog, today_proverb, today_proverb_author, getButton);
        }
    }

    // 結果を表示する処理
    @SuppressLint("SetTextI18n")
    private void displayResult(TextView questionTextView, TextView quoteTextView, TextView quoteTextName, Button buttonYes,
                               Button buttonNo, TextView closeButton, Button closeButton_final, TextView homeText,
                               Dialog dialog, TextView today_proverb, TextView today_proverb_author, Button getButton) {
        String selectedQuote;

        if (score == 0) {
            selectedQuote = databaseHelper.getRandomProverbByType("positive");
        } else if (score == 1 || score == 2) {
            selectedQuote = databaseHelper.getRandomProverbByType("encouragement");
        } else {
            selectedQuote = databaseHelper.getRandomProverbByType("rest");
        }

        // 格言と偉人名を分割
        String[] parts = selectedQuote.split(" - ", 2);
        final String quoteOriginal = parts[0];
        String author = (parts.length > 1) ? parts[1] : "不明";

        // UIを更新
        questionTextView.setVisibility(View.GONE);
        buttonYes.setVisibility(View.GONE);
        buttonNo.setVisibility(View.GONE);
        closeButton.setVisibility(View.GONE);
        closeButton_final.setVisibility(View.VISIBLE);
        quoteTextView.setText(quoteOriginal);
        quoteTextView.setVisibility(View.VISIBLE);
        quoteTextName.setText("- " + author);
        quoteTextName.setVisibility(View.VISIBLE);

        // 変数をfinalにする
        final String quoteFinal = quoteOriginal;
        final String authorFinal = author;


        // Closeボタンの処理
        closeButton_final.setOnClickListener(view -> {
            //homeText.setText(quote);  homeText に格言をセット
            homeText.setVisibility(View.GONE);
            today_proverb.setText(quoteFinal);
            today_proverb_author.setText("- " + authorFinal);

            // 格言のパスを取得
            Integer drawable_path = databaseHelper.getDrawablePathBySpeaker(author);
            // pathのbool値を変更
            databaseHelper.toggleDrawableBoolByPath(drawable_path);
            // idを取得
            Integer id = databaseHelper.getIdByPath(drawable_path);
            ImageView badge = findViewById(getResources().getIdentifier(
                    "unopened_badge_" + id, // 例: "unopened_badge_5"
                    "id",
                    getPackageName()
            ));
            // 現在の日付を取得
            String nowDate = getNowDate();
            //DB更新
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            databaseHelper.CountUp(id); // 格言の取得回数を更新
            try {
                databaseHelper.insertDailyProverb(quoteFinal, authorFinal, nowDate);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            db.beginTransaction();
            try {
                databaseHelper.FalseButtonBool(db);
                db.setTransactionSuccessful(); // 成功した場合のみコミット
            } catch (Exception e) {
                e.printStackTrace(); // エラー内容をログに出力
            } finally {
                db.endTransaction(); // トランザクション終了
            }
            setupBadgeClickListeners();
            // ボタンの状態を更新
            checkButtonState(getButton);
            dialog.dismiss(); // ポップアップを閉じる

            // アニメーションをロード
            Animation glowAnimation = AnimationUtils.loadAnimation(this, R.anim.badge_glow);

            // アニメーションを開始
            if (badge != null) {
                badge.startAnimation(glowAnimation);
            }
            // badgeの画像を切り替え
            badge.setImageResource(drawable_path);
        });
    }

    private String getNowDate() {
        Date now = new Date();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        return dateFormat.format(now);
    }

    private void showNotificationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("通知の許可")
                .setMessage("通知を有効化しますか？")
                .setPositiveButton("許可する", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 通知の設定
                        setAlarm(MainActivity.this);
                    }
                })
                .setNegativeButton("許可しない", null)
                .show();
    }
}