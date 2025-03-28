package com.example.collectproverb;

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

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private int questionIndex = 0; // 現在の質問のインデックス
    private int score = 0; // Yesの回数をカウント
    private Random random;

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

        Button getButton = findViewById(R.id.get_button);
        TextView cloudText = findViewById(R.id.cloudText);
        TextView today_proverb = findViewById(R.id.today_proverb);
        TextView today_proverb_author = findViewById(R.id.today_proverb_author);
        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupDialog(cloudText, today_proverb, today_proverb_author); // ポップアップを表示
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

    // 格言リストを初期化する関数
    private void initializeQuotes() {
        random = new Random();

        positiveQuotes = new ArrayList<>();
        positiveQuotes.add("成功する秘訣は、成功するまでやり続けることである。 - トーマス・エジソン");
        positiveQuotes.add("行動しなければ何も変わらない。 - ベンジャミン・フランクリン");

        encouragementQuotes = new ArrayList<>();
        encouragementQuotes.add("どんなに暗い夜でも、朝は必ずやってくる。 - ハリエット・ビーチャー・ストウ");
        encouragementQuotes.add("あなたは一人じゃない。 - アノニマス");

        restQuotes = new ArrayList<>();
        restQuotes.add("休むことも大切だ。焦らなくていい。 - 老子");
        restQuotes.add("休息なしに成長なし。 - レオナルド・ダ・ヴィンチ");
    }

    // ポップアップを表示する関数
    private void showPopupDialog(TextView cloudText, TextView today_proverb, TextView today_proverb_author) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_layout, null);
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(popupView);

        // UI要素
        TextView questionTextView = popupView.findViewById(R.id.questionTextView);
        TextView quoteTextView = popupView.findViewById(R.id.quoteTextView);
        TextView quoteTextName = popupView.findViewById(R.id.quoteTextName);
        Button buttonYes = popupView.findViewById(R.id.buttonYes);
        Button buttonNo = popupView.findViewById(R.id.buttonNo);
        Button closeButton_final = popupView.findViewById(R.id.close_button_final);
        Button closeButton = popupView.findViewById(R.id.close_button);

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
            nextQuestion(questionTextView, quoteTextView, quoteTextName, buttonYes, buttonNo, closeButton, closeButton_final, cloudText, dialog, today_proverb, today_proverb_author);
        });

        // Noボタンの処理
        buttonNo.setOnClickListener(view -> {
            nextQuestion(questionTextView, quoteTextView, quoteTextName, buttonYes, buttonNo, closeButton, closeButton_final, cloudText, dialog, today_proverb, today_proverb_author);
        });

        // Closeボタンの処理
        closeButton.setOnClickListener(view -> dialog.dismiss());

        // ポップアップを表示
        dialog.show();
    }

    // 次の質問へ進む処理
    private void nextQuestion(TextView questionTextView, TextView quoteTextView, TextView quoteTextName, Button buttonYes, Button buttonNo, Button closeButton, Button closeButton_final, TextView cloudText, Dialog dialog, TextView today_proverb, TextView today_proverb_author) {
        questionIndex++;
        if (questionIndex < questions.length) {
            questionTextView.setText(questions[questionIndex]);
        } else {
            displayResult(questionTextView, quoteTextView, quoteTextName, buttonYes, buttonNo, closeButton, closeButton_final, cloudText, dialog, today_proverb, today_proverb_author);
        }
    }

    // 結果を表示する処理
    private void displayResult(TextView questionTextView, TextView quoteTextView, TextView quoteTextName, Button buttonYes, Button buttonNo, Button closeButton, Button closeButton_final, TextView cloudText, Dialog dialog, TextView today_proverb, TextView today_proverb_author) {
        String selectedQuote;

        if (score == 0) {
            selectedQuote = positiveQuotes.get(random.nextInt(positiveQuotes.size()));
        } else if (score == 1 || score == 2) {
            selectedQuote = encouragementQuotes.get(random.nextInt(encouragementQuotes.size()));
        } else {
            selectedQuote = restQuotes.get(random.nextInt(restQuotes.size()));
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
            dialog.dismiss(); // ポップアップを閉じる
        });
    }
}