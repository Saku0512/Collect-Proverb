package com.example.collectproverb;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.app.Dialog;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String PREF_NAME = "ProverbAppPreferences";
    private static final String PREF_LAST_CLICK_DATE = ""; // "yyyy-MM-dd"
    private int questionIndex = 0; // 現在の質問のインデックス
    private int score = 0; // Yesの回数をカウント
    private Random random;
    private DatabaseHelper databaseHelper; // SQLiteデータベースのヘルパークラス

    private final String[] questions = {
            "最近疲れを感じていますか？",
            "やる気が出ないことが多いですか？",
            "リラックスする時間はとれていますか？"
    };

    private ArrayList<String> positiveQuotes;
    private ArrayList<String> encouragementQuotes;
    private ArrayList<String> restQuotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // DatabaseHelperのインスタンスを作成
        databaseHelper = new DatabaseHelper(this);

        Button getButton = findViewById(R.id.get_button);
        TextView cloudText = findViewById(R.id.cloudText);
        TextView today_proverb = findViewById(R.id.today_proverb);
        TextView today_proverb_author = findViewById(R.id.today_proverb_author);

        //checkButtonState(getButton);

        // ボタンが押されたときの処理
        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ポップアップを表示
                showPopupDialog(cloudText, today_proverb, today_proverb_author, getButton);

                // ボタンが押された時点で現在の日付を保存
                saveLastClickDate();
            }
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 格言リストの初期化
        initializeQuotes();
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
        // ボタンが押された日と現在の日付を比較
        String savedDay = getSavedClickDate(); // 保存された日
        String currentDay = new SimpleDateFormat("dd", Locale.getDefault()).format(new Date()); // 今日の日

        if (savedDay.equals(currentDay)) {
            // 今日押された場合
            getButton.setEnabled(false); // ボタン無効化
            getButton.setText("今日はもう押せません");
        } else {
            // 違う日付の場合
            getButton.setEnabled(true); // ボタン有効化
            getButton.setText("今日の格言を表示");
        }
    }

    // 保存された日を取得する
    private String getSavedClickDate() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(PREF_LAST_CLICK_DATE, "");
    }


    // 格言リストを初期化する関数
    private void initializeQuotes() {
        random = new Random();

        positiveQuotes = new ArrayList<>();
        positiveQuotes.add("成功する秘訣は、成功するまでやり続けることである。 - トーマス・エジソン");
        positiveQuotes.add("行動しなければ何も変わらない。 - ベンジャミン・フランクリン");
        positiveQuotes.add("追い続ける勇気があるのなら、全ての夢は必ず実現する。 - ウォルト・ディズニー");
        positiveQuotes.add("一番大事なことは、自分の心と直感に従う勇気を持つことだ。 - ウォルト・ディズニー");

        encouragementQuotes = new ArrayList<>();
        encouragementQuotes.add("どんなに暗い夜でも、朝は必ずやってくる。 - ハリエット・ビーチャー・ストウ");
        encouragementQuotes.add("上を向いている限り、絶対にいいことがある。 - 三浦知良");
        encouragementQuotes.add("いつかこの日さえも、楽しく思い出すことがあるだろう。 - ウェルギリウス");
        encouragementQuotes.add("笑われて、笑われて、つよくなる。 - 太宰治");

        restQuotes = new ArrayList<>();
        restQuotes.add("休むことも大切だ。焦らなくていい。 - 老子");
        restQuotes.add("休息なしに成長なし。 - レオナルド・ダ・ヴィンチ");
        restQuotes.add("明日が素晴らしい日だといけないから、うんと休息するのさ。 - スヌーピー");
        restQuotes.add("疑う余地のない純粋の歓びの一つは、勤勉の後の休息である。 - イマヌエル・カント");
    }

    // ポップアップを表示する関数
    private void showPopupDialog(TextView cloudText, TextView today_proverb, TextView today_proverb_author, Button getButton) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_layout, null);
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(popupView);

        // ダイアログが外をタッチしても閉じないようにする
        dialog.setCancelable(false);

        // UI要素
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
            int width = (int) (displayMetrics.widthPixels * 0.9); // 画面幅の80%
            layoutParams.width = width;

            // "Yes"ボタンの左マージンを設定
            LinearLayout.LayoutParams ButtonYesParams = (LinearLayout.LayoutParams) buttonYes.getLayoutParams();
            ButtonYesParams.setMargins((int) (width * 0.10), 0, 0, 0); // 左マージンを8%に設定
            buttonYes.setLayoutParams(ButtonYesParams);

            LinearLayout.LayoutParams ButtonNoParams = (LinearLayout.LayoutParams) buttonNo.getLayoutParams();
            ButtonNoParams.setMargins((int) (width * 0.03), 0, 0, 0); // 左マージンを5%に設定
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
            nextQuestion(questionTextView, quoteTextView, quoteTextName, buttonYes, buttonNo, closeButton, closeButton_final, cloudText, dialog, today_proverb, today_proverb_author, getButton);
        });

        // Noボタンの処理
        buttonNo.setOnClickListener(view -> {
            nextQuestion(questionTextView, quoteTextView, quoteTextName, buttonYes, buttonNo, closeButton, closeButton_final, cloudText, dialog, today_proverb, today_proverb_author, getButton);
        });

        // Closeボタンの処理
        closeButton.setOnClickListener(view -> dialog.dismiss());

        // ポップアップを表示
        dialog.show();
    }

    // 次の質問へ進む処理
    private void nextQuestion(TextView questionTextView, TextView quoteTextView, TextView quoteTextName, Button buttonYes, Button buttonNo, TextView closeButton, Button closeButton_final, TextView cloudText, Dialog dialog, TextView today_proverb, TextView today_proverb_author, Button getButton) {
        questionIndex++;
        if (questionIndex < questions.length) {
            questionTextView.setText(questions[questionIndex]);
        } else {
            displayResult(questionTextView, quoteTextView, quoteTextName, buttonYes, buttonNo, closeButton, closeButton_final, cloudText, dialog, today_proverb, today_proverb_author, getButton);
        }
    }

    // 結果を表示する処理
    private void displayResult(TextView questionTextView, TextView quoteTextView, TextView quoteTextName, Button buttonYes, Button buttonNo, TextView closeButton, Button closeButton_final, TextView cloudText, Dialog dialog, TextView today_proverb, TextView today_proverb_author, Button getButton) {
        String selectedQuote;

        if (score == 0) {
            //selectedQuote = positiveQuotes.get(random.nextInt(positiveQuotes.size()));
            selectedQuote = databaseHelper.getRandomProverbByType("positive");
        } else if (score == 1 || score == 2) {
            //selectedQuote = encouragementQuotes.get(random.nextInt(encouragementQuotes.size()));
            selectedQuote = databaseHelper.getRandomProverbByType("encouragement");
        } else {
            //selectedQuote = restQuotes.get(random.nextInt(restQuotes.size()));
            selectedQuote = databaseHelper.getRandomProverbByType("rest");
        }

        // 格言と偉人名を分割
        String[] parts = selectedQuote.split(" - ", 2);
        final String quoteOriginal = parts[0];
        String author = (parts.length > 1) ? parts[1] : "不明";

        // 自動的に改行を入れる
        String quoteFormatted = quoteOriginal.replaceAll("([。！？])", "$1\n");
        quoteFormatted = quoteFormatted.replaceAll("(、)", "$1\n");

        // UIを更新
        questionTextView.setVisibility(View.GONE);
        buttonYes.setVisibility(View.GONE);
        buttonNo.setVisibility(View.GONE);
        closeButton.setVisibility(View.GONE);
        closeButton_final.setVisibility(View.VISIBLE);
        quoteTextView.setText(quoteFormatted);
        quoteTextView.setVisibility(View.VISIBLE);
        quoteTextName.setText("- " + author);
        quoteTextName.setVisibility(View.VISIBLE);

        // 変数をfinalにする
        final String quoteFinal = quoteFormatted;
        final String authorFinal = author;


        // Closeボタンの処理
        closeButton_final.setOnClickListener(view -> {
            //cloudText.setText(quote);  cloudText に格言をセット
            cloudText.setVisibility(View.GONE);
            today_proverb.setText(quoteFinal);
            today_proverb_author.setText("- " + authorFinal);
            // ボタンの状態を更新
            checkButtonState(getButton);
            dialog.dismiss(); // ポップアップを閉じる
        });
    }
}