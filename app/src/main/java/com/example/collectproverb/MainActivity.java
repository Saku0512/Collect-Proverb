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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button getButton = findViewById(R.id.get_button);
        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupDialog();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void showPopupDialog() {
        // Layout Inflaterを取得
        LayoutInflater inflater = LayoutInflater.from(this);

        // ポップアップのレイアウトをインフレート
        View popupView = inflater.inflate(R.layout.popup_layout, null);

        // Dialogを生成
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // タイトルを非表示
        dialog.setContentView(popupView);

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

            // 角の丸さを設定
            float cornerRadius = getResources().getDimension(R.dimen.dialog_corner_radius);
            window.setBackgroundDrawable(new GradientDrawable() {{
                setShape(GradientDrawable.RECTANGLE);
                setCornerRadius(cornerRadius);
            }});

            // "次へ"ボタンの左マージンを設定
            Button nextButton = popupView.findViewById(R.id.next_button);
            LinearLayout.LayoutParams nextButtonParams = (LinearLayout.LayoutParams) nextButton.getLayoutParams();
            nextButtonParams.setMargins((int) (width * 0.4), 0, 0, 0); // 左マージンを60%に設定
            nextButton.setLayoutParams(nextButtonParams);

            // 変更を適用
            window.setAttributes(layoutParams);

        }

        // ポップアップ内の要素にアクセス
        //TextView popupTextView = popupView.findViewById(R.id.popup_text);
        // テキストを設定
        //popupTextView.setText("これはポップアップ内のテキストです。");

        // Close ボタンのOnClickListenerを設定
        Button closeButton = popupView.findViewById(R.id.close_button); // popupViewからfindViewByIdを行う
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show(); // ダイアログを表示
    }
}