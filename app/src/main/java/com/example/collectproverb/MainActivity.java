package com.example.collectproverb;

import android.os.Bundle;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

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

        // ポップアップ内の要素にアクセス
        TextView popupTextView = popupView.findViewById(R.id.popup_text);
        // テキストを設定
        popupTextView.setText("これはポップアップ内のテキストです。");

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