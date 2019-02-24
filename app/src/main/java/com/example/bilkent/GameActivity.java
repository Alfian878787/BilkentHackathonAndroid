package com.example.bilkent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class GameActivity extends AppCompatActivity {

    Socket mSocket;
    String uniqueID;
    int timeLeft;
    int play, idle, ads, result;

    {
        try {
            mSocket = IO.socket("http://104.248.131.83:8080/quiz");
        } catch (URISyntaxException ignore) {
        }
    }

    ProgressBar pbWait;
    FrameLayout mainLayout;
    Button btnFirstAnswer, btnSecondAnswer, btnThirdAnswer, btnFourthAnswer;

    private void enableButton(Button btn){
        btn.setEnabled(true);
        btn.setAlpha(1);
    }

    private void disableButton(Button btn){
        btn.setEnabled(false);
        btn.setAlpha((float) 0.5);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        uniqueID = IDUtil.id(this);
        (pbWait = findViewById(R.id.pb_wait_connect)).setVisibility(View.VISIBLE);
        (mainLayout = findViewById(R.id.fl_main)).setVisibility(View.INVISIBLE);
        btnFirstAnswer = findViewById(R.id.btn_first_answer);
        btnSecondAnswer = findViewById(R.id.btn_second_answer);
        btnThirdAnswer = findViewById(R.id.btn_third_answer);
        btnFourthAnswer = findViewById(R.id.btn_fourth_answer);


        final String message = getIntent().getStringExtra("categories");

        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mSocket.emit("login",
                        String.format("{phoneId:'%s',  categories:%s}",
                                IDUtil.id(GameActivity.this), message));
            }
        });

        timeLeft = 20;

        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

            }
        });

        mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

            }
        });

        mSocket.on("general", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject jsonObject = (JSONObject) args[0];
                    jsonObject = jsonObject.getJSONObject("stateDurations");
                    int idle = jsonObject.getInt("Idle");
                    int play = jsonObject.getInt("Play");
                    int result = jsonObject.getInt("Result");
                    int ads = jsonObject.getInt("Ads");
                    GameActivity.this.idle = idle;
                    GameActivity.this.play = play;
                    GameActivity.this.result = result;
                    GameActivity.this.ads = ads;
                    ProgressBar progressBar = findViewById(R.id.progressBarToday);
                    progressBar.setMax(play);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        mSocket.on("play", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject object = (JSONObject) args[0];
                    object = object.getJSONObject("question");
                    final String text = object.getString("text");
                    JSONArray choicesJSONArr = object.getJSONArray("choices");
                    final String[] choices = new String[4];
                    for (int i = 0; i < choicesJSONArr.length(); i++) {
                        choices[i] = choicesJSONArr.getString(i);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.tv_question)).setText(text);
                            btnFirstAnswer.setText(choices[0]);
                            btnSecondAnswer.setText(choices[1]);
                            btnThirdAnswer.setText(choices[2]);
                            btnFourthAnswer.setText(choices[3]);
                            pbWait.setVisibility(View.INVISIBLE);
                            mainLayout.setVisibility(View.VISIBLE);
                            enableButton(btnFirstAnswer);
                            enableButton(btnSecondAnswer);
                            enableButton(btnThirdAnswer);
                            enableButton(btnFourthAnswer);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        mSocket.on("realtime", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject object = (JSONObject) args[0];
                    timeLeft = object.getInt("timeLeft");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ProgressBar) findViewById(R.id.progressBarToday))
                                    .setProgress(timeLeft);
                            ((TextView) findViewById(R.id.tv_remaining_time)).
                                    setText(String.valueOf(timeLeft));
                            if (timeLeft == 0) {
                                disableButton(btnFirstAnswer);
                                disableButton(btnSecondAnswer);
                                disableButton(btnThirdAnswer);
                                disableButton(btnFourthAnswer);
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });


        if (!mSocket.connected())
            mSocket.connect();
    }

}
