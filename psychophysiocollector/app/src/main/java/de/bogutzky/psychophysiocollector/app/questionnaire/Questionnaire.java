package de.bogutzky.psychophysiocollector.app.questionnaire;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

import de.bogutzky.psychophysiocollector.app.R;

public class Questionnaire {

    private static final String TAG = "Questionnaire";
    private Activity activity;
    private ArrayList<String> scaleTypes;
    private ArrayList<Integer> scaleViewIds;
    private String questionnaireFileName;
    private Dialog questionnaireDialog;
    private JSONObject questionnaire;
    private int questionsCount = 0;
    private int questionsAmount = 0;
    private Button saveButton;
    private long showTimestamp;
    private long startTimestamp;

    public Questionnaire(Activity activity, String questionnaireFileName) {
        this.activity = activity;
        this.scaleTypes = new ArrayList<>();
        this.scaleViewIds = new ArrayList<>();
        this.questionsCount = 0;
        this.questionsAmount = 0;
        this.questionnaireFileName = questionnaireFileName;
        this.questionnaire = readQuestionnaireFromJSON();
        this.questionnaireDialog = new Dialog(activity);
        String title = activity.getString(R.string.questionnaire);
        String description = "";
        try {
            title = questionnaire.getJSONObject("questionnaire").getString("title");
            description = questionnaire.getJSONObject("questionnaire").getString("description");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.questionnaireDialog.setTitle(title);
        this.questionnaireDialog.setCancelable(false);

        this.showTimestamp = System.currentTimeMillis();
        questionnaireDialog.setContentView(R.layout.questionnaire);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(questionnaireDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        questionnaireDialog.getWindow().setAttributes(lp);
        Button startQuestionnaireButton = (Button) questionnaireDialog.findViewById(R.id.startQuestionnaireButton);
        startQuestionnaireButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQuestionnaire();
            }
        });
        questionnaireDialog.show();

        this.saveButton = new Button(activity);
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Dialog getQuestionnaireDialog() {
        return questionnaireDialog;
    }

    private void startQuestionnaire() {
        this.startTimestamp = System.currentTimeMillis();
        ScrollView scrollView = new ScrollView(activity);
        final RelativeLayout relativeLayout = new RelativeLayout(activity);
        Button nextButton = new Button(activity);
        RelativeLayout.LayoutParams slp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        RelativeLayout.LayoutParams saveParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams nextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        Random rnd = new Random();

        int hidden = 0;
        try {
            JSONArray questions = questionnaire.getJSONObject("questionnaire").getJSONArray("questions");

            // fragen zufällig sortieren
            //questions = Utils.shuffleJsonArray(questions);

            questionsAmount = questions.length();
            int tmpid;
            int tmpid2;
            int tmpid3;
            int oldtmp = 0;
            for (int i = 0; i < questions.length(); i++) {
                RelativeLayout wrapperLayout = new RelativeLayout(activity);
                JSONObject q = questions.getJSONObject(i);
                if (q.getString("type").equals("rating")) {
                    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    RelativeLayout.LayoutParams params4 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    tmpid = rnd.nextInt(Integer.MAX_VALUE);
                    tmpid2 = rnd.nextInt(Integer.MAX_VALUE);
                    tmpid3 = rnd.nextInt(Integer.MAX_VALUE);
                    TextView textView = new TextView(activity);
                    textView.setId(tmpid);
                    textView.setPadding(4, 0, 0, 0);
                    textView.setTextColor(Color.WHITE);
                    textView.setTextSize(16);
                    textView.setMinLines(3);
                    params1.setMargins(0, 10, 0, 0);
                    textView.setText(q.getString("question"));
                    if(i > 0) {
                        params1.addRule(RelativeLayout.BELOW, oldtmp);
                    }
                    textView.setLayoutParams(params1);

                    TextView textView1 = new TextView(activity);
                    textView1.setText(q.getJSONArray("ratings").getString(0));
                    params2.setMargins(0, 8, 0, 0);
                    params2.addRule(RelativeLayout.BELOW, tmpid);
                    textView1.setLayoutParams(params2);

                    TextView textView2 = new TextView(activity);
                    textView2.setId(tmpid2);
                    textView2.setText(q.getJSONArray("ratings").getString(1));
                    params3.setMargins(0, 8, 0, 0);
                    params3.addRule(RelativeLayout.BELOW, tmpid);
                    params3.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    textView2.setLayoutParams(params3);

                    RatingBar ratingBar = new RatingBar(new ContextThemeWrapper(activity, R.style.RatingBar), null, 0);
                    ratingBar.setNumStars(q.getInt("stars"));
                    ratingBar.setStepSize(1.0f);
                    ratingBar.setId(tmpid3);
                    params4.addRule(RelativeLayout.BELOW, tmpid2);
                    params4.setMargins(0, 8, 0, 20);
                    params4.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    oldtmp = tmpid3;
                    ratingBar.setLayoutParams(params4);

                    wrapperLayout.addView(textView);
                    wrapperLayout.addView(textView1);
                    wrapperLayout.addView(textView2);
                    wrapperLayout.addView(ratingBar);
                    relativeLayout.addView(wrapperLayout);
                    wrapperLayout.setVisibility(View.INVISIBLE);

                    scaleTypes.add(q.getString("type"));
                    scaleViewIds.add(tmpid3);

                } else if (q.getString("type").equals("text")) {
                    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

                    tmpid = rnd.nextInt(Integer.MAX_VALUE);
                    tmpid2 = rnd.nextInt(Integer.MAX_VALUE);

                    TextView textView = new TextView(activity);
                    textView.setMinLines(3);
                    textView.setId(tmpid);
                    textView.setText(q.getString("question"));
                    if(i > 0) {
                        params1.addRule(RelativeLayout.BELOW, oldtmp);
                    }
                    textView.setLayoutParams(params1);

                    EditText editText = new EditText(activity);
                    editText.setId(tmpid2);
                    params2.addRule(RelativeLayout.BELOW, tmpid);
                    editText.setLayoutParams(params2);
                    oldtmp = tmpid2;
                    wrapperLayout.addView(textView);
                    wrapperLayout.addView(editText);
                    relativeLayout.addView(wrapperLayout);
                    wrapperLayout.setVisibility(View.INVISIBLE);

                    scaleTypes.add(q.getString("type"));
                    scaleViewIds.add(tmpid2);
                } else if (q.getString("type").equals("truefalse")) {
                    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

                    tmpid = rnd.nextInt(Integer.MAX_VALUE);
                    tmpid2 = rnd.nextInt(Integer.MAX_VALUE);

                    TextView textView = new TextView(activity);
                    textView.setId(tmpid);
                    textView.setMinLines(3);
                    textView.setText(q.getString("question"));
                    if(i > 0) {
                        params1.addRule(RelativeLayout.BELOW, oldtmp);
                    }
                    textView.setLayoutParams(params1);

                    Switch yesNoSwitch = new Switch(activity);
                    yesNoSwitch.setText("");
                    yesNoSwitch.setTextOff(activity.getResources().getString(R.string.no));
                    yesNoSwitch.setTextOn(activity.getResources().getString(R.string.yes));
                    yesNoSwitch.setId(tmpid2);

                    params2.addRule(RelativeLayout.BELOW, tmpid);
                    yesNoSwitch.setLayoutParams(params2);
                    oldtmp = tmpid2;
                    wrapperLayout.addView(textView);
                    wrapperLayout.addView(yesNoSwitch);
                    relativeLayout.addView(wrapperLayout);
                    wrapperLayout.setVisibility(View.INVISIBLE);

                    scaleTypes.add(q.getString("type"));
                    scaleViewIds.add(tmpid2);
                } else if(q.getString("type").equals("hidden")) {
                    hidden++;
                    scaleTypes.add(q.getString("type"));
                    scaleViewIds.add(0);
                    continue;
                }
            }
            //saveParams.addRule(RelativeLayout.BELOW, relativeLayout.getId());
            saveParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            saveParams.setMargins(0, 400, 0, 0);
            saveButton.setLayoutParams(saveParams);
            saveButton.setText(activity.getText(R.string.save));
            saveButton.setVisibility(View.INVISIBLE);
            //nextParams.addRule(RelativeLayout.BELOW, relativeLayout.getId());
            nextParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            nextParams.setMargins(0, 400, 0, 0);
            nextButton.setLayoutParams(nextParams);
            nextButton.setText(activity.getString(R.string.next));

            relativeLayout.addView(nextButton);
            relativeLayout.addView(saveButton);
            relativeLayout.setLayoutParams(rlp);

            scrollView.addView(relativeLayout);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        relativeLayout.getChildAt(0).setVisibility(View.VISIBLE);

        //dialog.setContentView(R.layout.flow_short_scale);
        this.questionnaireDialog.setContentView(scrollView, slp);
        final int amountHiddenQuestions = hidden;

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (questionsCount == questionsAmount - 2 - amountHiddenQuestions) {
                    v.setVisibility(View.INVISIBLE);
                    saveButton.setVisibility(View.VISIBLE);
                }
                if (questionsCount == questionsAmount - 1) {
                    relativeLayout.getChildAt(questionsCount - 1).setVisibility(View.INVISIBLE);
                    questionsCount = 0;
                    questionsAmount = 0;
                } else {
                    questionsCount++;
                    relativeLayout.getChildAt(questionsCount - 1).setVisibility(View.INVISIBLE);
                    relativeLayout.getChildAt(questionsCount).setVisibility(View.VISIBLE);

                }
            }
        });
    }

    public void saveQuestionnaireItems(File root, Boolean writeHeader, String headerComments, String footerComments, long startActivityTimestamp) {

        String outputString = "";
        if(writeHeader) {
            outputString = headerComments;
            outputString += "" + activity.getString(R.string.file_header_timestamp_show) + "," + activity.getString(R.string.file_header_timestamp_start) + "," + activity.getString(R.string.file_header_timestamp_stop) + ",";
            for (int i = 0; i < scaleTypes.size(); i++) {
                if (i != scaleTypes.size() - 1) {
                    outputString += "item." + String.format("%02d", (i+1)) + ",";
                } else {
                    outputString += "item." + String.format("%02d", (i+1)) + "\n";
                }
            }
        }
        outputString += Long.toString((this.showTimestamp - startActivityTimestamp)) + "," + Long.toString((this.startTimestamp - startActivityTimestamp)) + "," + Long.toString((System.currentTimeMillis() - startActivityTimestamp)) + ",";
        for (int i = 0; i < scaleTypes.size(); i++) {
            String value = "";
            if(scaleTypes.get(i).equals("rating")) {
                RatingBar r = (RatingBar) this.questionnaireDialog.findViewById(scaleViewIds.get(i));
                value = Float.toString(r.getRating());
            } else if(scaleTypes.get(i).equals("text")) {
                EditText e = (EditText) this.questionnaireDialog.findViewById(scaleViewIds.get(i));
                value = e.getText().toString();
            } else if(scaleTypes.get(i).equals("truefalse")) {
                Switch s = (Switch) this.questionnaireDialog.findViewById(scaleViewIds.get(i));
                if(s.isChecked())
                    value = "1";
                else
                    value = "0";
            } else if(scaleTypes.get(i).equals("hidden")) {
                value = activity.getString(R.string.questionnaire_not_available_item);
            }
            if(i != scaleTypes.size()-1) {
                outputString += value + ",";
            } else {
                outputString += value;
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(root, activity.getString(R.string.file_name_self_report)), true));
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        if(footerComments != null) {
            writeFooter(footerComments, root, activity.getString(R.string.file_name_self_report));
        }
    }

    private JSONObject readQuestionnaireFromJSON() {
        BufferedReader input;
        JSONObject jsonObject = null;
        try {
            input = new BufferedReader(new InputStreamReader(
                    activity.getAssets().open(this.questionnaireFileName)));
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int num;
            while ((num = input.read(buffer)) > 0) {
                content.append(buffer, 0, num);
            }
            jsonObject = new JSONObject(content.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void writeFooter (String data, File root, String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(root, filename), true));
            writer.write(data);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }
}
